package com.jogandobem.commands;

import com.jogandobem.LanguageStore;
import com.jogandobem.TranslationConfig;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class LanguageCommand extends AbstractAsyncCommand {
   private final TranslationConfig config;
   private final LanguageStore languageStore;
   private static final String[] HELP_INTRO = new String[] {
         "Como funciona: o chat e traduzido para o idioma escolhido por voce.",
         "Use /l <codigo_idioma> para definir o idioma ou /l auto para usar o padrao do servidor.",
         "Exemplo: /l pt-BR",
         "Para listar idiomas: /l help <letra> (ex.: /l help p)"
   };
   private static final String[] LANGUAGE_LINES = new String[] {
         "Português: [pt-BR], [pt-PT], [pt-AO], [pt-MZ]",
         "English: [en-US], [en-GB], [en-CA], [en-AU], [en-IN], [en-NZ], [en-IE], [en-ZA]",
         "Español: [es-ES], [es-MX], [es-AR], [es-CO], [es-CL], [es-PE], [es-US]",
         "Français: [fr-FR], [fr-CA], [fr-BE], [fr-CH], [fr-LU]",
         "Deutsch: [de-DE], [de-AT], [de-CH]",
         "Italiano: [it-IT], [it-CH]",
         "Nederlands: [nl-NL], [nl-BE]",
         "Svenska: [sv-SE]",
         "Norsk: [nb-NO], [nn-NO]",
         "Dansk: [da-DK]",
         "Suomi: [fi-FI]",
         "Íslenska: [is-IS]",
         "Gaeilge: [ga-IE]",
         "Cymraeg: [cy-GB]",
         "Polski: [pl-PL]",
         "Čeština: [cs-CZ]",
         "Slovenčina: [sk-SK]",
         "Magyar: [hu-HU]",
         "Română: [ro-RO], [ro-MD]",
         "Български: [bg-BG]",
         "Ελληνικά: [el-GR], [el-CY]",
         "Русский: [ru-RU]",
         "Українська: [uk-UA]",
         "Српски: [sr-RS], [sr-Cyrl-RS], [sr-Latn-RS]",
         "Hrvatski: [hr-HR]",
         "Bosanski: [bs-BA]",
         "Slovenščina: [sl-SI]",
         "Shqip: [sq-AL], [sq-XK]",
         "Македонски: [mk-MK]",
         "Lietuvių: [lt-LT]",
         "Latvian: [lv-LV]",
         "Eesti: [et-EE]",
         "Türkçe: [tr-TR]",
         "العربية: [ar], [ar-SA], [ar-EG], [ar-MA], [ar-AE], [ar-DZ]",
         "עברית: [he-IL]",
         "فارسی: [fa-IR], [fa-AF]",
         "کوردی: [ku-TR], [ckb-IQ]",
         "हिन्दी: [hi-IN]",
         "اردو: [ur-PK], [ur-IN]",
         "বাংলা: [bn-BD], [bn-IN]",
         "ਪੰਜਾਬੀ: [pa-IN], [pa-PK]",
         "தமிழ்: [ta-IN], [ta-LK], [ta-SG], [ta-MY]",
         "తెలుగు: [te-IN]",
         "मराठी: [mr-IN]",
         "ગુજરાતી: [gu-IN]",
         "ಕನ್ನಡ: [kn-IN]",
         "മലയാളം: [ml-IN]",
         "සිංහල: [si-LK]",
         "नेपाली: [ne-NP]",
         "中文: [zh-CN], [zh-TW], [zh-HK], [zh-SG], [zh-Hans], [zh-Hant], [zh-Hans-CN], [zh-Hant-TW]",
         "粵語: [yue-HK], [yue-Hant-HK]",
         "日本語: [ja-JP]",
         "한국어: [ko-KR]",
         "Tiếng Việt: [vi-VN]",
         "ไทย: [th-TH]",
         "Bahasa Indonesia: [id-ID]",
         "Bahasa Melayu: [ms-MY], [ms-BN], [ms-SG]",
         "Filipino/Tagalog: [fil-PH], [tl-PH]",
         "Kiswahili: [sw-KE], [sw-TZ]",
         "Afrikaans: [af-ZA]",
         "isiZulu: [zu-ZA]",
         "isiXhosa: [xh-ZA]",
         "አማርኛ: [am-ET]",
         "Soomaali: [so-SO], [so-KE]",
         "Yorùbá: [yo-NG]",
         "Igbo: [ig-NG]",
         "Hausa: [ha-NG]",
         "Shona: [sn-ZW]",
         "Latīna: [la-VA]",
         "Esperanto: [eo]"
   };
   private static final Map<String, String> ALLOWED_CODES = buildAllowedCodes();

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
         sendHelp(ctx, uuid, null);
         return CompletableFuture.completedFuture((Void) null);
      }

      String code = parts[1].trim();
      if (code.isEmpty()) {
         ctx.sendMessage(Message.raw("Uso: /l <codigo_idioma>"));
         return CompletableFuture.completedFuture((Void) null);
      }

      code = normalizeCodeInput(code);
      if (isHelpKeyword(code)) {
         String filter = parts.length > 2 ? parts[2] : null;
         sendHelp(ctx, uuid, filter);
         return CompletableFuture.completedFuture((Void) null);
      }

      if (code.equalsIgnoreCase("auto") || code.equalsIgnoreCase("default") || code.equalsIgnoreCase("padrao")) {
         this.languageStore.clearLanguage(uuid);
         ctx.sendMessage(Message.raw("Idioma resetado para o padrao."));
         return CompletableFuture.completedFuture((Void) null);
      }

      String canonical = getCanonicalCode(code);
      if (canonical == null) {
         ctx.sendMessage(Message.raw("Codigo de idioma invalido. Exemplos: en-US, pt-BR, zh-CN, zh-Hans-CN."));
         ctx.sendMessage(Message.raw("Use /l help <letra> para ver a lista de idiomas."));
         return CompletableFuture.completedFuture((Void) null);
      }

      this.languageStore.setLanguage(uuid, username, canonical);
      ctx.sendMessage(Message.raw("Idioma definido para: " + canonical));
      return CompletableFuture.completedFuture((Void) null);
   }

   private void sendHelp(CommandContext ctx, UUID uuid, String filter) {
      if (ctx == null) {
         return;
      }
      String current = this.languageStore.getLanguage(uuid);
      String display = current == null || current.isEmpty() ? "(padrao: " + this.config.defaultLanguage + ")" : current;
      ctx.sendMessage(Message.raw("Idioma atual: " + display));
      for (String line : HELP_INTRO) {
         ctx.sendMessage(Message.raw(line));
      }
      if (filter == null || filter.isBlank()) {
         return;
      }
      String normalized = filter.trim().toLowerCase();
      ctx.sendMessage(Message.raw("Idiomas disponiveis com filtro \"" + filter + "\":"));
      int matches = 0;
      for (String line : LANGUAGE_LINES) {
         if (line == null || line.isBlank()) {
            continue;
         }
         if (matchesFilter(line, normalized)) {
            ctx.sendMessage(Message.raw(line));
            matches++;
         }
      }
      if (matches == 0) {
         ctx.sendMessage(Message.raw("Nenhum idioma encontrado para esse filtro."));
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
      return ALLOWED_CODES.get(key);
   }

   private static Map<String, String> buildAllowedCodes() {
      Map<String, String> map = new HashMap<>();
      for (String line : LANGUAGE_LINES) {
         if (line == null || line.isBlank()) {
            continue;
         }
         int idx = 0;
         while (true) {
            int start = line.indexOf('[', idx);
            if (start < 0) {
               break;
            }
            int end = line.indexOf(']', start + 1);
            if (end < 0) {
               break;
            }
            String code = line.substring(start + 1, end).trim();
            if (!code.isEmpty()) {
               map.put(code.toLowerCase(Locale.ROOT), code);
            }
            idx = end + 1;
         }
      }
      return map;
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
}
