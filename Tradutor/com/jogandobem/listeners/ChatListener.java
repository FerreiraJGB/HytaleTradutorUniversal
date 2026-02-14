package com.jogandobem.listeners;

import com.jogandobem.LanguageStore;
import com.jogandobem.OpenAiTranslationService;
import com.jogandobem.PendingChatStore;
import com.jogandobem.PendingChatStore.PendingChat;
import com.jogandobem.SocketModels.ChatPayload;
import com.jogandobem.TranslationConfig;
import com.jogandobem.TranslationDispatcher;
import com.jogandobem.TranslationModels.TranslationTarget;
import com.jogandobem.TranslationSocketClient;
import com.jogandobem.discord.DiscordIntegration;
import com.google.gson.Gson;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ChatListener {
   private final TranslationConfig config;
   private final LanguageStore languageStore;
   private final TranslationSocketClient socketClient;
   private final PendingChatStore pendingStore;
   private final OpenAiTranslationService openAiTranslationService;
   private final TranslationDispatcher translationDispatcher;
   private final HytaleLogger logger;
   private final DiscordIntegration discordIntegration;
   private final Gson gson = new Gson();

   public ChatListener(TranslationConfig config,
                       LanguageStore languageStore,
                       TranslationSocketClient socketClient,
                       PendingChatStore pendingStore,
                       OpenAiTranslationService openAiTranslationService,
                       TranslationDispatcher translationDispatcher,
                       HytaleLogger logger,
                       DiscordIntegration discordIntegration) {
      this.config = config;
      this.languageStore = languageStore;
      this.socketClient = socketClient;
      this.pendingStore = pendingStore;
      this.openAiTranslationService = openAiTranslationService;
      this.translationDispatcher = translationDispatcher;
      this.logger = logger;
      this.discordIntegration = discordIntegration;
   }

   public void onChatEvent(PlayerChatEvent chatEvent) {
      if (chatEvent == null) {
         return;
      }

      PlayerRef sender = chatEvent.getSender();
      if (sender == null) {
         return;
      }

      String original = chatEvent.getContent();
      if (original == null) {
         original = "";
      }

      List<PlayerRef> targets = chatEvent.getTargets();
      if (targets == null || targets.isEmpty()) {
         targets = Universe.get().getPlayers();
      }

      if (targets == null) {
         targets = Collections.emptyList();
      }

      chatEvent.setCancelled(true);
      this.languageStore.updateUsername(sender.getUuid(), sender.getUsername());

      Message senderMessage = formatMessage(chatEvent, sender, original);
      sendMessageSafe(sender, senderMessage);

      Map<String, PlayerRef> recipientsByName = new HashMap<>();
      for (PlayerRef target : targets) {
         if (target == null) {
            continue;
         }
         if (sameUuid(sender.getUuid(), target.getUuid())) {
            continue;
         }
         String username = target.getUsername();
         if (username == null || username.isEmpty()) {
            continue;
         }
         recipientsByName.put(username.toLowerCase(Locale.ROOT), target);
      }

      String senderLanguage = resolveLanguage(sender);
      boolean playerTranslationNeeded = !recipientsByName.isEmpty() && shouldTranslate(senderLanguage, recipientsByName);

      if (!playerTranslationNeeded && !recipientsByName.isEmpty()) {
         Message formatted = formatMessage(chatEvent, sender, original);
         for (PlayerRef target : recipientsByName.values()) {
            sendMessageSafe(target, formatted);
         }
      }

      if (this.discordIntegration != null && this.discordIntegration.hasChannelForLanguage(senderLanguage)) {
         this.discordIntegration.handleUntranslatedChat(sender, sender.getUsername(), original, senderLanguage);
      }

      List<TranslationTarget> onlineList = new ArrayList<>();
      if (playerTranslationNeeded) {
         List<PlayerRef> onlinePlayers = Universe.get().getPlayers();
         onlineList.addAll(buildOnlineList(onlinePlayers));
         if (onlineList.isEmpty()) {
            onlineList.addAll(buildOnlineListFromRecipients(recipientsByName, sender));
         }
      }
      if (this.discordIntegration != null) {
         this.discordIntegration.appendDiscordTargets(onlineList, senderLanguage);
      }

      if (onlineList.isEmpty()) {
         return;
      }

      String messageId = generateMessageId();
      PendingChat pending = new PendingChat(sender, chatEvent.getFormatter(), sender.getUsername());
      this.pendingStore.put(messageId, pending);

      ChatPayload payload = new ChatPayload();
      payload.type = "chat";
      payload.serverId = this.config.serverId;
      payload.messageId = messageId;
      payload.textoOriginal = original;
      payload.idiomaOriginal = senderLanguage;
      payload.jogador = sender.getUsername() == null ? "" : sender.getUsername();
      payload.jogadorUuid = sender.getUuid() == null ? "" : sender.getUuid().toString();
      payload.jogadoresOnline = onlineList;

      if (this.config.isDirectTranslationConfigured() && this.openAiTranslationService != null) {
         this.openAiTranslationService.translateAsync(payload)
               .thenAccept(response -> this.translationDispatcher.dispatch(messageId, response))
               .exceptionally(err -> {
                  ((Api) this.logger.atWarning().withCause(err)).log("ChatTranslation OpenAI dispatch failed");
                  this.translationDispatcher.dispatch(messageId, this.openAiTranslationService.buildFallbackResponse(payload));
                  return null;
               });
         return;
      }

      if (!this.config.isWsConfigured()) {
         ((Api) this.logger.atWarning()).log("ChatTranslation disabled: configure openai_api_key or ws_url/server_id");
         return;
      }

      String json = this.gson.toJson(payload);
      this.socketClient.sendChat(json);
   }

   private List<TranslationTarget> buildOnlineList(List<PlayerRef> players) {
      if (players == null || players.isEmpty()) {
         return Collections.emptyList();
      }

      List<TranslationTarget> list = new ArrayList<>();
      for (PlayerRef player : players) {
         if (player == null) {
            continue;
         }
         String username = player.getUsername();
         if (username == null || username.isEmpty()) {
            continue;
         }
         TranslationTarget target = new TranslationTarget();
         target.jogador = username;
         target.idioma = resolveLanguage(player);
         list.add(target);
      }
      return list;
   }

   private Message formatMessage(PlayerChatEvent chatEvent, PlayerRef sender, String content) {
      PlayerChatEvent.Formatter formatter = chatEvent.getFormatter();
      if (formatter == null) {
         formatter = PlayerChatEvent.DEFAULT_FORMATTER;
      }
      return formatter.format(sender, content);
   }

   private String resolveLanguage(PlayerRef player) {
      String lang = this.languageStore.getLanguage(player.getUuid());
      if (lang == null || lang.isEmpty()) {
         lang = player.getLanguage();
      }
      if (lang == null || lang.isEmpty()) {
         lang = this.config.defaultLanguage;
      }
      if (lang == null || lang.isEmpty()) {
         lang = "auto";
      }
      return lang.trim();
   }

   private boolean shouldTranslate(String senderLanguage, Map<String, PlayerRef> recipientsByName) {
      String base = normalizeLanguage(senderLanguage);
      for (PlayerRef player : recipientsByName.values()) {
         String lang = normalizeLanguage(resolveLanguage(player));
         if (base == null) {
            base = lang;
            continue;
         }
         if (lang != null && !base.equals(lang)) {
            return true;
         }
      }
      return false;
   }

   private static String normalizeLanguage(String language) {
      if (language == null) {
         return null;
      }
      String normalized = language.trim().toLowerCase(Locale.ROOT);
      return normalized.isEmpty() ? null : normalized;
   }

   private static boolean sameUuid(UUID a, UUID b) {
      if (a == null || b == null) {
         return false;
      }
      return a.equals(b);
   }

   private static String safeName(PlayerRef player) {
      if (player == null) {
         return "unknown";
      }
      String name = player.getUsername();
      return name == null || name.isBlank() ? "unknown" : name;
   }

   private List<TranslationTarget> buildOnlineListFromRecipients(Map<String, PlayerRef> recipientsByName, PlayerRef sender) {
      Map<String, PlayerRef> unique = new HashMap<>();
      if (sender != null && sender.getUuid() != null) {
         unique.put(sender.getUuid().toString(), sender);
      }
      for (PlayerRef player : recipientsByName.values()) {
         if (player == null || player.getUuid() == null) {
            continue;
         }
         unique.put(player.getUuid().toString(), player);
      }
      if (unique.isEmpty()) {
         return Collections.emptyList();
      }
      List<TranslationTarget> list = new ArrayList<>();
      for (PlayerRef player : unique.values()) {
         String username = player.getUsername();
         if (username == null || username.isEmpty()) {
            continue;
         }
         TranslationTarget target = new TranslationTarget();
         target.jogador = username;
         target.idioma = resolveLanguage(player);
         list.add(target);
      }
      return list;
   }

   private void sendMessageSafe(PlayerRef player, Message message) {
      if (player == null || message == null) {
         return;
      }
      try {
         player.sendMessage(message);
      } catch (Exception e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to send message");
      }
   }

   private String generateMessageId() {
      long now = System.currentTimeMillis();
      int rand = ThreadLocalRandom.current().nextInt();
      return Long.toHexString(now) + "-" + Integer.toHexString(rand);
   }
}
