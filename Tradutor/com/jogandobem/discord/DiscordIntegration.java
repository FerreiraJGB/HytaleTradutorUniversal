package com.jogandobem.discord;

import com.jogandobem.LanguageStore;
import com.jogandobem.OpenAiTranslationService;
import com.jogandobem.SocketModels.ChatPayload;
import com.jogandobem.TranslationConfig;
import com.jogandobem.TranslationModels.TranslationResponse;
import com.jogandobem.TranslationModels.TranslationResult;
import com.jogandobem.TranslationModels.TranslationTarget;
import com.jogandobem.TranslationSocketClient;
import com.google.gson.Gson;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class DiscordIntegration {
   private final HytaleLogger logger;
   private final LanguageStore languageStore;
   private TranslationConfig translationConfig;
   private TranslationSocketClient socketClient;
   private OpenAiTranslationService openAiTranslationService;
   private DiscordConfig config;
   private DiscordBot bot;
   private DiscordWebhookManager webhookManager;
   private DiscordStatusManager statusManager;
   private DiscordGameBroadcaster broadcaster;
   private DiscordChatListener discordChatListener;
   private boolean discordListenerRegistered;
   private final Set<String> warnedMissingChannels = ConcurrentHashMap.newKeySet();
   private final ConcurrentHashMap<String, DiscordPending> pendingDiscord = new ConcurrentHashMap<>();
   private final ScheduledExecutorService pendingScheduler = Executors.newSingleThreadScheduledExecutor();
   private final Gson gson = new Gson();
   private static final String DISCORD_TARGET_PREFIX = "__discord__:";

   public DiscordIntegration(java.nio.file.Path dataDir,
                             HytaleLogger logger,
                             LanguageStore languageStore,
                             TranslationConfig translationConfig) {
      this.logger = logger;
      this.languageStore = languageStore;
      this.translationConfig = translationConfig;
      this.config = DiscordConfig.loadOrCreate(dataDir, logger);
   }

   public void setSocketClient(TranslationSocketClient socketClient) {
      this.socketClient = socketClient;
   }

   public void setOpenAiTranslationService(OpenAiTranslationService openAiTranslationService) {
      this.openAiTranslationService = openAiTranslationService;
   }

   public void start(JavaPlugin plugin) {
      if (this.config == null) {
         ((Api) this.logger.atWarning()).log("Discord integration disabled (config missing)");
         return;
      }

      this.webhookManager = new DiscordWebhookManager(this.config, this.logger);

      if (this.config.hasBotToken()) {
         this.bot = new DiscordBot(this.logger);
         this.bot.start(this.config.botToken);
         this.broadcaster = new DiscordGameBroadcaster(this.config, this.logger);
         this.statusManager = new DiscordStatusManager(getJda(), this::getOnlineCountSafe, this.config, this.logger);
         this.discordChatListener = new DiscordChatListener(this.config, this, this.logger);
         JDA jda = getJda();
         if (jda != null) {
            jda.addEventListener(new ReadyListener());
         }
      } else {
         ((Api) this.logger.atWarning()).log("Discord bot token not configured; bot features disabled.");
      }

      if (plugin != null) {
         DiscordPlayerListener listener = new DiscordPlayerListener();
         plugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, listener::onPlayerConnect);
         plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, listener::onPlayerDisconnect);
      }

      if (!this.config.hasBotToken() && this.config.serverEventsEnabled && this.webhookManager != null
            && this.webhookManager.isEnabled()) {
         sendServerStartMessage();
      }
   }

   public void shutdown() {
      if (this.config != null && this.config.serverEventsEnabled) {
         sendServerStopMessage();
      }
      if (this.statusManager != null) {
         this.statusManager.shutdown();
         this.statusManager = null;
      }
      if (this.webhookManager != null) {
         this.webhookManager.shutdown();
         this.webhookManager = null;
      }
      if (this.bot != null) {
         this.bot.shutdown();
         this.bot = null;
      }
      this.pendingScheduler.shutdownNow();
      this.pendingDiscord.clear();
   }

   public void reload(java.nio.file.Path dataDir, TranslationConfig translationConfig) {
      this.translationConfig = translationConfig;
      DiscordConfig refreshed = DiscordConfig.loadOrCreate(dataDir, this.logger);
      if (refreshed == null) {
         return;
      }
      this.config = refreshed;
      if (this.discordChatListener != null) {
         this.discordChatListener.setConfig(refreshed);
      }
      if (this.broadcaster != null) {
         this.broadcaster.setConfig(refreshed);
      }
   }

   public boolean hasChannelForLanguage(String language) {
      if (this.config == null) {
         return false;
      }
      if (this.config.useWebhookForChat && this.webhookManager != null && this.webhookManager.isEnabled()) {
         return true;
      }
      String channelId = this.config.getChannelId(language);
      return channelId != null && !channelId.isBlank();
   }

   public boolean shouldTranslateForDiscord(String senderLanguage) {
      if (this.config == null || this.config.channelsIds == null || this.config.channelsIds.isEmpty()) {
         return false;
      }
      String base = normalizeLanguage(senderLanguage);
      for (Map.Entry<String, String> entry : this.config.channelsIds.entrySet()) {
         String language = entry.getKey();
         String channelId = entry.getValue();
         if (language == null || language.isBlank() || !isChannelIdValid(channelId)) {
            continue;
         }
         String normalized = normalizeLanguage(language);
         if (normalized == null) {
            continue;
         }
         if (base == null || !base.equals(normalized)) {
            return true;
         }
      }
      return false;
   }

   public boolean appendDiscordTargets(List<TranslationTarget> targets, String senderLanguage) {
      if (targets == null || this.config == null || this.config.channelsIds == null || this.config.channelsIds.isEmpty()) {
         return false;
      }
      String base = normalizeLanguage(senderLanguage);
      Set<String> seen = new LinkedHashSet<>();
      boolean added = false;
      for (Map.Entry<String, String> entry : this.config.channelsIds.entrySet()) {
         String language = entry.getKey();
         String channelId = entry.getValue();
         if (language == null || language.isBlank() || !isChannelIdValid(channelId)) {
            continue;
         }
         String normalized = normalizeLanguage(language);
         if (normalized == null || !seen.add(normalized)) {
            continue;
         }
         if (base != null && base.equals(normalized)) {
            continue;
         }
         TranslationTarget target = new TranslationTarget();
         target.jogador = buildDiscordTargetName(normalized);
         target.idioma = language.trim();
         targets.add(target);
         added = true;
      }
      return added;
   }

   public void handleIncomingDiscordMessage(String channelId, String username, String message) {
      if (this.config == null || message == null || message.isBlank()) {
         return;
      }
      String channelLanguage = this.config.getLanguageForChannelId(channelId);
      if (channelLanguage == null || channelLanguage.isBlank()) {
         return;
      }

      List<PlayerRef> players = Universe.get().getPlayers();
      if (players == null || players.isEmpty()) {
         return;
      }

      List<TranslationTarget> targets = new ArrayList<>();
      String baseLang = normalizeLanguage(channelLanguage);
      boolean translationNeeded = false;

      for (PlayerRef player : players) {
         if (player == null) {
            continue;
         }
         String name = player.getUsername();
         if (name == null || name.isBlank()) {
            continue;
         }
         TranslationTarget target = new TranslationTarget();
         target.jogador = name;
         target.idioma = resolveLanguage(player);
         targets.add(target);

         String playerLang = normalizeLanguage(target.idioma);
         if (baseLang == null || playerLang == null || !baseLang.equals(playerLang)) {
            translationNeeded = true;
         }
      }

      if (targets.isEmpty()) {
         return;
      }

      if (!translationNeeded) {
         if (this.broadcaster != null) {
            this.broadcaster.broadcastToGame(username, message);
         }
         return;
      }

      boolean canUseOpenAi = this.openAiTranslationService != null
            && this.translationConfig != null
            && this.translationConfig.isDirectTranslationConfigured();
      boolean canUseWs = this.socketClient != null
            && this.translationConfig != null
            && this.translationConfig.isWsConfigured();

      if (!canUseOpenAi && !canUseWs) {
         if (this.broadcaster != null) {
            this.broadcaster.broadcastToGame(username, message);
         }
         return;
      }

      String messageId = "discord:" + generateMessageId();
      trackPendingDiscord(messageId, new DiscordPending(username, message));

      ChatPayload payload = new ChatPayload();
      payload.type = "chat";
      payload.serverId = this.translationConfig.serverId;
      payload.messageId = messageId;
      payload.textoOriginal = message;
      payload.idiomaOriginal = channelLanguage;
      payload.jogador = username == null ? "" : username;
      payload.jogadorUuid = "";
      payload.jogadoresOnline = targets;

      if (canUseOpenAi) {
         this.openAiTranslationService.translateAsync(payload)
               .thenAccept(response -> handleTranslatedDiscordToGame(messageId, response))
               .exceptionally(err -> {
                  ((Api) this.logger.atWarning().withCause(err)).log("Discord OpenAI translation failed");
                  handleTranslatedDiscordToGame(messageId, this.openAiTranslationService.buildFallbackResponse(payload));
                  return null;
               });
         return;
      }

      String json = this.gson.toJson(payload);
      this.socketClient.sendChat(json);
   }

   public void handleTranslatedDiscordToGame(String messageId, TranslationResponse response) {
      DiscordPending pending = this.pendingDiscord.remove(messageId);
      if (pending == null) {
         return;
      }
      if (this.config == null) {
         return;
      }
      if (response == null || response.traducao == null || response.traducao.isEmpty()) {
         if (this.broadcaster != null) {
            this.broadcaster.broadcastToGame(pending.user, pending.message);
         }
         return;
      }

      Map<String, PlayerRef> playersByName = buildPlayersByName();
      for (TranslationResult item : response.traducao) {
         if (item == null) {
            continue;
         }
         String targetName = item.jogador;
         if (targetName == null || targetName.isBlank()) {
            continue;
         }
         PlayerRef target = playersByName.get(targetName.toLowerCase(Locale.ROOT));
         if (target == null) {
            continue;
         }
         String text = item.textoTraduzido;
         if (text == null || text.isBlank()) {
            text = pending.message;
         }
         String sanitized = DiscordMessageSanitizer.sanitizeForGame(text);
         String formatted = this.config.formatForGame(pending.user, sanitized);
         try {
            target.sendMessage(Message.raw(formatted));
         } catch (Exception e) {
            ((Api) this.logger.atWarning().withCause(e)).log("Failed to send Discord message to player");
         }
      }
   }

   public void handleTranslatedChat(TranslationResponse response,
                                    Map<String, PlayerRef> playersByName,
                                    PlayerRef sender,
                                    String senderName) {
      if (response == null || response.traducao == null || response.traducao.isEmpty()) {
         return;
      }
      if (this.config == null) {
         return;
      }

      Map<String, String> languageToText = new LinkedHashMap<>();
      for (TranslationResult item : response.traducao) {
         if (item == null) {
            continue;
         }
         String targetName = item.jogador;
         if (targetName == null || targetName.isBlank()) {
            continue;
         }
         String text = item.textoTraduzido;
         if (text == null) {
            continue;
         }
         String language = parseDiscordTargetLanguage(targetName);
         if (language == null) {
            continue;
         }
         if (!languageToText.containsKey(language)) {
            languageToText.put(language, text);
         }
      }

      if (languageToText.isEmpty()) {
         return;
      }

      String resolvedSender = resolveSenderName(sender, senderName, response);
      for (Map.Entry<String, String> entry : languageToText.entrySet()) {
         if (!hasChannelForLanguage(entry.getKey())) {
            continue;
         }
         sendChatToDiscord(resolvedSender, entry.getValue(), entry.getKey());
      }
   }

   public void handleUntranslatedChat(PlayerRef sender, String senderName, String message, String senderLanguage) {
      if (message == null || message.isBlank()) {
         return;
      }
      if (this.config == null) {
         return;
      }
      String language = normalizeLanguage(senderLanguage);
      if (language == null && sender != null) {
         language = normalizeLanguage(resolveLanguage(sender));
      }
      if (language == null) {
         return;
      }
      String resolvedSender = resolveSenderName(sender, senderName, null);
      sendChatToDiscord(resolvedSender, message, language);
   }

   private void sendChatToDiscord(String senderName, String message, String language) {
      if (this.config == null || message == null) {
         return;
      }
      String sanitized = DiscordMessageSanitizer.sanitizeForDiscord(message);
      String formatted = this.config.formatForDiscord(senderName, sanitized);

      if (this.config.useWebhookForChat && this.webhookManager != null && this.webhookManager.isEnabled()) {
         this.webhookManager.sendMessage(formatted);
         return;
      }

      if (this.bot == null || !this.bot.isReady()) {
         return;
      }
      String channelId = this.config.getChannelId(language);
      if (channelId == null || channelId.isBlank()) {
         warnMissingChannel(language);
         return;
      }
      this.bot.sendMessage(channelId, formatted);
   }

   private void sendServerStartMessage() {
      if (this.config == null || !this.config.serverEventsEnabled) {
         return;
      }
      sendServerEvent(this.config.serverStartMessage, false);
      ((Api) this.logger.atInfo()).log("Sent server start message to Discord.");
   }

   private void sendServerStopMessage() {
      if (this.config == null || !this.config.serverEventsEnabled) {
         return;
      }
      sendServerEvent(this.config.serverStopMessage, true);
      ((Api) this.logger.atInfo()).log("Sent server stop message to Discord.");
   }

   private void sendServerEvent(String message, boolean sync) {
      if (message == null || message.isBlank()) {
         return;
      }
      String sanitized = DiscordMessageSanitizer.sanitizeForDiscord(message);
      if (this.config.useWebhookForEvents && this.webhookManager != null && this.webhookManager.isEnabled()) {
         this.webhookManager.sendMessage(sanitized);
         if (sync) {
            try {
               Thread.sleep(1000L);
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
            }
         }
         return;
      }

      if (this.bot == null || !this.bot.isReady()) {
         return;
      }
      for (String channelId : this.config.getAllChannelIds()) {
         if (sync) {
            this.bot.sendMessageSync(channelId, sanitized, 5L);
         } else {
            this.bot.sendMessage(channelId, sanitized);
         }
      }
   }

   private void handlePlayerJoin(PlayerRef player) {
      if (player == null || this.config == null || !this.config.playerJoinLeaveEnabled) {
         return;
      }
      String name = player.getUsername();
      String sanitized = DiscordMessageSanitizer.sanitizeForDiscord(name == null ? "Unknown" : name);
      String message = this.config.formatPlayerJoin(sanitized);
      sendServerEvent(message, false);
   }

   private void handlePlayerLeave(PlayerRef player) {
      if (player == null || this.config == null || !this.config.playerJoinLeaveEnabled) {
         return;
      }
      String name = player.getUsername();
      String sanitized = DiscordMessageSanitizer.sanitizeForDiscord(name == null ? "Unknown" : name);
      String message = this.config.formatPlayerLeave(sanitized);
      sendServerEvent(message, false);
   }

   private void trackPendingDiscord(String messageId, DiscordPending pending) {
      if (messageId == null || pending == null) {
         return;
      }
      this.pendingDiscord.put(messageId, pending);
      int ttlSeconds = this.translationConfig == null ? 30 : this.translationConfig.pendingTtlSeconds;
      int delay = Math.max(5, ttlSeconds);
      this.pendingScheduler.schedule(() -> this.pendingDiscord.remove(messageId, pending), delay, TimeUnit.SECONDS);
   }

   private Map<String, PlayerRef> buildPlayersByName() {
      List<PlayerRef> players = Universe.get().getPlayers();
      Map<String, PlayerRef> map = new LinkedHashMap<>();
      if (players == null) {
         return map;
      }
      for (PlayerRef player : players) {
         if (player == null) {
            continue;
         }
         String username = player.getUsername();
         if (username == null || username.isBlank()) {
            continue;
         }
         map.put(username.toLowerCase(Locale.ROOT), player);
      }
      return map;
   }

   private String generateMessageId() {
      long now = System.currentTimeMillis();
      int rand = ThreadLocalRandom.current().nextInt();
      return Long.toHexString(now) + "-" + Integer.toHexString(rand);
   }

   private String resolveSenderName(PlayerRef sender, String senderName, TranslationResponse response) {
      String name = senderName;
      if (name == null || name.isBlank()) {
         if (sender != null) {
            name = sender.getUsername();
         }
      }
      if ((name == null || name.isBlank()) && response != null) {
         name = response.jogador;
      }
      return (name == null || name.isBlank()) ? "unknown" : name;
   }

   private String resolveLanguage(PlayerRef player) {
      if (player == null) {
         return null;
      }
      UUID uuid = player.getUuid();
      String lang = uuid == null ? null : this.languageStore.getLanguage(uuid);
      if (lang == null || lang.isBlank()) {
         lang = player.getLanguage();
      }
      if (lang == null || lang.isBlank()) {
         lang = this.translationConfig == null ? null : this.translationConfig.defaultLanguage;
      }
      if (lang == null || lang.isBlank()) {
         lang = "auto";
      }
      return lang.trim();
   }

   private int getOnlineCountSafe() {
      if (this.broadcaster != null) {
         return this.broadcaster.getOnlinePlayerCount();
      }
      return 0;
   }

   private void warnMissingChannel(String language) {
      if (language == null) {
         return;
      }
      String normalized = normalizeLanguage(language);
      if (normalized == null) {
         return;
      }
      if (this.warnedMissingChannels.add(normalized)) {
         ((Api) this.logger.atWarning()).log("Discord channel not configured for language: " + normalized);
      }
   }

   private static String normalizeLanguage(String language) {
      if (language == null) {
         return null;
      }
      String normalized = language.trim().toLowerCase(Locale.ROOT);
      return normalized.isEmpty() ? null : normalized;
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

   private static String buildDiscordTargetName(String language) {
      String normalized = normalizeLanguage(language);
      if (normalized == null) {
         return null;
      }
      return DISCORD_TARGET_PREFIX + normalized;
   }

   private static String parseDiscordTargetLanguage(String targetName) {
      if (targetName == null) {
         return null;
      }
      if (!targetName.startsWith(DISCORD_TARGET_PREFIX)) {
         return null;
      }
      String language = targetName.substring(DISCORD_TARGET_PREFIX.length());
      if (language.isBlank()) {
         return null;
      }
      return normalizeLanguage(language);
   }

   private JDA getJda() {
      return this.bot == null ? null : this.bot.getJda();
   }

   private static final class DiscordPending {
      public final String user;
      public final String message;

      private DiscordPending(String user, String message) {
         this.user = user == null ? "unknown" : user;
         this.message = message == null ? "" : message;
      }
   }

   private final class ReadyListener extends ListenerAdapter {
      @Override
      public void onReady(ReadyEvent event) {
         if (DiscordIntegration.this.bot != null && DiscordIntegration.this.discordChatListener != null
               && !DiscordIntegration.this.discordListenerRegistered) {
            JDA jda = DiscordIntegration.this.getJda();
            if (jda != null) {
               jda.addEventListener(DiscordIntegration.this.discordChatListener);
               DiscordIntegration.this.discordListenerRegistered = true;
               ((Api) DiscordIntegration.this.logger.atInfo()).log("Registered Discord chat listener.");
            }
         }

         if (DiscordIntegration.this.config != null && DiscordIntegration.this.config.serverEventsEnabled) {
            DiscordIntegration.this.sendServerStartMessage();
         }

         if (DiscordIntegration.this.statusManager != null && DiscordIntegration.this.config != null
               && DiscordIntegration.this.config.statusEnabled) {
            DiscordIntegration.this.statusManager.start();
            ((Api) DiscordIntegration.this.logger.atInfo()).log("Started Discord status updates.");
         }
      }
   }

   private final class DiscordPlayerListener {
      public void onPlayerConnect(PlayerConnectEvent event) {
         if (DiscordIntegration.this.broadcaster != null) {
            DiscordIntegration.this.broadcaster.onPlayerConnect(event);
         }
         if (DiscordIntegration.this.statusManager != null) {
            DiscordIntegration.this.statusManager.updateStatusNow();
         }
         PlayerRef player = event == null ? null : event.getPlayerRef();
         DiscordIntegration.this.handlePlayerJoin(player);
      }

      public void onPlayerDisconnect(PlayerDisconnectEvent event) {
         if (DiscordIntegration.this.broadcaster != null) {
            DiscordIntegration.this.broadcaster.onPlayerDisconnect(event);
         }
         if (DiscordIntegration.this.statusManager != null) {
            DiscordIntegration.this.statusManager.updateStatusNow();
         }
         PlayerRef player = event == null ? null : event.getPlayerRef();
         DiscordIntegration.this.handlePlayerLeave(player);
      }
   }
}

