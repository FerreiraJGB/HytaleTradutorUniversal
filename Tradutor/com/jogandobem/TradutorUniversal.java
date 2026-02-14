package com.jogandobem;

import com.jogandobem.commands.LanguageCommand;
import com.jogandobem.commands.ReloadCommand;
import com.jogandobem.discord.DiscordIntegration;
import com.jogandobem.listeners.ChatListener;
import com.jogandobem.listeners.PlayerConnectListener;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.nio.file.Path;
import javax.annotation.Nonnull;

public class TradutorUniversal extends JavaPlugin {
   private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
   private TranslationConfig translationConfig;
   private LanguageStore languageStore;
   private PendingChatStore pendingChatStore;
   private TranslationDispatcher translationDispatcher;
   private TranslationSocketClient socketClient;
   private OpenAiTranslationService openAiTranslationService;
   private MessageStore messageStore;
   private IpInfoService ipInfoService;
   private ChatListener chatListener;
   private DiscordIntegration discordIntegration;

   public TradutorUniversal(@Nonnull JavaPluginInit init) {
      super(init);
   }

   protected void setup() {
      ((Api)LOGGER.atInfo()).log("Setting up plugin " + this.getName());
      Path dataDir = this.getDataDirectory();
      this.translationConfig = TranslationConfig.loadOrCreate(dataDir, this.getLogger());
      this.languageStore = LanguageStore.loadOrCreate(dataDir, this.getLogger());
      this.messageStore = MessageStore.loadOrCreate(dataDir, this.getLogger());
      this.ipInfoService = new IpInfoService(this.translationConfig, this.getLogger());
      this.pendingChatStore = new PendingChatStore(this.translationConfig.pendingTtlSeconds);
      if (isDiscordAvailable()) {
         this.discordIntegration = new DiscordIntegration(dataDir, this.getLogger(), this.languageStore, this.translationConfig);
      } else {
         ((Api)LOGGER.atWarning()).log("Discord integration disabled (JDA not found on classpath).");
         this.discordIntegration = null;
      }
      this.translationDispatcher = new TranslationDispatcher(this.pendingChatStore, this.getLogger(), this.discordIntegration);
      this.openAiTranslationService = new OpenAiTranslationService(this.translationConfig, this.getLogger());
      this.socketClient = new TranslationSocketClient(this.translationConfig, this.getLogger(), this.translationDispatcher);
      if (this.discordIntegration != null) {
         this.discordIntegration.setSocketClient(this.socketClient);
         this.discordIntegration.setOpenAiTranslationService(this.openAiTranslationService);
      }
      this.socketClient.start();

      this.chatListener = new ChatListener(
            this.translationConfig,
            this.languageStore,
            this.socketClient,
            this.pendingChatStore,
            this.openAiTranslationService,
            this.translationDispatcher,
            this.getLogger(),
            this.discordIntegration
      );
      this.getEventRegistry().registerGlobal(PlayerChatEvent.class, this.chatListener::onChatEvent);

      PlayerConnectListener connectListener = new PlayerConnectListener(this.translationConfig, this.languageStore, this.messageStore, this.ipInfoService, this.getLogger());
      this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, connectListener::onPlayerConnect);

      CommandManager.get().register(new LanguageCommand(this.translationConfig, this.languageStore, this.messageStore));
      CommandManager.get().register(new ReloadCommand(this, this.translationConfig, this.languageStore, this.messageStore));

      if (this.discordIntegration != null) {
         try {
            this.discordIntegration.start(this);
         } catch (NoClassDefFoundError e) {
            ((Api)LOGGER.atWarning().withCause(e)).log("Discord integration disabled (missing dependency).");
            this.discordIntegration = null;
         }
      }
   }

   protected void shutdown() {
      if (this.discordIntegration != null) {
         this.discordIntegration.shutdown();
      }
      if (this.pendingChatStore != null) {
         this.pendingChatStore.shutdown();
      }
      if (this.openAiTranslationService != null) {
         this.openAiTranslationService.shutdown();
      }
   }

   public void reloadTranslation() {
      Path dataDir = this.getDataDirectory();
      TranslationConfig refreshed = TranslationConfig.loadOrCreate(dataDir, this.getLogger());
      this.translationConfig.applyFrom(refreshed);
      this.languageStore.reload();
      if (this.messageStore != null) {
         this.messageStore.reload();
      }
      if (this.socketClient != null) {
         this.socketClient.reconnectNow();
      }
      if (this.discordIntegration != null) {
         try {
            this.discordIntegration.reload(dataDir, this.translationConfig);
         } catch (NoClassDefFoundError e) {
            ((Api)LOGGER.atWarning().withCause(e)).log("Discord integration disabled (missing dependency).");
            this.discordIntegration = null;
         }
      }
   }

   private boolean isDiscordAvailable() {
      try {
         ClassLoader loader = this.getClass().getClassLoader();
         Class.forName("net.dv8tion.jda.api.JDABuilder", false, loader);
         return true;
      } catch (ClassNotFoundException e) {
         return false;
      } catch (NoClassDefFoundError e) {
         return false;
      }
   }
}
