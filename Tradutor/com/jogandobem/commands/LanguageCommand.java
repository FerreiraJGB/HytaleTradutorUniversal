package com.jogandobem.commands;

import com.jogandobem.LanguageStore;
import com.jogandobem.TranslationConfig;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LanguageCommand extends AbstractAsyncCommand {
   private final TranslationConfig config;
   private final LanguageStore languageStore;

   public LanguageCommand(TranslationConfig config, LanguageStore languageStore) {
      super("l", "Define o idioma do tradutor");
      this.config = config;
      this.languageStore = languageStore;
      this.setAllowsExtraArguments(true);
   }

   protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
      if (ctx == null) {
         return CompletableFuture.completedFuture((Void) null);
      }
      if (!ctx.isPlayer()) {
         ctx.sendMessage(Message.raw("Este comando so pode ser usado por jogadores."));
         return CompletableFuture.completedFuture((Void) null);
      }

      String input = ctx.getInputString();
      String[] parts = input == null ? new String[0] : input.trim().split("\\s+");
      UUID uuid = ctx.sender().getUuid();
      PlayerRef player = Universe.get().getPlayer(uuid);
      String username = player != null ? player.getUsername() : ctx.sender().getDisplayName();

      if (parts.length <= 1) {
         String current = this.languageStore.getLanguage(uuid);
         String display = current == null || current.isEmpty() ? "(padrao: " + this.config.defaultLanguage + ")" : current;
         ctx.sendMessage(Message.raw("Idioma atual: " + display));
         ctx.sendMessage(Message.raw("Uso: /l <codigo_idioma>"));
         return CompletableFuture.completedFuture((Void) null);
      }

      String code = parts[1].trim();
      if (code.isEmpty()) {
         ctx.sendMessage(Message.raw("Uso: /l <codigo_idioma>"));
         return CompletableFuture.completedFuture((Void) null);
      }

      if (code.equalsIgnoreCase("auto") || code.equalsIgnoreCase("default") || code.equalsIgnoreCase("padrao")) {
         this.languageStore.clearLanguage(uuid);
         ctx.sendMessage(Message.raw("Idioma resetado para o padrao."));
         return CompletableFuture.completedFuture((Void) null);
      }

      this.languageStore.setLanguage(uuid, username, code);
      ctx.sendMessage(Message.raw("Idioma definido para: " + code));
      return CompletableFuture.completedFuture((Void) null);
   }
}
