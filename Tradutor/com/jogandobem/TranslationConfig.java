package com.jogandobem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

public final class TranslationConfig {
   private static final String FILE_NAME = "translator_config.json";

   @SerializedName("api_host")
   public String apiHost;

   @SerializedName("api_key")
   public String apiKey;

   @SerializedName("ws_url")
   public String wsUrl;

   @SerializedName("server_id")
   public String serverId;

   @SerializedName("server_secret")
   public String serverSecret;

   @SerializedName("default_language")
   public String defaultLanguage;

   @SerializedName("warn_on_join")
   public boolean warnOnJoin;

   @SerializedName("warn_message")
   public String warnMessage;

   @SerializedName("ipinfo_token")
   public String ipinfoToken;

   @SerializedName("api_timeout_ms")
   public int apiTimeoutMs;

   @SerializedName("ws_reconnect_seconds")
   public int wsReconnectSeconds;

   @SerializedName("pending_ttl_seconds")
   public int pendingTtlSeconds;

   public static TranslationConfig loadOrCreate(Path dataDir, HytaleLogger logger) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      TranslationConfig defaults = defaultConfig();
      if (dataDir == null) {
         return defaults;
      }

      try {
         Files.createDirectories(dataDir);
      } catch (IOException e) {
         ((Api) logger.atWarning().withCause(e)).log("ChatTranslation failed to create data directory");
      }

      Path cfgPath = dataDir.resolve(FILE_NAME);
      if (!Files.exists(cfgPath)) {
         writeConfig(cfgPath, defaults, gson, logger);
         return defaults;
      }

      try {
         String json = Files.readString(cfgPath, StandardCharsets.UTF_8);
         TranslationConfig cfg = gson.fromJson(json, TranslationConfig.class);
         if (cfg == null) {
            return defaults;
         }
         cfg.applyDefaults(defaults);
         return cfg;
      } catch (IOException | JsonParseException e) {
         ((Api) logger.atWarning().withCause(e)).log("ChatTranslation failed to read translator_config.json");
         return defaults;
      }
   }

   public boolean isApiConfigured() {
      return this.wsUrl != null && !this.wsUrl.isBlank() && this.serverId != null && !this.serverId.isBlank();
   }

   public boolean hasIpInfoToken() {
      return this.ipinfoToken != null && !this.ipinfoToken.isBlank();
   }

   public void applyFrom(TranslationConfig other) {
      if (other == null) {
         return;
      }
      this.apiHost = other.apiHost;
      this.apiKey = other.apiKey;
      this.wsUrl = other.wsUrl;
      this.serverId = other.serverId;
      this.serverSecret = other.serverSecret;
      this.defaultLanguage = other.defaultLanguage;
      this.warnOnJoin = other.warnOnJoin;
      this.warnMessage = other.warnMessage;
      this.ipinfoToken = other.ipinfoToken;
      this.apiTimeoutMs = other.apiTimeoutMs;
      this.wsReconnectSeconds = other.wsReconnectSeconds;
      this.pendingTtlSeconds = other.pendingTtlSeconds;
   }

   public String getEndpoint() {
      if (this.apiHost == null) {
         return "";
      }
      String host = this.apiHost.trim();
      if (host.endsWith("/")) {
         host = host.substring(0, host.length() - 1);
      }
      return host + "/traduzir";
   }

   private static TranslationConfig defaultConfig() {
      TranslationConfig cfg = new TranslationConfig();
      cfg.apiHost = "http://127.0.0.1:5521";
      cfg.apiKey = "";
      cfg.wsUrl = "ws://127.0.0.1:5521/ws";
      cfg.serverId = "server-1";
      cfg.serverSecret = "";
      cfg.defaultLanguage = "auto";
      cfg.warnOnJoin = true;
      cfg.warnMessage = "Servidor com traducao automatica. Use /l <codigo> para escolher o idioma.";
      cfg.ipinfoToken = "";
      cfg.apiTimeoutMs = 5000;
      cfg.wsReconnectSeconds = 3;
      cfg.pendingTtlSeconds = 30;
      return cfg;
   }

   private void applyDefaults(TranslationConfig defaults) {
      if (this.apiHost == null || this.apiHost.isBlank()) {
         this.apiHost = defaults.apiHost;
      }
      if (this.apiKey == null) {
         this.apiKey = defaults.apiKey;
      }
      if (this.wsUrl == null || this.wsUrl.isBlank()) {
         this.wsUrl = defaults.wsUrl;
      }
      if (this.serverId == null || this.serverId.isBlank()) {
         this.serverId = defaults.serverId;
      }
      if (this.serverSecret == null) {
         this.serverSecret = defaults.serverSecret;
      }
      if (this.defaultLanguage == null || this.defaultLanguage.isBlank()) {
         this.defaultLanguage = defaults.defaultLanguage;
      }
      if (this.warnMessage == null || this.warnMessage.isBlank()) {
         this.warnMessage = defaults.warnMessage;
      }
      if (this.ipinfoToken == null) {
         this.ipinfoToken = defaults.ipinfoToken;
      }
      if (this.apiTimeoutMs <= 0) {
         this.apiTimeoutMs = defaults.apiTimeoutMs;
      }
      if (this.wsReconnectSeconds <= 0) {
         this.wsReconnectSeconds = defaults.wsReconnectSeconds;
      }
      if (this.pendingTtlSeconds <= 0) {
         this.pendingTtlSeconds = defaults.pendingTtlSeconds;
      }
   }

   private static void writeConfig(Path path, TranslationConfig cfg, Gson gson, HytaleLogger logger) {
      try {
         String json = gson.toJson(cfg);
         Files.writeString(path, json, StandardCharsets.UTF_8, new OpenOption[0]);
      } catch (IOException e) {
         ((Api) logger.atWarning().withCause(e)).log("ChatTranslation failed to write translator_config.json");
      }
   }
}
