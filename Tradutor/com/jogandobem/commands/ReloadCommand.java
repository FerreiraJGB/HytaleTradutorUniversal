package com.jogandobem.commands;

import com.jogandobem.TradutorUniversal;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import java.util.concurrent.CompletableFuture;

public final class ReloadCommand extends AbstractAsyncCommand {
   private final TradutorUniversal plugin;

   public ReloadCommand(TradutorUniversal plugin) {
      super("treload", "Recarrega configuracoes do tradutor");
      this.plugin = plugin;
   }

   protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
      if (this.plugin != null) {
         this.plugin.reloadTranslation();
      }
      if (ctx != null) {
         ctx.sendMessage(Message.raw("Tradutor recarregado."));
      }
      return CompletableFuture.completedFuture((Void) null);
   }
}
