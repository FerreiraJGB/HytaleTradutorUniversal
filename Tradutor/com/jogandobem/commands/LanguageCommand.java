package com.jogandobem.commands;

import com.jogandobem.LanguageCatalog;
import com.jogandobem.LanguageStore;
import com.jogandobem.MessageStore;
import com.jogandobem.TranslationConfig;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LanguageCommand extends AbstractAsyncCommand {
   private final TranslationConfig config;
   private final LanguageStore languageStore;
   private final MessageStore messageStore;

   public LanguageCommand(TranslationConfig config, LanguageStore languageStore, MessageStore messageStore) {
      super("l", "Define o idioma do tradutor");
      this.config = config;
      this.languageStore = languageStore;
      this.messageStore = messageStore;
      this.setAllowsExtraArguments(true);
   }

   protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
      if (ctx == null) {
         return CompletableFuture.completedFuture((Void) null);
      }
      String messageLang = resolveMessageLanguage(ctx);
      if (!ctx.isPlayer()) {
         ctx.sendMessage(Message.raw(this.messageStore.getString("cmd_only_players", messageLang, this.config)));
         return CompletableFuture.completedFuture((Void) null);
      }

      String input = ctx.getInputString();
      String[] parts = input == null ? new String[0] : input.trim().split("\\s+");
      UUID uuid = ctx.sender().getUuid();
      PlayerRef player = Universe.get().getPlayer(uuid);
      String username = player != null ? player.getUsername() : ctx.sender().getDisplayName();

      if (parts.length <= 1) {
         sendHelp(ctx, uuid, null, messageLang);
         return CompletableFuture.completedFuture((Void) null);
      }

      String code = parts[1].trim();
      if (code.isEmpty()) {
         ctx.sendMessage(Message.raw(this.messageStore.getString("cmd_usage_language", messageLang, this.config)));
         return CompletableFuture.completedFuture((Void) null);
      }

      code = normalizeCodeInput(code);
      if (isHelpKeyword(code)) {
         String filter = parts.length > 2 ? parts[2] : null;
         sendHelp(ctx, uuid, filter, messageLang);
         return CompletableFuture.completedFuture((Void) null);
      }

      if (code.equalsIgnoreCase("auto") || code.equalsIgnoreCase("default") || code.equalsIgnoreCase("padrao")) {
         this.languageStore.clearLanguage(uuid);
         ctx.sendMessage(Message.raw(this.messageStore.getString("cmd_language_reset", messageLang, this.config)));
         return CompletableFuture.completedFuture((Void) null);
      }

      String canonical = getCanonicalCode(code);
      if (canonical == null) {
         ctx.sendMessage(Message.raw(this.messageStore.getString("cmd_language_invalid", messageLang, this.config)));
         ctx.sendMessage(Message.raw(this.messageStore.getString("cmd_language_invalid_hint", messageLang, this.config)));
         return CompletableFuture.completedFuture((Void) null);
      }

      this.languageStore.setLanguage(uuid, username, canonical);
      String confirmLang = this.messageStore.resolveLanguage(canonical, this.config);
      ctx.sendMessage(Message.raw(this.messageStore.format("cmd_language_set", confirmLang, this.config, Map.of("lang", canonical))));
      return CompletableFuture.completedFuture((Void) null);
   }

   private void sendHelp(CommandContext ctx, UUID uuid, String filter, String messageLang) {
      if (ctx == null) {
         return;
      }
      String current = this.languageStore.getLanguage(uuid);
      String display = current == null || current.isEmpty()
            ? this.messageStore.format("cmd_language_default", messageLang, this.config, Map.of("lang", this.config.defaultLanguage))
            : current;
      ctx.sendMessage(Message.raw(this.messageStore.format("cmd_language_current", messageLang, this.config, Map.of("current", display))));
      for (String line : this.messageStore.getLines("cmd_help_intro", messageLang, this.config)) {
         ctx.sendMessage(Message.raw(line));
      }
      if (filter == null || filter.isBlank()) {
         return;
      }
      String normalized = filter.trim().toLowerCase();
      ctx.sendMessage(Message.raw(this.messageStore.format("cmd_language_filter_title", messageLang, this.config, Map.of("filter", filter))));
      int matches = 0;
      for (String line : this.messageStore.getLines("language_lines", messageLang, this.config)) {
         if (line == null || line.isBlank()) {
            continue;
         }
         if (matchesFilter(line, normalized)) {
            ctx.sendMessage(Message.raw(line));
            matches++;
         }
      }
      if (matches == 0) {
         ctx.sendMessage(Message.raw(this.messageStore.getString("cmd_language_filter_none", messageLang, this.config)));
      }
   }

   private static boolean isHelpKeyword(String value) {
      return value.equalsIgnoreCase("help") || value.equalsIgnoreCase("ajuda") || value.equals("?") || value.equalsIgnoreCase("idiomas");
   }

   private static String normalizeCodeInput(String value) {
      if (value == null) {
         return "";
      }
      String trimmed = value.trim();
      if (trimmed.startsWith("[") && trimmed.endsWith("]") && trimmed.length() > 2) {
         trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
      }
      return trimmed;
   }

   private static String getCanonicalCode(String value) {
      if (value == null) {
         return null;
      }
      String key = value.trim().toLowerCase(Locale.ROOT);
      if (key.isEmpty()) {
         return null;
      }
      return LanguageCatalog.ALLOWED_CODES.get(key);
   }

   private static boolean matchesFilter(String line, String filter) {
      int colon = line.indexOf(':');
      if (colon > 0) {
         String name = line.substring(0, colon).trim();
         if (!name.isEmpty() && name.toLowerCase().startsWith(filter)) {
            return true;
         }
      }
      String lower = line.toLowerCase();
      int idx = 0;
      while (true) {
         int start = lower.indexOf('[', idx);
         if (start < 0) {
            break;
         }
         int end = lower.indexOf(']', start + 1);
         if (end < 0) {
            break;
         }
         String code = lower.substring(start + 1, end).trim();
         if (!code.isEmpty() && code.contains(filter)) {
            return true;
         }
         idx = end + 1;
      }
      return false;
   }

   private String resolveMessageLanguage(CommandContext ctx) {
      if (ctx == null) {
         return this.messageStore.resolveLanguage(null, this.config);
      }
      if (!ctx.isPlayer()) {
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
