package com.jogandobem.discord;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DiscordGameBroadcaster {
   private volatile DiscordConfig config;
   private final HytaleLogger logger;
   private final ConcurrentHashMap<UUID, PlayerRef> onlinePlayers = new ConcurrentHashMap<>();

   public DiscordGameBroadcaster(DiscordConfig config, HytaleLogger logger) {
      this.config = config;
      this.logger = logger;
   }

   public void setConfig(DiscordConfig config) {
      this.config = config;
   }

   public void onPlayerConnect(PlayerConnectEvent event) {
      if (event == null) {
         return;
      }
      PlayerRef player = event.getPlayerRef();
      if (player == null || player.getUuid() == null) {
         return;
      }
      this.onlinePlayers.put(player.getUuid(), player);
   }

   public void onPlayerDisconnect(PlayerDisconnectEvent event) {
      if (event == null) {
         return;
      }
      PlayerRef player = event.getPlayerRef();
      if (player == null || player.getUuid() == null) {
         return;
      }
      this.onlinePlayers.remove(player.getUuid());
   }

   public void broadcastToGame(String user, String message) {
      DiscordConfig cfg = this.config;
      if (cfg == null) {
         return;
      }
      String formatted = cfg.formatForGame(user, message);
      Message raw = Message.raw(formatted);
      int sent = 0;
      Collection<PlayerRef> players = this.onlinePlayers.values();
      for (PlayerRef player : players) {
         if (player == null) {
            continue;
         }
         try {
            player.sendMessage(raw);
            sent++;
         } catch (Exception e) {
            ((Api) this.logger.atWarning().withCause(e)).log("Discord broadcast failed for player");
         }
      }
      ((Api) this.logger.atFine()).log("Broadcast Discord message to " + sent + " players");
   }

   public int getOnlinePlayerCount() {
      return this.onlinePlayers.size();
   }
}

