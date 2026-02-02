package com.jogandobem.discord;

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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public final class DiscordConfig {
   private static final String FILE_NAME = "discord.json";
   private static final String WEBHOOK_PREFIX = "https://discord.com/api/webhooks/";

   @SerializedName("botToken")
   public String botToken;

   @SerializedName("channelsIds")
   public Map<String, String> channelsIds;

   @SerializedName("gameToDiscordFormat")
   public String gameToDiscordFormat;

   @SerializedName("discordToGameFormat")
   public String discordToGameFormat;

   @SerializedName("serverEventsEnabled")
   public boolean serverEventsEnabled;

   @SerializedName("serverStartMessage")
   public String serverStartMessage;

   @SerializedName("serverStopMessage")
   public String serverStopMessage;

   @SerializedName("playerJoinLeaveEnabled")
   public boolean playerJoinLeaveEnabled;

   @SerializedName("playerJoinFormat")
   public String playerJoinFormat;

   @SerializedName("playerLeaveFormat")
   public String playerLeaveFormat;

   @SerializedName("webhookUrl")
   public String webhookUrl;

   @SerializedName("webhookUsername")
   public String webhookUsername;

   @SerializedName("webhookAvatarUrl")
   public String webhookAvatarUrl;

   @SerializedName("useWebhookForChat")
   public boolean useWebhookForChat;

   @SerializedName("useWebhookForEvents")
   public boolean useWebhookForEvents;

   @SerializedName("statusEnabled")
   public boolean statusEnabled;

   @SerializedName("statusFormat")
   public String statusFormat;

   @SerializedName("statusUpdateIntervalSeconds")
   public int statusUpdateIntervalSeconds;

   public static DiscordConfig loadOrCreate(Path dataDir, HytaleLogger logger) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      DiscordConfig defaults = defaultConfig();
      if (dataDir == null) {
         return defaults;
      }

      try {
         Files.createDirectories(dataDir);
      } catch (IOException e) {
         ((Api) logger.atWarning().withCause(e)).log("Discord config failed to create data directory");
      }

      Path cfgPath = dataDir.resolve(FILE_NAME);
      if (!Files.exists(cfgPath)) {
         writeConfig(cfgPath, defaults, gson, logger);
         return defaults;
      }

      try {
         String json = Files.readString(cfgPath, StandardCharsets.UTF_8);
         DiscordConfig cfg = gson.fromJson(json, DiscordConfig.class);
         if (cfg == null) {
            return defaults;
         }
         cfg.applyDefaults(defaults);
         return cfg;
      } catch (IOException | JsonParseException e) {
         ((Api) logger.atWarning().withCause(e)).log("Discord config failed to read discord.json");
         return defaults;
      }
   }

   public boolean hasBotToken() {
      return this.botToken != null && !this.botToken.isBlank();
   }

   public boolean hasAnyChannelId() {
      if (this.channelsIds == null || this.channelsIds.isEmpty()) {
         return false;
      }
      for (String id : this.channelsIds.values()) {
         if (isChannelIdValid(id)) {
            return true;
         }
      }
      return false;
   }

   public Set<String> getAllChannelIds() {
      if (this.channelsIds == null || this.channelsIds.isEmpty()) {
         return Collections.emptySet();
      }
      Set<String> ids = new HashSet<>();
      for (String id : this.channelsIds.values()) {
         if (isChannelIdValid(id)) {
            ids.add(id.trim());
         }
      }
      return ids;
   }

   public String getChannelId(String language) {
      if (this.channelsIds == null || this.channelsIds.isEmpty()) {
         return null;
      }
      String normalized = normalizeLanguage(language);
      if (normalized == null) {
         return null;
      }
      Map<String, String> normalizedMap = normalizeChannelMap(this.channelsIds);
      String id = normalizedMap.get(normalized);
      if (id != null) {
         return id;
      }
      int dash = normalized.indexOf('-');
      if (dash > 0) {
         id = normalizedMap.get(normalized.substring(0, dash));
      }
      return id;
   }

   public String getLanguageForChannelId(String channelId) {
      if (channelId == null || channelId.isBlank()) {
         return null;
      }
      if (this.channelsIds == null || this.channelsIds.isEmpty()) {
         return null;
      }
      String trimmed = channelId.trim();
      for (Map.Entry<String, String> entry : this.channelsIds.entrySet()) {
         String lang = entry.getKey();
         String id = entry.getValue();
         if (lang == null || id == null) {
            continue;
         }
         if (trimmed.equals(id.trim())) {
            return lang;
         }
      }
      return null;
   }

   public String formatForDiscord(String player, String message) {
      String safePlayer = player == null ? "Unknown" : player;
      String safeMessage = message == null ? "" : message;
      String template = this.gameToDiscordFormat == null ? "{player}: {message}" : this.gameToDiscordFormat;
      return template.replace("{player}", safePlayer).replace("{message}", safeMessage);
   }

   public String formatForGame(String user, String message) {
      String safeUser = user == null ? "Unknown" : user;
      String safeMessage = message == null ? "" : message;
      String template = this.discordToGameFormat == null ? "[DISCORD] {user}: {message}" : this.discordToGameFormat;
      return template.replace("{user}", safeUser).replace("{message}", safeMessage);
   }

   public String formatPlayerJoin(String player) {
      String safePlayer = player == null ? "Unknown" : player;
      String template = this.playerJoinFormat == null ? "**{player}** joined the world" : this.playerJoinFormat;
      return template.replace("{player}", safePlayer);
   }

   public String formatPlayerLeave(String player) {
      String safePlayer = player == null ? "Unknown" : player;
      String template = this.playerLeaveFormat == null ? "**{player}** left the world" : this.playerLeaveFormat;
      return template.replace("{player}", safePlayer);
   }

   public String formatStatus(int count) {
      String template = this.statusFormat == null ? "{count} players online" : this.statusFormat;
      return template.replace("{count}", String.valueOf(count));
   }

   public int getStatusUpdateIntervalSeconds() {
      return Math.max(30, this.statusUpdateIntervalSeconds);
   }

   public boolean isWebhookUrlValid() {
      if (this.webhookUrl == null || this.webhookUrl.isBlank()) {
         return false;
      }
      return this.webhookUrl.startsWith(WEBHOOK_PREFIX);
   }

   private static DiscordConfig defaultConfig() {
      DiscordConfig cfg = new DiscordConfig();
      cfg.botToken = "";
      Map<String, String> channels = new LinkedHashMap<>();
      channels.put("pt-BR", "1461461984238370918");
      channels.put("en-US", "1461461984263680918");
      channels.put("es-MX", "1461461453238370918");
      cfg.channelsIds = channels;
      cfg.gameToDiscordFormat = "**[{player}]**: {message}";
      cfg.discordToGameFormat = "[DISCORD] {user}: {message}";
      cfg.serverEventsEnabled = true;
      cfg.serverStartMessage = "Server Online!";
      cfg.serverStopMessage = "Server Offline!";
      cfg.playerJoinLeaveEnabled = false;
      cfg.playerJoinFormat = "**{player}** joined the world";
      cfg.playerLeaveFormat = "**{player}** left the world";
      cfg.webhookUrl = "";
      cfg.webhookUsername = "Jogando Bem";
      cfg.webhookAvatarUrl = "";
      cfg.useWebhookForChat = false;
      cfg.useWebhookForEvents = false;
      cfg.statusEnabled = true;
      cfg.statusFormat = "{count} players online";
      cfg.statusUpdateIntervalSeconds = 60;
      return cfg;
   }

   private void applyDefaults(DiscordConfig defaults) {
      if (defaults == null) {
         return;
      }
      if (this.botToken == null) {
         this.botToken = defaults.botToken;
      }
      if (this.channelsIds == null) {
         this.channelsIds = defaults.channelsIds;
      }
      if (this.gameToDiscordFormat == null || this.gameToDiscordFormat.isBlank()) {
         this.gameToDiscordFormat = defaults.gameToDiscordFormat;
      }
      if (this.discordToGameFormat == null || this.discordToGameFormat.isBlank()) {
         this.discordToGameFormat = defaults.discordToGameFormat;
      }
      if (this.serverStartMessage == null || this.serverStartMessage.isBlank()) {
         this.serverStartMessage = defaults.serverStartMessage;
      }
      if (this.serverStopMessage == null || this.serverStopMessage.isBlank()) {
         this.serverStopMessage = defaults.serverStopMessage;
      }
      if (this.playerJoinFormat == null || this.playerJoinFormat.isBlank()) {
         this.playerJoinFormat = defaults.playerJoinFormat;
      }
      if (this.playerLeaveFormat == null || this.playerLeaveFormat.isBlank()) {
         this.playerLeaveFormat = defaults.playerLeaveFormat;
      }
      if (this.webhookUrl == null) {
         this.webhookUrl = defaults.webhookUrl;
      }
      if (this.webhookUsername == null || this.webhookUsername.isBlank()) {
         this.webhookUsername = defaults.webhookUsername;
      }
      if (this.webhookAvatarUrl == null) {
         this.webhookAvatarUrl = defaults.webhookAvatarUrl;
      }
      if (this.statusFormat == null || this.statusFormat.isBlank()) {
         this.statusFormat = defaults.statusFormat;
      }
      if (this.statusUpdateIntervalSeconds <= 0) {
         this.statusUpdateIntervalSeconds = defaults.statusUpdateIntervalSeconds;
      }
   }

   private static void writeConfig(Path path, DiscordConfig cfg, Gson gson, HytaleLogger logger) {
      try {
         String json = gson.toJson(cfg);
         Files.writeString(path, json, StandardCharsets.UTF_8, new OpenOption[0]);
      } catch (IOException e) {
         ((Api) logger.atWarning().withCause(e)).log("Discord config failed to write discord.json");
      }
   }

   private static Map<String, String> normalizeChannelMap(Map<String, String> source) {
      Map<String, String> map = new LinkedHashMap<>();
      if (source == null) {
         return map;
      }
      for (Map.Entry<String, String> entry : source.entrySet()) {
         String key = normalizeLanguage(entry.getKey());
         String value = entry.getValue();
         if (key == null || value == null || value.isBlank()) {
            continue;
         }
         map.put(key, value.trim());
      }
      return map;
   }

   private static boolean isChannelIdValid(String id) {
      if (id == null || id.isBlank()) {
         return false;
      }
      for (int i = 0; i < id.length(); i++) {
         if (!Character.isDigit(id.charAt(i))) {
            return false;
         }
      }
      return true;
   }

   private static String normalizeLanguage(String language) {
      if (language == null) {
         return null;
      }
      String normalized = language.trim().toLowerCase(Locale.ROOT);
      return normalized.isEmpty() ? null : normalized;
   }
}

