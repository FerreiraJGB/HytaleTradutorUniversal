package com.jogandobem.commands;

import com.jogandobem.LanguageStore;
import com.jogandobem.MessageStore;
import com.jogandobem.TradutorUniversal;
import com.jogandobem.TranslationConfig;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ReloadCommand extends AbstractAsyncCommand {
   private final TradutorUniversal plugin;
   private final TranslationConfig config;
   private final LanguageStore languageStore;
   private final MessageStore messageStore;

   public ReloadCommand(TradutorUniversal plugin, TranslationConfig config, LanguageStore languageStore, MessageStore messageStore) {
      super("treload", "Recarrega configuracoes do tradutor");
      this.plugin = plugin;
      this.config = config;
      this.languageStore = languageStore;
      this.messageStore = messageStore;
   }

   protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
      if (this.plugin != null) {
         this.plugin.reloadTranslation();
      }
      if (ctx != null) {
         String lang = resolveMessageLanguage(ctx);
         ctx.sendMessage(Message.raw(this.messageStore.getString("cmd_reload_done", lang, this.config)));
      }
      return CompletableFuture.completedFuture((Void) null);
   }

   private String resolveMessageLanguage(CommandContext ctx) {
      if (ctx == null || !ctx.isPlayer()) {
         return this.messageStore.resolveLanguage(null, this.config);
      }
      UUID uuid = ctx.sender().getUuid();
      String lang = this.languageStore.getLanguage(uuid);
      if (lang == null || lang.isBlank()) {
         PlayerRef player = Universe.get().getPlayer(uuid);
         lang = player != null ? player.getLanguage() : null;
      }
      if (lang == null || lang.isBlank()) {
         lang = this.config.defaultLanguage;
      }
      return this.messageStore.resolveLanguage(lang, this.config);
   }
}
