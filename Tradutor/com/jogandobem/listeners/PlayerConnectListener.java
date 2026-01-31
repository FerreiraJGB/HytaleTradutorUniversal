package com.jogandobem.listeners;

import com.jogandobem.LanguageStore;
import com.jogandobem.TranslationConfig;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class PlayerConnectListener {
   private final TranslationConfig config;
   private final LanguageStore languageStore;
   private final HytaleLogger logger;

   public PlayerConnectListener(TranslationConfig config, LanguageStore languageStore, HytaleLogger logger) {
      this.config = config;
      this.languageStore = languageStore;
      this.logger = logger;
   }

   public void onPlayerConnect(PlayerConnectEvent event) {
      if (event == null) {
         return;
      }
      PlayerRef player = event.getPlayerRef();
      if (player == null) {
         return;
      }

      this.languageStore.updateUsername(player.getUuid(), player.getUsername());

      if (!this.config.warnOnJoin) {
         return;
      }
      String warn = this.config.warnMessage;
      if (warn == null || warn.isBlank()) {
         return;
      }
      try {
         player.sendMessage(Message.raw(warn));
      } catch (Exception e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to send warn message");
      }
   }
}
