package com.jogandobem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LanguageStore {
   private static final String FILE_NAME = "languages.json";

   private final Path filePath;
   private final Gson gson;
   private final HytaleLogger logger;
   private final ConcurrentHashMap<String, PlayerLanguage> players = new ConcurrentHashMap<>();
   private final Object fileLock = new Object();

   private LanguageStore(Path filePath, Gson gson, HytaleLogger logger) {
      this.filePath = filePath;
      this.gson = gson;
      this.logger = logger;
   }

   public static LanguageStore loadOrCreate(Path dataDir, HytaleLogger logger) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      if (dataDir == null) {
         Path fallback = Path.of(".");
         LanguageStore store = new LanguageStore(fallback.resolve(FILE_NAME), gson, logger);
         store.reload();
         return store;
      }
      try {
         Files.createDirectories(dataDir);
      } catch (IOException e) {
         ((Api) logger.atWarning().withCause(e)).log("ChatTranslation failed to create data directory");
      }
      Path filePath = dataDir.resolve(FILE_NAME);
      LanguageStore store = new LanguageStore(filePath, gson, logger);
      store.reload();
      return store;
   }

   public String getLanguage(UUID uuid) {
      if (uuid == null) {
         return null;
      }
      PlayerLanguage entry = this.players.get(uuid.toString());
      return entry == null ? null : entry.language;
   }

   public boolean hasEntry(UUID uuid) {
      if (uuid == null) {
         return false;
      }
      return this.players.containsKey(uuid.toString());
   }

   public void setLanguage(UUID uuid, String username, String language) {
      setLanguage(uuid, username, language, null);
   }

   public void setLanguage(UUID uuid, String username, String language, String ip) {
      if (uuid == null) {
         return;
      }
      String key = uuid.toString();
      if (language == null || language.isBlank()) {
         this.players.remove(key);
         save();
         return;
      }

      PlayerLanguage entry = new PlayerLanguage();
      PlayerLanguage existing = this.players.get(key);
      if (existing != null && (ip == null || ip.isBlank())) {
         entry.ip = existing.ip;
      } else {
         entry.ip = ip == null ? "" : ip;
      }
      entry.username = username == null ? "" : username;
      entry.language = language.trim();
      this.players.put(key, entry);
      save();
   }

   public void clearLanguage(UUID uuid) {
      if (uuid == null) {
         return;
      }
      this.players.remove(uuid.toString());
      save();
   }

   public void updateUsername(UUID uuid, String username) {
      if (uuid == null || username == null || username.isBlank()) {
         return;
      }
      PlayerLanguage entry = this.players.get(uuid.toString());
      if (entry == null) {
         return;
      }
      if (entry.username == null || !entry.username.equals(username)) {
         entry.username = username;
         save();
      }
   }

   public void reload() {
      Map<String, PlayerLanguage> loaded = readFile();
      if (loaded == null) {
         return;
      }
      this.players.clear();
      this.players.putAll(loaded);
   }

   private void save() {
      synchronized (this.fileLock) {
         LanguageData data = new LanguageData();
         data.players = new HashMap<>(this.players);
         try {
            String json = this.gson.toJson(data);
            Files.writeString(this.filePath, json, StandardCharsets.UTF_8, new OpenOption[0]);
         } catch (IOException e) {
            ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to write languages.json");
         }
      }
   }

   private Map<String, PlayerLanguage> readFile() {
      if (!Files.exists(this.filePath)) {
         save();
         return new HashMap<>();
      }
      synchronized (this.fileLock) {
         try {
            String json = Files.readString(this.filePath, StandardCharsets.UTF_8);
            LanguageData data = this.gson.fromJson(json, LanguageData.class);
            if (data != null && data.players != null) {
               return new HashMap<>(data.players);
            }
            return new HashMap<>();
         } catch (IOException | JsonParseException e) {
            ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to read languages.json");
            return null;
         }
      }
   }

   private static final class LanguageData {
      public Map<String, PlayerLanguage> players = new HashMap<>();
   }

   public static final class PlayerLanguage {
      public String username;
      public String language;
      public String ip;
   }
}
