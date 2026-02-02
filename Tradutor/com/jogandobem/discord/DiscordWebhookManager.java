package com.jogandobem.discord;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import net.tinkstav.discordlinkhytale.libs.webhook.WebhookClient;
import net.tinkstav.discordlinkhytale.libs.webhook.send.WebhookMessageBuilder;

public final class DiscordWebhookManager {
   private final DiscordConfig config;
   private final HytaleLogger logger;
   private WebhookClient client;
   private boolean enabled;

   public DiscordWebhookManager(DiscordConfig config, HytaleLogger logger) {
      this.config = config;
      this.logger = logger;
      this.enabled = false;
      init();
   }

   private void init() {
      if (this.config == null) {
         return;
      }
      String url = this.config.webhookUrl;
      if (url == null || url.isBlank()) {
         ((Api) this.logger.atInfo()).log("Discord webhook URL not configured.");
         return;
      }
      if (!this.config.isWebhookUrlValid()) {
         ((Api) this.logger.atWarning()).log("Discord webhook URL invalid. Must start with https://discord.com/api/webhooks/");
         return;
      }
      try {
         this.client = WebhookClient.withUrl(url);
         this.enabled = true;
         ((Api) this.logger.atInfo()).log("Discord webhook client initialized.");
      } catch (IllegalArgumentException e) {
         this.enabled = false;
         ((Api) this.logger.atWarning().withCause(e)).log("Discord webhook init failed");
      }
   }

   public boolean isEnabled() {
      return this.enabled && this.client != null;
   }

   public void sendMessage(String content) {
      if (!isEnabled()) {
         return;
      }
      WebhookMessageBuilder builder = new WebhookMessageBuilder().setContent(content);
      String username = this.config == null ? null : this.config.webhookUsername;
      if (username != null && !username.isBlank()) {
         builder.setUsername(username);
      }
      String avatar = this.config == null ? null : this.config.webhookAvatarUrl;
      if (avatar != null && !avatar.isBlank()) {
         builder.setAvatarUrl(avatar);
      }
      this.client.send(builder.build());
   }

   public void shutdown() {
      if (this.client == null) {
         return;
      }
      this.client.close();
      this.client = null;
      this.enabled = false;
      ((Api) this.logger.atInfo()).log("Discord webhook client shutdown.");
   }
}

