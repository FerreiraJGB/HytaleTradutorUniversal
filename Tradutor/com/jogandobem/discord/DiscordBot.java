package com.jogandobem.discord;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

public final class DiscordBot {
   private final HytaleLogger logger;
   private JDA jda;

   public DiscordBot(HytaleLogger logger) {
      this.logger = logger;
   }

   public void start(String botToken) {
      if (botToken == null || botToken.isBlank()) {
         ((Api) this.logger.atWarning()).log("Discord bot token not configured.");
         return;
      }
      try {
         this.jda = JDABuilder.createLight(botToken)
               .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
               .build();
         ((Api) this.logger.atInfo()).log("Discord bot started, waiting for connection...");
      } catch (Exception e) {
         this.jda = null;
         ((Api) this.logger.atWarning().withCause(e)).log("Discord bot failed to start");
      }
   }

   public void shutdown() {
      if (this.jda == null) {
         return;
      }
      try {
         this.jda.shutdown();
      } catch (Exception e) {
         ((Api) this.logger.atWarning().withCause(e)).log("Discord bot shutdown failed");
      } finally {
         this.jda = null;
      }
   }

   public boolean isReady() {
      return this.jda != null && this.jda.getStatus() == JDA.Status.CONNECTED;
   }

   public JDA getJda() {
      return this.jda;
   }

   public void sendMessage(String channelId, String content) {
      if (this.jda == null) {
         ((Api) this.logger.atWarning()).log("Discord bot not connected (sendMessage)");
         return;
      }
      if (channelId == null || channelId.isBlank()) {
         return;
      }
      TextChannel channel = this.jda.getTextChannelById(channelId);
      if (channel == null) {
         ((Api) this.logger.atWarning()).log("Discord channel not found: " + channelId);
         return;
      }
      channel.sendMessage(content)
            .queue(
                  msg -> { },
                  err -> ((Api) this.logger.atWarning().withCause(err)).log("Failed to send Discord message")
            );
   }

   public void sendMessageSync(String channelId, String content, long timeoutSeconds) {
      if (this.jda == null) {
         ((Api) this.logger.atWarning()).log("Discord bot not connected (sendMessageSync)");
         return;
      }
      if (channelId == null || channelId.isBlank()) {
         return;
      }
      TextChannel channel = this.jda.getTextChannelById(channelId);
      if (channel == null) {
         ((Api) this.logger.atWarning()).log("Discord channel not found: " + channelId);
         return;
      }
      try {
         channel.sendMessage(content).submit().get(timeoutSeconds, TimeUnit.SECONDS);
      } catch (TimeoutException e) {
         ((Api) this.logger.atWarning()).log("Discord message send timed out after " + timeoutSeconds + "s");
      } catch (ExecutionException e) {
         ((Api) this.logger.atWarning().withCause(e)).log("Discord message send failed");
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         ((Api) this.logger.atWarning()).log("Discord message send interrupted");
      }
   }
}

