package com.jogandobem.listeners;

import com.jogandobem.AutoLanguageMappings;
import com.jogandobem.IpInfoService;
import com.jogandobem.LanguageStore;
import com.jogandobem.MessageStore;
import com.jogandobem.TranslationConfig;
import com.jogandobem.IpInfoService.IpInfoResult;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import io.netty.channel.Channel;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;

public class PlayerConnectListener {
   private final TranslationConfig config;
   private final LanguageStore languageStore;
   private final MessageStore messageStore;
   private final IpInfoService ipInfoService;
   private final HytaleLogger logger;

   public PlayerConnectListener(TranslationConfig config, LanguageStore languageStore, MessageStore messageStore, IpInfoService ipInfoService, HytaleLogger logger) {
      this.config = config;
      this.languageStore = languageStore;
      this.messageStore = messageStore;
      this.ipInfoService = ipInfoService;
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

      tryAutoDetectLanguage(player);

      if (!this.config.warnOnJoin) {
         return;
      }
      String warn = this.messageStore.getString("warn_on_join", resolveMessageLanguage(player), this.config);
      if (warn == null || warn.isBlank()) {
         warn = this.config.warnMessage;
      }
      if (warn == null || warn.isBlank()) {
         return;
      }
      try {
         player.sendMessage(Message.raw(warn));
      } catch (Exception e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to send warn message");
      }
   }

   private void tryAutoDetectLanguage(PlayerRef player) {
      if (player == null || this.config == null || this.languageStore == null || this.ipInfoService == null) {
         return;
      }
      UUID uuid = player.getUuid();
      if (uuid == null) {
         return;
      }
      if (!this.config.hasIpInfoToken()) {
         return;
      }
      if (this.languageStore.hasEntry(uuid)) {
         return;
      }
      String ip = resolvePlayerIp(player);
      this.ipInfoService.lookup(ip).thenAccept(result -> handleIpInfoResult(player, uuid, ip, result));
   }

   private void handleIpInfoResult(PlayerRef player, UUID uuid, String fallbackIp, IpInfoResult result) {
      if (result == null || player == null || uuid == null) {
         return;
      }
      if (this.languageStore.hasEntry(uuid)) {
         return;
      }
      String countryCode = result.countryCode;
      if (countryCode == null || countryCode.isBlank()) {
         return;
      }
      String language = AutoLanguageMappings.getLanguageForCountry(countryCode);
      if (language == null || language.isBlank()) {
         return;
      }
      String username = player.getUsername();
      String ipToStore = result.ip;
      if (ipToStore == null || ipToStore.isBlank()) {
         ipToStore = fallbackIp == null ? "" : fallbackIp;
      }
      this.languageStore.setLanguage(uuid, username, language, ipToStore);

      if (!player.isValid()) {
         return;
      }
      String message = AutoLanguageMappings.getMessageForLanguage(language);
      if (message == null || message.isBlank()) {
         message = this.messageStore.getString("auto_language_set", language, this.config);
      }
      if (message == null || message.isBlank()) {
         return;
      }
      try {
         player.sendMessage(Message.raw(message));
      } catch (Exception e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to send auto language message");
      }
   }

   private String resolveMessageLanguage(PlayerRef player) {
      if (player == null) {
         return this.messageStore.resolveLanguage(null, this.config);
      }
      String lang = this.languageStore.getLanguage(player.getUuid());
      if (lang == null || lang.isBlank()) {
         lang = player.getLanguage();
      }
      if (lang == null || lang.isBlank()) {
         lang = this.config.defaultLanguage;
      }
      return this.messageStore.resolveLanguage(lang, this.config);
   }

   private String resolvePlayerIp(PlayerRef player) {
      try {
         PacketHandler handler = player.getPacketHandler();
         if (handler == null) {
            return null;
         }
         Channel channel = handler.getChannel();
         if (channel == null) {
            return null;
         }
         SocketAddress address = channel.remoteAddress();
         if (address instanceof InetSocketAddress) {
            InetAddress inet = ((InetSocketAddress) address).getAddress();
            if (inet != null) {
               return inet.getHostAddress();
            }
            return ((InetSocketAddress) address).getHostString();
         }
         if (address != null) {
            String raw = address.toString();
            if (raw == null || raw.isBlank()) {
               return null;
            }
            String candidate = null;
            if (raw.contains("QuicStreamAddress") || raw.contains("remote=")) {
               int remoteIdx = raw.indexOf("remote=");
               if (remoteIdx >= 0) {
                  int start = remoteIdx + "remote=".length();
                  int end = raw.indexOf(',', start);
                  if (end < 0) {
                     end = raw.indexOf('}', start);
                  }
                  if (end < 0) {
                     end = raw.length();
                  }
                  candidate = raw.substring(start, end).trim();
               }
            }
            if (candidate == null || candidate.isBlank()) {
               candidate = raw;
            }
            if (candidate.contains("QuicStreamAddress")) {
               int slash = candidate.indexOf('/');
               if (slash >= 0) {
                  int end = candidate.indexOf(',', slash);
                  if (end < 0) {
                     end = candidate.indexOf('}', slash);
                  }
                  if (end < 0) {
                     end = candidate.length();
                  }
                  candidate = candidate.substring(slash + 1, end).trim();
               } else {
                  return null;
               }
            }
            if (candidate.startsWith("/")) {
               candidate = candidate.substring(1);
            }
            int zoneIdx = candidate.indexOf('%');
            if (zoneIdx > 0) {
               candidate = candidate.substring(0, zoneIdx);
            }
            if (candidate.startsWith("[") && candidate.contains("]")) {
               int endBracket = candidate.indexOf(']');
               if (endBracket > 1) {
                  candidate = candidate.substring(1, endBracket);
               }
            } else {
               int colon = candidate.lastIndexOf(':');
               if (colon > 0 && candidate.indexOf(':') == colon) {
                  candidate = candidate.substring(0, colon);
               }
            }
            return candidate.isBlank() ? null : candidate;
         }
      } catch (Exception e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to resolve player ip");
      }
      return null;
   }
}
