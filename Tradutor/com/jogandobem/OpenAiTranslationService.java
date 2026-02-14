package com.jogandobem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.jogandobem.SocketModels.ChatPayload;
import com.jogandobem.TranslationModels.TranslationResponse;
import com.jogandobem.TranslationModels.TranslationResult;
import com.jogandobem.TranslationModels.TranslationTarget;

public final class OpenAiTranslationService {
   private static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/responses";
   private static final String DEFAULT_MODEL = "gpt-5-nano";
   private static final int MAX_HTTP_LOG_BODY = 1000;

   private final TranslationConfig config;
   private final HytaleLogger logger;
   private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
   private final HttpClient httpClient;
   private final ExecutorService executor;

   public OpenAiTranslationService(TranslationConfig config, HytaleLogger logger) {
      this.config = config;
      this.logger = logger;
      this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
      this.executor = Executors.newFixedThreadPool(2);
   }

   public CompletableFuture<TranslationResponse> translateAsync(ChatPayload payload) {
      return CompletableFuture.supplyAsync(() -> translateInternal(payload), this.executor);
   }

   public TranslationResponse buildFallbackResponse(ChatPayload payload) {
      if (payload == null) {
         return emptyResponse("", "");
      }
      return normalizeResponse(
            null,
            sanitizeTargets(payload.jogadoresOnline),
            sanitizeTargets(payload.jogadoresOnline),
            safe(payload.textoOriginal),
            safe(payload.idiomaOriginal),
            Map.of(),
            safe(payload.jogador),
            safe(payload.jogadorUuid)
      );
   }

   public void shutdown() {
      this.executor.shutdown();
      try {
         if (!this.executor.awaitTermination(3, TimeUnit.SECONDS)) {
            this.executor.shutdownNow();
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         this.executor.shutdownNow();
      }
   }

   private TranslationResponse translateInternal(ChatPayload payload) {
      if (payload == null) {
         return emptyResponse("", "");
      }

      List<TranslationTarget> originalTargets = sanitizeTargets(payload.jogadoresOnline);
      if (originalTargets.isEmpty()) {
         return emptyResponse(safe(payload.jogador), safe(payload.jogadorUuid));
      }

      String originalText = safe(payload.textoOriginal);
      String originalLanguage = safe(payload.idiomaOriginal);
      String sender = safe(payload.jogador);
      String senderUuid = safe(payload.jogadorUuid);

      DedupeResult dedupe = dedupeTargetsByLanguage(originalTargets);
      if (dedupe.targets.isEmpty()) {
         return emptyResponse(sender, senderUuid);
      }

      if (!this.config.hasOpenAiApiKey()) {
         ((Api) this.logger.atWarning()).log("ChatTranslation OpenAI key not configured. Using fallback.");
         return normalizeResponse(null, originalTargets, dedupe.targets, originalText, originalLanguage, dedupe.representativeToLanguage, sender, senderUuid);
      }

      String model = resolveModel();
      String prompt = buildPrompt(originalText, dedupe.targets);
      ((Api) this.logger.atInfo()).log(
            "ChatTranslation OpenAI request model=" + model
                  + " targets=" + originalTargets.size()
                  + " dedupe=" + dedupe.targets.size()
                  + " idioma_original=" + originalLanguage
      );

      for (int attempt = 1; attempt <= 2; attempt++) {
         long start = System.currentTimeMillis();
         try {
            JsonObject requestBody = buildRequestBody(model, prompt);
            HttpRequest request = HttpRequest.newBuilder()
                  .uri(URI.create(OPENAI_ENDPOINT))
                  .timeout(Duration.ofMillis(Math.max(1000, this.config.apiTimeoutMs)))
                  .header("Content-Type", "application/json")
                  .header("Authorization", "Bearer " + this.config.openAiApiKey.trim())
                  .POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(requestBody), StandardCharsets.UTF_8))
                  .build();

            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            String body = response.body();
            if (status < 200 || status >= 300) {
               ((Api) this.logger.atWarning()).log(
                     "ChatTranslation OpenAI status " + status + " attempt=" + attempt
                           + " body=" + truncate(body, MAX_HTTP_LOG_BODY)
               );
               continue;
            }

            JsonObject translationObject = parseResponseToTranslationObject(body);
            if (translationObject == null) {
               ((Api) this.logger.atWarning()).log(
                     "ChatTranslation OpenAI parse returned null attempt=" + attempt
                           + " body=" + truncate(body, MAX_HTTP_LOG_BODY)
               );
               continue;
            }
            if (!validateTranslationOutput(translationObject)) {
               ((Api) this.logger.atWarning()).log(
                     "ChatTranslation OpenAI invalid output attempt=" + attempt
                           + " parsed=" + truncate(this.gson.toJson(translationObject), MAX_HTTP_LOG_BODY)
               );
               continue;
            }

            TranslationResponse normalized = normalizeResponse(
                  translationObject,
                  originalTargets,
                  dedupe.targets,
                  originalText,
                  originalLanguage,
                  dedupe.representativeToLanguage,
                  sender,
                  senderUuid
            );
            ((Api) this.logger.atInfo()).log(
                  "ChatTranslation OpenAI ok in " + (System.currentTimeMillis() - start)
                        + "ms translated=" + normalized.traducao.size()
            );
            return normalized;
         } catch (Exception e) {
            ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation OpenAI failed attempt=" + attempt);
         }
      }

      ((Api) this.logger.atWarning()).log("ChatTranslation OpenAI exhausted retries. Using fallback.");
      return normalizeResponse(null, originalTargets, dedupe.targets, originalText, originalLanguage, dedupe.representativeToLanguage, sender, senderUuid);
   }

   private String resolveModel() {
      String configured = this.config.openAiModel;
      if (configured == null || configured.isBlank()) {
         return DEFAULT_MODEL;
      }
      return configured.trim();
   }

   private static List<TranslationTarget> sanitizeTargets(List<TranslationTarget> source) {
      if (source == null || source.isEmpty()) {
         return List.of();
      }
      List<TranslationTarget> list = new ArrayList<>();
      for (TranslationTarget item : source) {
         if (item == null || item.jogador == null) {
            continue;
         }
         String name = item.jogador.trim();
         if (name.isEmpty()) {
            continue;
         }
         TranslationTarget target = new TranslationTarget();
         target.jogador = name;
         target.idioma = item.idioma == null ? "" : item.idioma.trim();
         list.add(target);
      }
      return list;
   }

   private static DedupeResult dedupeTargetsByLanguage(List<TranslationTarget> targets) {
      Map<String, String> representativeToLanguage = new HashMap<>();
      List<TranslationTarget> deduped = new ArrayList<>();
      Map<String, Boolean> seenByLanguage = new HashMap<>();

      for (TranslationTarget item : targets) {
         if (item == null || item.jogador == null) {
            continue;
         }
         String name = item.jogador.trim();
         if (name.isEmpty()) {
            continue;
         }
         String languageKey = normalizeLanguage(item.idioma);
         if (seenByLanguage.putIfAbsent(languageKey, Boolean.TRUE) == null) {
            TranslationTarget rep = new TranslationTarget();
            rep.jogador = name;
            rep.idioma = item.idioma == null ? "" : item.idioma.trim();
            deduped.add(rep);
            representativeToLanguage.put(name.toLowerCase(Locale.ROOT), languageKey);
         }
      }

      return new DedupeResult(deduped, representativeToLanguage);
   }

   private static String buildPrompt(String originalText, List<TranslationTarget> dedupedTargets) {
      Gson jsonGson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
      String playersJson = jsonGson.toJson(dedupedTargets);
      return (
            "Voce e um tradutor de chat que traduz o chat de um servidor de Hytale. " +
                  "Traduza a mensagem recebida para os idiomas selecionados.\n\n" +
                  "Realize a traducao da melhor forma possivel adaptando girias e expressoes unicas para uma compativel para o idioma destino quando necessario.\n\n" +
                  "A lista de jogadores abaixo contem no maximo 1 jogador por idioma. Traduza para o idioma indicado em cada entrada.\n\n" +
                  "O texto para jogadores falantes do mesmo idioma deve ser enviado EXATAMENTE igual ao original sem nenhuma alteracao.\n\n" +
                  "Retorne SOMENTE um JSON valido no formato:\n" +
                  "{\"traducao\":[{\"jogador\":\"Nome\",\"texto_traduzido\":\"Mensagem\"}]}\n\n" +
                  "---\n\n" +
                  "**Texto Original:**\n" +
                  "\"" + escapeForPrompt(originalText) + "\"\n\n" +
                  "**Jogadores Online:**\n" +
                  playersJson
      );
   }

   private static String escapeForPrompt(String value) {
      if (value == null) {
         return "";
      }
      return value.replace("\\", "\\\\").replace("\"", "\\\"");
   }

   private JsonObject buildRequestBody(String model, String prompt) {
      JsonObject root = new JsonObject();
      root.addProperty("model", model);
      root.addProperty("instructions", "Responda somente com JSON valido.");
      root.addProperty("input", prompt);

      JsonObject text = new JsonObject();
      JsonObject format = new JsonObject();
      format.addProperty("type", "json_schema");
      format.addProperty("name", "chat_translation");
      format.addProperty("description", "Lista de traducoes por jogador");
      format.add("schema", buildSchema());
      format.addProperty("strict", true);
      text.add("format", format);
      root.add("text", text);
      return root;
   }

   private static JsonObject buildSchema() {
      JsonObject schema = new JsonObject();
      schema.addProperty("type", "object");

      JsonObject properties = new JsonObject();
      JsonObject traducao = new JsonObject();
      traducao.addProperty("type", "array");

      JsonObject item = new JsonObject();
      item.addProperty("type", "object");

      JsonObject itemProperties = new JsonObject();
      JsonObject jogador = new JsonObject();
      jogador.addProperty("type", "string");
      JsonObject textoTraduzido = new JsonObject();
      textoTraduzido.addProperty("type", "string");
      itemProperties.add("jogador", jogador);
      itemProperties.add("texto_traduzido", textoTraduzido);
      item.add("properties", itemProperties);

      JsonArray requiredItem = new JsonArray();
      requiredItem.add("jogador");
      requiredItem.add("texto_traduzido");
      item.add("required", requiredItem);
      item.addProperty("additionalProperties", false);

      traducao.add("items", item);
      properties.add("traducao", traducao);
      schema.add("properties", properties);

      JsonArray requiredRoot = new JsonArray();
      requiredRoot.add("traducao");
      schema.add("required", requiredRoot);
      schema.addProperty("additionalProperties", false);
      return schema;
   }

   private JsonObject parseResponseToTranslationObject(String body) {
      if (body == null || body.isBlank()) {
         return null;
      }
      try {
         JsonElement rootElement = JsonParser.parseString(body);
         String outputText = extractOutputText(rootElement);
         if (outputText != null && !outputText.isBlank()) {
            JsonObject parsedOutput = parseJsonObjectSafe(outputText);
            if (parsedOutput != null) {
               JsonObject foundInOutput = findTranslationObject(parsedOutput, 0);
               if (foundInOutput != null) {
                  return foundInOutput;
               }
            }
         }
         JsonObject bySearch = findTranslationObject(rootElement, 0);
         if (bySearch != null) {
            return bySearch;
         }
         return null;
      } catch (JsonParseException e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation OpenAI response parse error");
         return null;
      }
   }

   private static JsonObject findTranslationObject(JsonElement element, int depth) {
      if (element == null || element.isJsonNull() || depth > 10) {
         return null;
      }
      if (element.isJsonObject()) {
         JsonObject obj = element.getAsJsonObject();
         if (hasTranslationArrayKey(obj)) {
            return obj;
         }
         for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            JsonObject found = findTranslationObject(entry.getValue(), depth + 1);
            if (found != null) {
               return found;
            }
         }
         return null;
      }
      if (element.isJsonArray()) {
         JsonArray arr = element.getAsJsonArray();
         for (JsonElement item : arr) {
            JsonObject found = findTranslationObject(item, depth + 1);
            if (found != null) {
               return found;
            }
         }
      }
      return null;
   }

   private static boolean hasTranslationArrayKey(JsonObject data) {
      if (data == null) {
         return false;
      }
      JsonElement a = data.get("traducao");
      if (a != null && a.isJsonArray()) {
         return true;
      }
      JsonElement b = data.get("tradução");
      if (b != null && b.isJsonArray()) {
         return true;
      }
      JsonElement c = data.get("translations");
      if (c != null && c.isJsonArray()) {
         return true;
      }
      JsonElement d = data.get("translation");
      return d != null && d.isJsonArray();
   }

   private static String extractOutputText(JsonElement root) {
      if (root == null || !root.isJsonObject()) {
         return null;
      }
      JsonObject obj = root.getAsJsonObject();
      if (obj.has("output_text") && obj.get("output_text").isJsonPrimitive()) {
         return obj.get("output_text").getAsString();
      }

      StringBuilder out = new StringBuilder();
      JsonElement output = obj.get("output");
      if (output != null && output.isJsonArray()) {
         for (JsonElement item : output.getAsJsonArray()) {
            if (!item.isJsonObject()) {
               continue;
            }
            JsonObject msg = item.getAsJsonObject();
            JsonElement content = msg.get("content");
            if (content == null || !content.isJsonArray()) {
               continue;
            }
            for (JsonElement chunk : content.getAsJsonArray()) {
               String text = readTextChunk(chunk);
               if (text == null || text.isBlank()) {
                  continue;
               }
               if (out.length() > 0) {
                  out.append('\n');
               }
               out.append(text);
            }
         }
      }

      if (out.length() > 0) {
         return out.toString();
      }
      return null;
   }

   private static String readTextChunk(JsonElement chunk) {
      if (chunk == null || !chunk.isJsonObject()) {
         return null;
      }
      JsonObject obj = chunk.getAsJsonObject();
      if (obj.has("text") && obj.get("text").isJsonPrimitive()) {
         return obj.get("text").getAsString();
      }
      if (obj.has("output_text") && obj.get("output_text").isJsonPrimitive()) {
         return obj.get("output_text").getAsString();
      }
      if (obj.has("value") && obj.get("value").isJsonPrimitive()) {
         return obj.get("value").getAsString();
      }
      JsonElement textObj = obj.get("text");
      if (textObj != null && textObj.isJsonObject()) {
         JsonObject nested = textObj.getAsJsonObject();
         if (nested.has("value") && nested.get("value").isJsonPrimitive()) {
            return nested.get("value").getAsString();
         }
      }
      return null;
   }

   private static JsonObject parseJsonObjectSafe(String text) {
      if (text == null || text.isBlank()) {
         return null;
      }
      try {
         JsonElement parsed = JsonParser.parseString(text);
         if (parsed.isJsonObject()) {
            return parsed.getAsJsonObject();
         }
      } catch (Exception ignored) {
      }
      int start = text.indexOf('{');
      int end = text.lastIndexOf('}');
      if (start >= 0 && end > start) {
         String slice = text.substring(start, end + 1);
         try {
            JsonElement parsed = JsonParser.parseString(slice);
            if (parsed.isJsonObject()) {
               return parsed.getAsJsonObject();
            }
         } catch (Exception ignored) {
         }
      }
      return null;
   }

   private static JsonArray getTranslationItems(JsonObject data) {
      if (data == null) {
         return null;
      }
      JsonElement items = null;
      if (data.has("traducao")) {
         items = data.get("traducao");
      } else if (data.has("tradução")) {
         items = data.get("tradução");
      } else if (data.has("translations")) {
         items = data.get("translations");
      } else if (data.has("translation")) {
         items = data.get("translation");
      }
      if (items == null || !items.isJsonArray()) {
         return null;
      }
      return items.getAsJsonArray();
   }

   private static String getPlayerName(JsonObject item) {
      String name = getAsString(item, "jogador");
      if (name == null || name.isBlank()) {
         name = getAsString(item, "player");
      }
      if (name == null || name.isBlank()) {
         name = getAsString(item, "name");
      }
      return name;
   }

   private static String getTranslatedText(JsonObject item) {
      String text = getAsString(item, "texto_traduzido");
      if (text == null) {
         text = getAsString(item, "textoTraduzido");
      }
      if (text == null) {
         text = getAsString(item, "translated_text");
      }
      if (text == null) {
         text = getAsString(item, "text");
      }
      return text;
   }

   private static boolean validateTranslationOutput(JsonObject data) {
      if (data == null) {
         return false;
      }
      JsonArray items = getTranslationItems(data);
      if (items == null) {
         return false;
      }
      for (JsonElement item : items) {
         if (!item.isJsonObject()) {
            return false;
         }
         JsonObject obj = item.getAsJsonObject();
         String texto = getTranslatedText(obj);
         if (texto == null) {
            return false;
         }
      }
      return true;
   }

   private static TranslationResponse normalizeResponse(
         JsonObject data,
         List<TranslationTarget> jogadoresOnline,
         List<TranslationTarget> dedupedTargets,
         String textoOriginal,
         String idiomaOriginal,
         Map<String, String> representativeToLanguage,
         String jogador,
         String jogadorUuid
   ) {
      Map<String, String> byName = new HashMap<>();
      Map<String, String> byLanguage = new HashMap<>();
      List<String> textsInOrder = new ArrayList<>();

      JsonArray items = getTranslationItems(data);
      if (items != null) {
         for (JsonElement item : items) {
            if (!item.isJsonObject()) {
               continue;
            }
            JsonObject obj = item.getAsJsonObject();
            String targetName = safe(getPlayerName(obj));
            String translated = getTranslatedText(obj);
            if (translated == null) {
               continue;
            }
            textsInOrder.add(translated);
            if (!targetName.isBlank()) {
               String targetKey = targetName.toLowerCase(Locale.ROOT);
               byName.put(targetKey, translated);
               String language = representativeToLanguage.get(targetKey);
               if (language != null && !language.isBlank()) {
                  byLanguage.put(language, translated);
               }
            }
         }
      }

      // If the model changed target names, map by index against deduped list.
      if (!textsInOrder.isEmpty() && dedupedTargets != null && !dedupedTargets.isEmpty()) {
         int max = Math.min(textsInOrder.size(), dedupedTargets.size());
         for (int i = 0; i < max; i++) {
            TranslationTarget deduped = dedupedTargets.get(i);
            if (deduped == null) {
               continue;
            }
            String langKey = normalizeLanguage(deduped.idioma);
            if (langKey.isBlank()) {
               continue;
            }
            byLanguage.putIfAbsent(langKey, textsInOrder.get(i));
         }
      }

      String baseLanguage = normalizeLanguage(idiomaOriginal);
      List<TranslationResult> translatedItems = new ArrayList<>();
      for (TranslationTarget target : jogadoresOnline) {
         if (target == null || target.jogador == null) {
            continue;
         }
         String targetName = target.jogador.trim();
         if (targetName.isEmpty()) {
            continue;
         }
         String targetLanguage = normalizeLanguage(target.idioma);
         String text;
         if (Objects.equals(targetLanguage, baseLanguage)) {
            text = textoOriginal;
         } else {
            String key = targetName.toLowerCase(Locale.ROOT);
            text = byName.get(key);
            if (text == null || text.isBlank()) {
               text = byLanguage.get(targetLanguage);
            }
            if (text == null || text.isBlank()) {
               text = textoOriginal;
            }
         }

         TranslationResult result = new TranslationResult();
         result.jogador = targetName;
         result.textoTraduzido = text;
         translatedItems.add(result);
      }

      TranslationResponse response = new TranslationResponse();
      response.jogador = jogador;
      response.jogadorUuid = jogadorUuid;
      response.traducao = translatedItems;
      return response;
   }

   private static TranslationResponse emptyResponse(String jogador, String jogadorUuid) {
      TranslationResponse response = new TranslationResponse();
      response.jogador = jogador == null ? "" : jogador;
      response.jogadorUuid = jogadorUuid == null ? "" : jogadorUuid;
      response.traducao = List.of();
      return response;
   }

   private static String getAsString(JsonObject obj, String key) {
      if (obj == null || key == null) {
         return null;
      }
      JsonElement value = obj.get(key);
      if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
         return null;
      }
      return value.getAsString();
   }

   private static String normalizeLanguage(String language) {
      if (language == null) {
         return "";
      }
      return language.trim().toLowerCase(Locale.ROOT);
   }

   private static String safe(String value) {
      if (value == null) {
         return "";
      }
      return value;
   }

   private static String truncate(String value, int maxLen) {
      if (value == null) {
         return "";
      }
      if (value.length() <= maxLen) {
         return value;
      }
      return value.substring(0, Math.max(0, maxLen)) + "...(truncated)";
   }

   private static final class DedupeResult {
      private final List<TranslationTarget> targets;
      private final Map<String, String> representativeToLanguage;

      private DedupeResult(List<TranslationTarget> targets, Map<String, String> representativeToLanguage) {
         this.targets = targets;
         this.representativeToLanguage = representativeToLanguage;
      }
   }
}
