package com.jogandobem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MessageStore {
   private static final String FILE_NAME = "messages.json";
   private static final String DEFAULT_LANGUAGE = "pt-BR";

   private final Path filePath;
   private final Gson gson;
   private final HytaleLogger logger;
   private final Map<String, Map<String, Object>> messages = new LinkedHashMap<>();
   private final Map<String, String> canonicalLangs = new LinkedHashMap<>();

   private MessageStore(Path filePath, Gson gson, HytaleLogger logger) {
      this.filePath = filePath;
      this.gson = gson;
      this.logger = logger;
   }

   public static MessageStore loadOrCreate(Path dataDir, HytaleLogger logger) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      if (dataDir == null) {
         Path fallback = Path.of(".");
         MessageStore store = new MessageStore(fallback.resolve(FILE_NAME), gson, logger);
         store.loadOrCreateInternal();
         return store;
      }
      try {
         Files.createDirectories(dataDir);
      } catch (IOException e) {
         ((Api) logger.atWarning().withCause(e)).log("ChatTranslation failed to create data directory");
      }
      MessageStore store = new MessageStore(dataDir.resolve(FILE_NAME), gson, logger);
      store.loadOrCreateInternal();
      return store;
   }

   public void reload() {
      loadOrCreateInternal();
   }

   public String resolveLanguage(String preferred, TranslationConfig config) {
      String normalized = normalizeLang(preferred);
      if (hasLanguage(normalized)) {
         return canonicalLangs.get(normalized);
      }
      String fallback = normalizeLang(config == null ? null : config.defaultLanguage);
      if (hasLanguage(fallback)) {
         return canonicalLangs.get(fallback);
      }
      return DEFAULT_LANGUAGE;
   }

   public String getString(String key, String language, TranslationConfig config) {
      Object value = getValue(key, language, config);
      if (value instanceof String) {
         return (String) value;
      }
      if (value instanceof List) {
         @SuppressWarnings("unchecked")
         List<Object> items = (List<Object>) value;
         if (!items.isEmpty() && items.get(0) instanceof String) {
            return (String) items.get(0);
         }
      }
      return "";
   }

   public List<String> getLines(String key, String language, TranslationConfig config) {
      Object value = getValue(key, language, config);
      if (value instanceof List) {
         @SuppressWarnings("unchecked")
         List<Object> items = (List<Object>) value;
         List<String> lines = new ArrayList<>();
         for (Object item : items) {
            if (item instanceof String) {
               lines.add((String) item);
            }
         }
         return lines;
      }
      if (value instanceof String) {
         return List.of((String) value);
      }
      return Collections.emptyList();
   }

   public String format(String key, String language, TranslationConfig config, Map<String, String> params) {
      String template = getString(key, language, config);
      if (template.isEmpty() || params == null || params.isEmpty()) {
         return template;
      }
      String formatted = template;
      for (Map.Entry<String, String> entry : params.entrySet()) {
         String token = "{" + entry.getKey() + "}";
         String value = entry.getValue() == null ? "" : entry.getValue();
         formatted = formatted.replace(token, value);
      }
      return formatted;
   }

   public boolean hasLanguage(String language) {
      String normalized = normalizeLang(language);
      return normalized != null && !normalized.isEmpty() && canonicalLangs.containsKey(normalized);
   }

   private Object getValue(String key, String language, TranslationConfig config) {
      if (key == null || key.isBlank()) {
         return null;
      }
      String resolvedLang = resolveLanguage(language, config);
      Map<String, Object> langs = messages.get(key);
      if (langs == null || langs.isEmpty()) {
         return null;
      }
      Object value = langs.get(resolvedLang);
      if (value != null) {
         return value;
      }
      String normalized = normalizeLang(resolvedLang);
      String canonical = canonicalLangs.get(normalized);
      if (canonical != null) {
         value = langs.get(canonical);
      }
      if (value != null) {
         return value;
      }
      value = langs.get(DEFAULT_LANGUAGE);
      return value;
   }

   private void loadOrCreateInternal() {
      Map<String, Map<String, Object>> defaults = buildDefaultMessages();
      if (!Files.exists(this.filePath)) {
         this.messages.clear();
         this.messages.putAll(defaults);
         rebuildCanonicalLangs();
         writeMessages(defaults);
         return;
      }

      Map<String, Map<String, Object>> loaded = readMessages();
      if (loaded == null || loaded.isEmpty()) {
         this.messages.clear();
         this.messages.putAll(defaults);
         rebuildCanonicalLangs();
         writeMessages(defaults);
         return;
      }

      mergeDefaults(loaded, defaults);
      this.messages.clear();
      this.messages.putAll(loaded);
      rebuildCanonicalLangs();
      writeMessages(loaded);
   }

   private Map<String, Map<String, Object>> readMessages() {
      try {
         String json = Files.readString(this.filePath, StandardCharsets.UTF_8);
         Type type = new TypeToken<Map<String, Map<String, Object>>>() { }.getType();
         Map<String, Map<String, Object>> data = this.gson.fromJson(json, type);
         return data == null ? new LinkedHashMap<>() : data;
      } catch (IOException | JsonParseException e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to read messages.json");
         return null;
      }
   }

   private void writeMessages(Map<String, Map<String, Object>> data) {
      try {
         String json = this.gson.toJson(data);
         Files.writeString(this.filePath, json, StandardCharsets.UTF_8, new OpenOption[0]);
      } catch (IOException e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to write messages.json");
      }
   }

   private void mergeDefaults(Map<String, Map<String, Object>> target, Map<String, Map<String, Object>> defaults) {
      for (Map.Entry<String, Map<String, Object>> entry : defaults.entrySet()) {
         String key = entry.getKey();
         Map<String, Object> defaultLangs = entry.getValue();
         if (defaultLangs == null) {
            continue;
         }
         Map<String, Object> existing = target.get(key);
         if (existing == null) {
            target.put(key, new LinkedHashMap<>(defaultLangs));
            continue;
         }
         for (Map.Entry<String, Object> langEntry : defaultLangs.entrySet()) {
            String lang = langEntry.getKey();
            if (lang == null || lang.isBlank()) {
               continue;
            }
            if (!existing.containsKey(lang)) {
               existing.put(lang, langEntry.getValue());
            }
         }
      }
   }

   private void rebuildCanonicalLangs() {
      canonicalLangs.clear();
      for (Map<String, Object> values : messages.values()) {
         if (values == null) {
            continue;
         }
         for (String lang : values.keySet()) {
            if (lang == null || lang.isBlank()) {
               continue;
            }
            String normalized = normalizeLang(lang);
            if (!canonicalLangs.containsKey(normalized)) {
               canonicalLangs.put(normalized, lang);
            }
         }
      }
      if (!canonicalLangs.containsKey(normalizeLang(DEFAULT_LANGUAGE))) {
         canonicalLangs.put(normalizeLang(DEFAULT_LANGUAGE), DEFAULT_LANGUAGE);
      }
   }

   private static String normalizeLang(String lang) {
      if (lang == null) {
         return "";
      }
      return lang.trim().toLowerCase(Locale.ROOT);
   }

   private static Map<String, Map<String, Object>> buildDefaultMessages() {
      Map<String, Map<String, Object>> defaults = new LinkedHashMap<>();

      defaults.put("auto_language_set", langMap(
            "Seu idioma foi definido automaticamente para Português (Brasil) com base na sua localização.",
            "Your language was automatically set to English (United States) based on your location.",
            "Tu idioma se configuró automáticamente en Español (México) según tu ubicación.",
            "Votre langue a été définie automatiquement sur Français (France) en fonction de votre localisation.",
            "Ihre Sprache wurde basierend auf Ihrem Standort automatisch auf Deutsch (Deutschland) eingestellt.",
            "La tua lingua è stata impostata automaticamente su Italiano (Italia) in base alla tua posizione.",
            "Ваш язык был автоматически установлен на Русский (Россия) на основе вашего местоположения.",
            "お住まいの地域に基づいて、言語が自動的に日本語（日本）に設定されました。",
            "系统已根据您的位置自动将语言设置为中文（中国）。"
      ));

      defaults.put("warn_on_join", langMap(
            "Servidor com tradução automática. Use /l <codigo> para escolher o idioma.",
            "Server with automatic translation. Use /l <code> to choose your language.",
            "Servidor con traducción automática. Usa /l <codigo> para elegir el idioma.",
            "Serveur avec traduction automatique. Utilisez /l <code> pour choisir la langue.",
            "Server mit automatischer Übersetzung. Verwende /l <code>, um die Sprache auszuwählen.",
            "Server con traduzione automatica. Usa /l <codice> per scegliere la lingua.",
            "Сервер с автоматическим переводом. Используйте /l <код>, чтобы выбрать язык.",
            "自動翻訳サーバーです。/l <コード> で言語を選択してください。",
            "这是自动翻译服务器。使用 /l <代码> 选择语言。"
      ));

      defaults.put("cmd_only_players", langMap(
            "Este comando só pode ser usado por jogadores.",
            "This command can only be used by players.",
            "Este comando solo puede ser usado por jugadores.",
            "Cette commande ne peut être utilisée que par les joueurs.",
            "Dieser Befehl kann nur von Spielern verwendet werden.",
            "Questo comando può essere usato solo dai giocatori.",
            "Эту команду могут использовать только игроки.",
            "このコマンドはプレイヤーのみ使用できます。",
            "此命令仅可由玩家使用。"
      ));

      defaults.put("cmd_usage_language", langMap(
            "Uso: /l <codigo_idioma>",
            "Usage: /l <language_code>",
            "Uso: /l <codigo_idioma>",
            "Utilisation : /l <code_langue>",
            "Verwendung: /l <sprachcode>",
            "Uso: /l <codice_lingua>",
            "Использование: /l <код_языка>",
            "使い方: /l <言語コード>",
            "用法：/l <语言代码>"
      ));

      defaults.put("cmd_language_reset", langMap(
            "Idioma resetado para o padrão.",
            "Language reset to the default.",
            "Idioma restablecido al predeterminado.",
            "Langue réinitialisée par défaut.",
            "Sprache auf Standard zurückgesetzt.",
            "Lingua reimpostata su quella predefinita.",
            "Язык сброшен на значение по умолчанию.",
            "言語がデフォルトにリセットされました。",
            "语言已重置为默认值。"
      ));

      defaults.put("cmd_language_invalid", langMap(
            "Código de idioma inválido. Exemplos: en-US, pt-BR, zh-CN, zh-Hans-CN.",
            "Invalid language code. Examples: en-US, pt-BR, zh-CN, zh-Hans-CN.",
            "Código de idioma inválido. Ejemplos: en-US, pt-BR, zh-CN, zh-Hans-CN.",
            "Code de langue invalide. Exemples : en-US, pt-BR, zh-CN, zh-Hans-CN.",
            "Ungültiger Sprachcode. Beispiele: en-US, pt-BR, zh-CN, zh-Hans-CN.",
            "Codice lingua non valido. Esempi: en-US, pt-BR, zh-CN, zh-Hans-CN.",
            "Неверный код языка. Примеры: en-US, pt-BR, zh-CN, zh-Hans-CN.",
            "無効な言語コードです。例: en-US, pt-BR, zh-CN, zh-Hans-CN.",
            "语言代码无效。示例：en-US, pt-BR, zh-CN, zh-Hans-CN。"
      ));

      defaults.put("cmd_language_invalid_hint", langMap(
            "Use /l help <letra> para ver a lista de idiomas.",
            "Use /l help <letter> to see the list of languages.",
            "Usa /l help <letra> para ver la lista de idiomas.",
            "Utilisez /l help <lettre> pour voir la liste des langues.",
            "Verwende /l help <buchstabe>, um die Sprachliste zu sehen.",
            "Usa /l help <lettera> per vedere l'elenco delle lingue.",
            "Используйте /l help <буква>, чтобы увидеть список языков.",
            "/l help <文字> で言語一覧を表示します。",
            "使用 /l help <字母> 查看语言列表。"
      ));

      defaults.put("cmd_language_set", langMap(
            "Idioma definido para: {lang}",
            "Language set to: {lang}",
            "Idioma establecido en: {lang}",
            "Langue définie sur : {lang}",
            "Sprache gesetzt auf: {lang}",
            "Lingua impostata su: {lang}",
            "Язык установлен на: {lang}",
            "言語が設定されました: {lang}",
            "语言已设置为：{lang}"
      ));

      defaults.put("cmd_language_current", langMap(
            "Idioma atual: {current}",
            "Current language: {current}",
            "Idioma actual: {current}",
            "Langue actuelle : {current}",
            "Aktuelle Sprache: {current}",
            "Lingua attuale: {current}",
            "Текущий язык: {current}",
            "現在の言語: {current}",
            "当前语言：{current}"
      ));

      defaults.put("cmd_language_default", langMap(
            "(padrão: {lang})",
            "(default: {lang})",
            "(predeterminado: {lang})",
            "(par défaut : {lang})",
            "(Standard: {lang})",
            "(predefinito: {lang})",
            "(по умолчанию: {lang})",
            "（既定: {lang}）",
            "（默认：{lang}）"
      ));

      defaults.put("cmd_language_filter_title", langMap(
            "Idiomas disponíveis com filtro \"{filter}\":",
            "Available languages matching \"{filter}\":",
            "Idiomas disponibles con el filtro \"{filter}\":",
            "Langues disponibles avec le filtre \"{filter}\" :",
            "Verfügbare Sprachen mit Filter \"{filter}\":",
            "Lingue disponibili con filtro \"{filter}\":",
            "Доступные языки по фильтру \"{filter}\":",
            "フィルター「{filter}」に一致する言語:",
            "与筛选“{filter}”匹配的语言："
      ));

      defaults.put("cmd_language_filter_none", langMap(
            "Nenhum idioma encontrado para esse filtro.",
            "No languages found for that filter.",
            "No se encontraron idiomas para ese filtro.",
            "Aucune langue trouvée pour ce filtre.",
            "Keine Sprachen für diesen Filter gefunden.",
            "Nessuna lingua trovata per questo filtro.",
            "Для этого фильтра языки не найдены.",
            "このフィルターに一致する言語が見つかりません。",
            "未找到与该筛选匹配的语言。"
      ));

      defaults.put("cmd_reload_done", langMap(
            "Tradutor recarregado.",
            "Translator reloaded.",
            "Traductor recargado.",
            "Traducteur rechargé.",
            "Übersetzer neu geladen.",
            "Traduttore ricaricato.",
            "Переводчик перезагружен.",
            "翻訳機能を再読み込みしました。",
            "翻译器已重新加载。"
      ));

      defaults.put("cmd_help_intro", langListMap(
            List.of(
                  "Como funciona: o chat é traduzido para o idioma escolhido por você.",
                  "Use /l <codigo_idioma> para definir o idioma ou /l auto para usar o padrão do servidor.",
                  "Exemplo: /l pt-BR",
                  "Para listar idiomas: /l help <letra> (ex.: /l help p)"
            ),
            List.of(
                  "How it works: chat is translated to the language you choose.",
                  "Use /l <language_code> to set your language or /l auto to use the server default.",
                  "Example: /l pt-BR",
                  "To list languages: /l help <letter> (e.g., /l help p)"
            ),
            List.of(
                  "Cómo funciona: el chat se traduce al idioma que elijas.",
                  "Usa /l <codigo_idioma> para definir el idioma o /l auto para usar el predeterminado del servidor.",
                  "Ejemplo: /l pt-BR",
                  "Para listar idiomas: /l help <letra> (ej.: /l help p)"
            ),
            List.of(
                  "Fonctionnement : le chat est traduit dans la langue que vous choisissez.",
                  "Utilisez /l <code_langue> pour définir la langue ou /l auto pour utiliser la langue par défaut du serveur.",
                  "Exemple : /l pt-BR",
                  "Pour lister les langues : /l help <lettre> (ex. : /l help p)"
            ),
            List.of(
                  "So funktioniert es: Der Chat wird in die von dir gewählte Sprache übersetzt.",
                  "Verwende /l <sprachcode>, um die Sprache festzulegen, oder /l auto für den Serverstandard.",
                  "Beispiel: /l pt-BR",
                  "Sprachen auflisten: /l help <buchstabe> (z. B. /l help p)"
            ),
            List.of(
                  "Come funziona: la chat viene tradotta nella lingua che scegli.",
                  "Usa /l <codice_lingua> per impostare la lingua oppure /l auto per usare il predefinito del server.",
                  "Esempio: /l pt-BR",
                  "Per elencare le lingue: /l help <lettera> (es.: /l help p)"
            ),
            List.of(
                  "Как это работает: чат переводится на выбранный вами язык.",
                  "Используйте /l <код_языка>, чтобы выбрать язык, или /l auto для языка по умолчанию на сервере.",
                  "Пример: /l pt-BR",
                  "Список языков: /l help <буква> (например, /l help p)"
            ),
            List.of(
                  "使い方: チャットは選んだ言語に翻訳されます。",
                  "/l <言語コード> で言語を設定、/l auto でサーバーの既定言語を使用します。",
                  "例: /l pt-BR",
                  "言語一覧: /l help <文字>（例: /l help p）"
            ),
            List.of(
                  "工作方式：聊天会被翻译为你选择的语言。",
                  "使用 /l <语言代码> 设置语言，或使用 /l auto 采用服务器默认语言。",
                  "示例：/l pt-BR",
                  "查看语言列表：/l help <字母>（例如：/l help p）"
            )
      ));

      List<String> languageLines = Arrays.asList(LanguageCatalog.LANGUAGE_LINES);
      defaults.put("language_lines", langListMap(
            languageLines,
            languageLines,
            languageLines,
            languageLines,
            languageLines,
            languageLines,
            languageLines,
            languageLines,
            languageLines
      ));

      return defaults;
   }

   private static Map<String, Object> langMap(
         String ptBr,
         String enUs,
         String esMx,
         String frFr,
         String deDe,
         String itIt,
         String ruRu,
         String jaJp,
         String zhCn
   ) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("pt-BR", ptBr);
      map.put("en-US", enUs);
      map.put("es-MX", esMx);
      map.put("fr-FR", frFr);
      map.put("de-DE", deDe);
      map.put("it-IT", itIt);
      map.put("ru-RU", ruRu);
      map.put("ja-JP", jaJp);
      map.put("zh-CN", zhCn);
      return map;
   }

   private static Map<String, Object> langListMap(
         List<String> ptBr,
         List<String> enUs,
         List<String> esMx,
         List<String> frFr,
         List<String> deDe,
         List<String> itIt,
         List<String> ruRu,
         List<String> jaJp,
         List<String> zhCn
   ) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("pt-BR", ptBr);
      map.put("en-US", enUs);
      map.put("es-MX", esMx);
      map.put("fr-FR", frFr);
      map.put("de-DE", deDe);
      map.put("it-IT", itIt);
      map.put("ru-RU", ruRu);
      map.put("ja-JP", jaJp);
      map.put("zh-CN", zhCn);
      return map;
   }
}
