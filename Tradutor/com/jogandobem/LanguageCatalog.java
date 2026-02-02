package com.jogandobem;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LanguageCatalog {
   private LanguageCatalog() {
   }

   public static final String[] LANGUAGE_LINES = new String[] {
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

   public static final Map<String, String> ALLOWED_CODES = buildAllowedCodes();

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
}
