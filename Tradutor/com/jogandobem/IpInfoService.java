package com.jogandobem;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class IpInfoService {
   private static final String BASE_URL = "https://api.ipinfo.io/lite";
   private final TranslationConfig config;
   private final HytaleLogger logger;
   private final HttpClient httpClient;
   private final Gson gson = new Gson();

   public IpInfoService(TranslationConfig config, HytaleLogger logger) {
      this.config = config;
      this.logger = logger;
      this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
   }

   public CompletableFuture<IpInfoResult> lookup(String ip) {
      if (this.config == null || !this.config.hasIpInfoToken()) {
         return CompletableFuture.completedFuture(null);
      }
      String sanitizedIp = sanitizeIp(ip);
      if (sanitizedIp == null || sanitizedIp.isBlank()) {
         ((Api) this.logger.atWarning()).log("ChatTranslation IPInfo lookup skipped: player ip missing or invalid");
         return CompletableFuture.completedFuture(null);
      }
      ((Api) this.logger.atInfo()).log("ChatTranslation IPInfo lookup using ip=" + sanitizedIp);
      String url = buildUrl(sanitizedIp, this.config.ipinfoToken);
      URI uri;
      try {
         uri = URI.create(url);
      } catch (IllegalArgumentException e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation IPInfo invalid URI");
         return CompletableFuture.completedFuture(null);
      }
      HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(5))
            .GET()
            .build();

      return this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
               if (response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
                  ((Api) this.logger.atWarning()).log("ChatTranslation IPInfo status " + (response == null ? "null" : response.statusCode()));
                  return null;
               }
               String body = response.body();
               if (body == null || body.isBlank()) {
                  return null;
               }
               try {
                  IpInfoResponse parsed = this.gson.fromJson(body, IpInfoResponse.class);
                  if (parsed == null || parsed.countryCode == null || parsed.countryCode.isBlank()) {
                     return null;
                  }
                  IpInfoResult result = new IpInfoResult();
                  result.ip = parsed.ip == null ? "" : parsed.ip.trim();
                  result.countryCode = parsed.countryCode.trim();
                  return result;
               } catch (JsonParseException e) {
                  ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation IPInfo parse error");
                  return null;
               }
            })
            .exceptionally(err -> {
               ((Api) this.logger.atWarning().withCause(err)).log("ChatTranslation IPInfo request failed");
               return null;
            });
   }

   private static String buildUrl(String ip, String token) {
      String trimmedToken = token == null ? "" : token.trim();
      if (trimmedToken.isEmpty()) {
         return BASE_URL + "/me";
      }
      if (ip != null && !ip.isBlank()) {
         return BASE_URL + "/" + ip.trim() + "?token=" + trimmedToken;
      }
      return BASE_URL + "/me?token=" + trimmedToken;
   }

   private static String sanitizeIp(String ip) {
      if (ip == null) {
         return null;
      }
      String value = ip.trim();
      if (value.isEmpty()) {
         return null;
      }
      if (value.startsWith("/")) {
         value = value.substring(1);
      }
      if (value.contains("QuicStreamAddress") || value.contains("{") || value.contains("}") || value.contains(" ")) {
         return null;
      }
      int zoneIdx = value.indexOf('%');
      if (zoneIdx > 0) {
         value = value.substring(0, zoneIdx);
      }
      if (value.startsWith("[") && value.contains("]")) {
         int end = value.indexOf(']');
         String inner = value.substring(1, end).trim();
         value = inner;
      } else {
         int firstColon = value.indexOf(':');
         int lastColon = value.lastIndexOf(':');
         if (firstColon == lastColon && firstColon > 0 && value.indexOf('.') > 0) {
            value = value.substring(0, firstColon);
         }
      }
      if (!isLikelyIpChars(value)) {
         return null;
      }
      return value.isEmpty() ? null : value;
   }

   private static boolean isLikelyIpChars(String value) {
      if (value == null || value.isEmpty()) {
         return false;
      }
      for (int i = 0; i < value.length(); i++) {
         char c = value.charAt(i);
         if ((c >= '0' && c <= '9') || c == '.' || c == ':' || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
            continue;
         }
         return false;
      }
      return true;
   }

   public static final class IpInfoResult {
      public String ip;
      public String countryCode;
   }

   private static final class IpInfoResponse {
      @SerializedName("ip")
      public String ip;

      @SerializedName("country_code")
      public String countryCode;
   }
}
