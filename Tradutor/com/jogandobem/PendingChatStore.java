package com.jogandobem;

import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class PendingChatStore {
   public static final class PendingChat {
      public final PlayerRef sender;
      public final PlayerChatEvent.Formatter formatter;
      public final String senderName;

      public PendingChat(PlayerRef sender, PlayerChatEvent.Formatter formatter, String senderName) {
         this.sender = sender;
         this.formatter = formatter;
         this.senderName = senderName;
      }
   }

   private final ConcurrentHashMap<String, PendingChat> pending = new ConcurrentHashMap<>();
   private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
   private final long ttlMs;

   public PendingChatStore(int ttlSeconds) {
      this.ttlMs = Math.max(5, ttlSeconds) * 1000L;
   }

   public void put(String messageId, PendingChat chat) {
      if (messageId == null || chat == null) {
         return;
      }
      this.pending.put(messageId, chat);
      this.scheduler.schedule(() -> this.pending.remove(messageId, chat), this.ttlMs, TimeUnit.MILLISECONDS);
   }

   public PendingChat remove(String messageId) {
      if (messageId == null) {
         return null;
      }
      return this.pending.remove(messageId);
   }

   public void shutdown() {
      this.scheduler.shutdownNow();
   }
}
