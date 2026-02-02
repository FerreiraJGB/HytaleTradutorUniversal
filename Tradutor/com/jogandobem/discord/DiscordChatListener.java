package com.jogandobem.discord;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import java.util.Set;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class DiscordChatListener extends ListenerAdapter {
   private volatile DiscordConfig config;
   private final DiscordIntegration integration;
   private final HytaleLogger logger;
   private volatile Set<String> allowedChannels;

   public DiscordChatListener(DiscordConfig config, DiscordIntegration integration, HytaleLogger logger) {
      this.config = config;
      this.integration = integration;
      this.logger = logger;
      this.allowedChannels = config == null ? Set.of() : config.getAllChannelIds();
   }

   public void setConfig(DiscordConfig config) {
      this.config = config;
      refreshAllowedChannels();
   }

   public void refreshAllowedChannels() {
      DiscordConfig cfg = this.config;
      this.allowedChannels = cfg == null ? Set.of() : cfg.getAllChannelIds();
   }

   @Override
   public void onMessageReceived(MessageReceivedEvent event) {
      if (event == null || event.getAuthor() == null) {
         return;
      }
      if (event.getAuthor().isBot()) {
         return;
      }
      String channelId = event.getChannel().getId();
      if (channelId == null || !this.allowedChannels.contains(channelId)) {
         return;
      }
      String username = event.getAuthor().getName();
      String content = event.getMessage().getContentDisplay();
      if (content == null || content.trim().isEmpty()) {
         return;
      }
      String sanitized = DiscordMessageSanitizer.sanitizeForGame(content);
      if (this.integration == null) {
         ((Api) this.logger.atWarning()).log("Discord integration missing, cannot relay message");
         return;
      }
      this.integration.handleIncomingDiscordMessage(channelId, username, sanitized);
   }
}

