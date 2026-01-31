package com.jogandobem;

import com.jogandobem.TranslationModels.TranslationRequest;
import com.jogandobem.TranslationModels.TranslationResponse;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class TranslationService {
   private final TranslationConfig config;
   private final HytaleLogger logger;
   private final Gson gson = new Gson();
   private final HttpClient httpClient;

   public TranslationService(TranslationConfig config, HytaleLogger logger) {
      this.config = config;
      this.logger = logger;
      this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(this.config.apiTimeoutMs))
            .build();
   }

   public TranslationResponse translate(TranslationRequest request) {
      if (request == null) {
         return null;
      }
      String endpoint = this.config.getEndpoint();
      if (endpoint == null || endpoint.isBlank()) {
         return null;
      }

      try {
         String payload = this.gson.toJson(request);
         HttpRequest httpRequest = HttpRequest.newBuilder()
               .uri(URI.create(endpoint))
               .timeout(Duration.ofMillis(this.config.apiTimeoutMs))
               .header("Content-Type", "application/json")
               .POST(HttpRequest.BodyPublishers.ofString(payload))
               .build();
         HttpResponse<String> response = this.httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
         if (response.statusCode() < 200 || response.statusCode() >= 300) {
            ((Api) this.logger.atWarning()).log("ChatTranslation API returned status " + response.statusCode());
            return null;
         }
         String body = response.body();
         if (body == null || body.isBlank()) {
            return null;
         }
         return this.gson.fromJson(body, TranslationResponse.class);
      } catch (JsonParseException e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to parse API response");
      } catch (Exception e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation API request failed");
      }
      return null;
   }
}
