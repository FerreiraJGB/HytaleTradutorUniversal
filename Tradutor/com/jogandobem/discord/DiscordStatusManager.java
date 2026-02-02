package com.jogandobem.discord;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;

public final class DiscordStatusManager {
   private static final long MIN_UPDATE_INTERVAL_MS = 15000L;

   private final JDA jda;
   private final Supplier<Integer> playerCountProvider;
   private final DiscordConfig config;
   private final HytaleLogger logger;
   private final ScheduledExecutorService executor;
   private final AtomicLong lastUpdateTime;
   private final AtomicBoolean started;

   public DiscordStatusManager(JDA jda, Supplier<Integer> playerCountProvider, DiscordConfig config, HytaleLogger logger) {
      this.jda = jda;
      this.playerCountProvider = playerCountProvider;
      this.config = config;
      this.logger = logger;
      this.executor = Executors.newSingleThreadScheduledExecutor();
      this.lastUpdateTime = new AtomicLong(0L);
      this.started = new AtomicBoolean(false);
   }

   public void start() {
      if (this.config == null || !this.config.statusEnabled) {
         ((Api) this.logger.atInfo()).log("Discord status updates disabled.");
         return;
      }
      if (!this.started.compareAndSet(false, true)) {
         return;
      }
      int interval = this.config.getStatusUpdateIntervalSeconds();
      ((Api) this.logger.atInfo()).log("Discord status updates every " + interval + "s");
      this.executor.scheduleAtFixedRate(this::updateStatus, 0L, interval, TimeUnit.SECONDS);
   }

   private void updateStatus() {
      if (this.jda == null || this.jda.getStatus() != JDA.Status.CONNECTED) {
         return;
      }
      try {
         int count = this.playerCountProvider.get();
         String status = this.config.formatStatus(count);
         this.jda.getPresence().setActivity(Activity.customStatus(status));
         this.lastUpdateTime.set(System.currentTimeMillis());
      } catch (Exception e) {
         ((Api) this.logger.atWarning().withCause(e)).log("Discord status update failed");
      }
   }

   public void updateStatusNow() {
      long now = System.currentTimeMillis();
      long last = this.lastUpdateTime.get();
      if (now - last < MIN_UPDATE_INTERVAL_MS) {
         return;
      }
      this.executor.execute(this::updateStatus);
   }

   public void shutdown() {
      this.executor.shutdown();
      try {
         if (!this.executor.awaitTermination(5L, TimeUnit.SECONDS)) {
            this.executor.shutdownNow();
         }
      } catch (InterruptedException e) {
         this.executor.shutdownNow();
         Thread.currentThread().interrupt();
      }
      ((Api) this.logger.atInfo()).log("Discord status manager shutdown.");
   }
}

