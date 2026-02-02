package com.jogandobem;

import com.jogandobem.PendingChatStore.PendingChat;
import com.jogandobem.TranslationModels.TranslationResponse;
import com.jogandobem.TranslationModels.TranslationResult;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TranslationDispatcher {
   private final PendingChatStore pendingStore;
   private final HytaleLogger logger;

   public TranslationDispatcher(PendingChatStore pendingStore, HytaleLogger logger) {
      this.pendingStore = pendingStore;
      this.logger = logger;
   }

   public void dispatch(String messageId, TranslationResponse response) {
      if (response == null || response.traducao == null || response.traducao.isEmpty()) {
         return;
      }

      PendingChat pending = this.pendingStore.remove(messageId);
      PlayerRef sender = pending == null ? null : pending.sender;
      String senderName = pending == null ? null : pending.senderName;
      PlayerChatEvent.Formatter formatter = pending == null || pending.formatter == null
            ? PlayerChatEvent.DEFAULT_FORMATTER
            : pending.formatter;

      Map<String, PlayerRef> playersByName = buildPlayersByName();
      if (sender == null) {
         String responseUuid = response.jogadorUuid;
         if (responseUuid != null && !responseUuid.isBlank()) {
            try {
               UUID uuid = UUID.fromString(responseUuid);
               sender = Universe.get().getPlayer(uuid);
            } catch (IllegalArgumentException ignored) {
            }
         }
         if (sender == null) {
            String responseName = response.jogador;
            if (responseName != null && !responseName.isBlank()) {
               sender = playersByName.get(responseName.toLowerCase(Locale.ROOT));
            }
         }
      }
      if (sender != null && (senderName == null || senderName.isBlank())) {
         senderName = sender.getUsername();
      }
      if (senderName == null || senderName.isBlank()) {
         senderName = response.jogador;
      }

      List<TranslationResult> items = response.traducao;
      for (TranslationResult item : items) {
         if (item == null) {
            continue;
         }
         String targetName = item.jogador;
         if (targetName == null || targetName.isBlank()) {
            continue;
         }
         if (senderName != null && targetName.equalsIgnoreCase(senderName)) {
            continue;
         }
         PlayerRef target = playersByName.get(targetName.toLowerCase(Locale.ROOT));
         if (target == null) {
            continue;
         }
         String text = item.textoTraduzido;
         if (text == null) {
            continue;
         }
         Message message;
         if (sender != null) {
            message = formatter.format(sender, text);
         } else if (senderName != null && !senderName.isBlank()) {
            message = Message.raw(senderName + ": " + text);
         } else {
            message = Message.raw(text);
         }
         try {
            target.sendMessage(message);
         } catch (Exception e) {
            ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to send translated message");
         }
      }
   }

   private Map<String, PlayerRef> buildPlayersByName() {
      List<PlayerRef> players = Universe.get().getPlayers();
      Map<String, PlayerRef> map = new HashMap<>();
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
}
