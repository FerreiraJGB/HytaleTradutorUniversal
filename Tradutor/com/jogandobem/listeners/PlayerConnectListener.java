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
import com.hypixel.hytale.server.core.io.netty.NettyUtil;
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
      String username = player.getUsername();
      ((Api) this.logger.atInfo()).log("ChatTranslation auto-detect start user=" + username + " uuid=" + uuid + " ip=" + (ip == null ? "null" : ip));
      this.ipInfoService.lookup(ip).thenAccept(result -> handleIpInfoResult(player, uuid, ip, result));
   }

   private void handleIpInfoResult(PlayerRef player, UUID uuid, String fallbackIp, IpInfoResult result) {
      if (result == null || player == null || uuid == null) {
         ((Api) this.logger.atInfo()).log("ChatTranslation auto-detect failed: ipinfo result null user=" + (player == null ? "null" : player.getUsername()) + " uuid=" + uuid + " ip=" + (fallbackIp == null ? "null" : fallbackIp));
         return;
      }
      if (this.languageStore.hasEntry(uuid)) {
         return;
      }
      String countryCode = result.countryCode;
      if (countryCode == null || countryCode.isBlank()) {
         ((Api) this.logger.atInfo()).log("ChatTranslation auto-detect failed: missing country code user=" + player.getUsername() + " uuid=" + uuid + " ip=" + (fallbackIp == null ? "null" : fallbackIp));
         return;
      }
      String language = AutoLanguageMappings.getLanguageForCountry(countryCode);
      if (language == null || language.isBlank()) {
         ((Api) this.logger.atInfo()).log("ChatTranslation auto-detect failed: no language mapping country=" + countryCode + " user=" + player.getUsername() + " uuid=" + uuid);
         return;
      }
      String username = player.getUsername();
      String ipToStore = result.ip;
      if (ipToStore == null || ipToStore.isBlank()) {
         ipToStore = fallbackIp == null ? "" : fallbackIp;
      }
      ((Api) this.logger.atInfo()).log("ChatTranslation auto-detect set language=" + language + " country=" + countryCode + " user=" + username + " uuid=" + uuid + " ip=" + (ipToStore == null ? "null" : ipToStore));
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
         SocketAddress nettyRemote = NettyUtil.getRemoteSocketAddress(channel);
         String ip = resolveSocketAddress(nettyRemote);
         if (ip != null) {
            ((Api) this.logger.atInfo()).log("ChatTranslation resolved player ip from NettyUtil.getRemoteSocketAddress ip=" + ip + " raw=" + safeString(nettyRemote));
            return ip;
         }
         ip = resolveSocketAddress(channel.remoteAddress());
         if (ip != null) {
            ((Api) this.logger.atInfo()).log("ChatTranslation resolved player ip from channel.remoteAddress ip=" + ip + " raw=" + safeString(channel.remoteAddress()));
            return ip;
         }
         Channel parent = channel.parent();
         if (parent != null) {
            ip = resolveSocketAddress(parent.remoteAddress());
            if (ip != null) {
               ((Api) this.logger.atInfo()).log("ChatTranslation resolved player ip from channel.parent.remoteAddress ip=" + ip + " raw=" + safeString(parent.remoteAddress()));
               return ip;
            }
         }
         String identifier = handler.getIdentifier();
         ip = resolveRawAddress(identifier);
         if (ip != null) {
            ((Api) this.logger.atInfo()).log("ChatTranslation resolved player ip from handler identifier ip=" + ip + " raw=" + safeString(identifier));
            return ip;
         }
         ((Api) this.logger.atInfo()).log("ChatTranslation failed to resolve player ip nettyRemote=" + safeString(nettyRemote) + " remote=" + safeString(channel.remoteAddress()) + " parent=" + safeString(parent == null ? null : parent.remoteAddress()) + " identifier=" + safeString(handler.getIdentifier()));
      } catch (Exception e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation failed to resolve player ip");
      }
      return null;
   }

   private String resolveSocketAddress(SocketAddress address) {
      if (address == null) {
         return null;
      }
      if (address instanceof InetSocketAddress) {
         InetAddress inet = ((InetSocketAddress) address).getAddress();
         if (inet != null) {
            return inet.getHostAddress();
         }
         String host = ((InetSocketAddress) address).getHostString();
         return host == null || host.isBlank() ? null : host;
      }
      String raw = address.toString();
      if (raw == null || raw.isBlank()) {
         return null;
      }
      String candidate = resolveRawAddress(raw);
      if (candidate == null) {
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
      if (candidate.isBlank() || !isLikelyIpChars(candidate)) {
         return null;
      }
      return candidate;
   }

   private String resolveRawAddress(String raw) {
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
         candidate = extractAddressFromRaw(raw);
      }
      if (candidate == null || candidate.isBlank()) {
         return null;
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
      if (candidate.isBlank() || !isLikelyIpChars(candidate)) {
         return null;
      }
      return candidate;
   }

   private String extractAddressFromRaw(String raw) {
      int slash = raw.indexOf('/');
      if (slash < 0 || slash + 1 >= raw.length()) {
         return null;
      }
      int start = slash + 1;
      if (raw.charAt(start) == '[') {
         int endBracket = raw.indexOf(']', start + 1);
         if (endBracket > start + 1) {
            return raw.substring(start, endBracket + 1).trim();
         }
      }
      int end = raw.length();
      int comma = raw.indexOf(',', start);
      if (comma >= 0) {
         end = Math.min(end, comma);
      }
      int space = raw.indexOf(' ', start);
      if (space >= 0) {
         end = Math.min(end, space);
      }
      int paren = raw.indexOf(')', start);
      if (paren >= 0) {
         end = Math.min(end, paren);
      }
      int brace = raw.indexOf('}', start);
      if (brace >= 0) {
         end = Math.min(end, brace);
      }
      if (end <= start) {
         return null;
      }
      String candidate = raw.substring(start, end).trim();
      return candidate.isEmpty() ? null : candidate;
   }

   private static String safeString(Object value) {
      return value == null ? "null" : value.toString();
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
}
