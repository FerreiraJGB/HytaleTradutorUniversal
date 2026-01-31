package com.jogandobem;

import com.jogandobem.commands.LanguageCommand;
import com.jogandobem.commands.ReloadCommand;
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
   private ChatListener chatListener;

   public TradutorUniversal(@Nonnull JavaPluginInit init) {
      super(init);
   }

   protected void setup() {
      ((Api)LOGGER.atInfo()).log("Setting up plugin " + this.getName());
      Path dataDir = this.getDataDirectory();
      this.translationConfig = TranslationConfig.loadOrCreate(dataDir, this.getLogger());
      this.languageStore = LanguageStore.loadOrCreate(dataDir, this.getLogger());
      this.pendingChatStore = new PendingChatStore(this.translationConfig.pendingTtlSeconds);
      this.translationDispatcher = new TranslationDispatcher(this.pendingChatStore, this.getLogger());
      this.socketClient = new TranslationSocketClient(this.translationConfig, this.getLogger(), this.translationDispatcher);
      this.socketClient.start();

      this.chatListener = new ChatListener(this.translationConfig, this.languageStore, this.socketClient, this.pendingChatStore, this.getLogger());
      this.getEventRegistry().registerGlobal(PlayerChatEvent.class, this.chatListener::onChatEvent);

      PlayerConnectListener connectListener = new PlayerConnectListener(this.translationConfig, this.languageStore, this.getLogger());
      this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, connectListener::onPlayerConnect);

      CommandManager.get().register(new LanguageCommand(this.translationConfig, this.languageStore));
      CommandManager.get().register(new ReloadCommand(this));
   }

   public void reloadTranslation() {
      Path dataDir = this.getDataDirectory();
      TranslationConfig refreshed = TranslationConfig.loadOrCreate(dataDir, this.getLogger());
      this.translationConfig.applyFrom(refreshed);
      this.languageStore.reload();
      if (this.socketClient != null) {
         this.socketClient.reconnectNow();
      }
   }
}
