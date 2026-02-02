package com.jogandobem;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AutoLanguageMappings {
   private static final Map<String, String> COUNTRY_TO_LANGUAGE;
   private static final Map<String, String> LANGUAGE_TO_MESSAGE;

   static {
      Map<String, String> country = new HashMap<>();
      Map<String, String> messages = new HashMap<>();

      put(country, messages, "AD", "ca-AD", "El teu idioma s’ha definit automàticament a Català (Andorra) segons la teva ubicació.");
      put(country, messages, "AE", "ar-AE", "تم تعيين لغتك تلقائيًا إلى العربية (الإمارات العربية المتحدة) بناءً على موقعك.");
      put(country, messages, "AF", "fa-AF", "زبان شما بر اساس موقعیت مکانی‌تان به‌طور خودکار روی فارسی (افغانستان) تنظیم شده است.");
      put(country, messages, "AG", "en-AG", "Your language was automatically set to English (Antigua and Barbuda) based on your location.");
      put(country, messages, "AI", "en-AI", "Your language was automatically set to English (Anguilla) based on your location.");
      put(country, messages, "AL", "sq-AL", "Gjuha juaj u caktua automatikisht në Shqip (Shqipëri) bazuar në vendndodhjen tuaj.");
      put(country, messages, "AM", "hy-AM", "Ձեր գտնվելու վայրի հիման վրա ձեր լեզուն ավտոմատ կերպով սահմանվել է Հայերեն (Հայաստան)։");
      put(country, messages, "AO", "pt-AO", "Seu idioma foi definido automaticamente para Português (Angola) com base na sua localização.");
      put(country, messages, "AR", "es-AR", "Tu idioma se configuró automáticamente en Español (Argentina) según tu ubicación.");
      put(country, messages, "AT", "de-AT", "Ihre Sprache wurde basierend auf Ihrem Standort automatisch auf Deutsch (Österreich) eingestellt.");
      put(country, messages, "AU", "en-AU", "Your language was automatically set to English (Australia) based on your location.");
      put(country, messages, "AW", "nl-AW", "Je taal is automatisch ingesteld op Nederlands (Aruba) op basis van je locatie.");
      put(country, messages, "AX", "sv-AX", "Ditt språk har automatiskt ställts in på Svenska (Åland) baserat på din plats.");
      put(country, messages, "BA", "bs-BA", "Vaš jezik je automatski postavljen na Bosanski (Bosna i Hercegovina) na osnovu vaše lokacije.");
      put(country, messages, "BB", "en-BB", "Your language was automatically set to English (Barbados) based on your location.");
      put(country, messages, "BD", "bn-BD", "আপনার অবস্থানের ভিত্তিতে আপনার ভাষা স্বয়ংক্রিয়ভাবে বাংলা (বাংলাদেশ) হিসেবে সেট করা হয়েছে।");
      put(country, messages, "BE", "nl-BE", "Je taal is automatisch ingesteld op Nederlands (België) op basis van je locatie.");
      put(country, messages, "BF", "fr-BF", "Votre langue a été définie automatiquement sur Français (Burkina Faso) en fonction de votre localisation.");
      put(country, messages, "BG", "bg-BG", "Езикът ви беше автоматично зададен на Български (България) въз основа на местоположението ви.");
      put(country, messages, "BH", "ar-BH", "تم تعيين لغتك تلقائيًا إلى العربية (البحرين) بناءً على موقعك.");
      put(country, messages, "BJ", "fr-BJ", "Votre langue a été définie automatiquement sur Français (Bénin) en fonction de votre localisation.");
      put(country, messages, "BL", "fr-BL", "Votre langue a été définie automatiquement sur Français (Saint-Barthélemy) en fonction de votre localisation.");
      put(country, messages, "BM", "en-BM", "Your language was automatically set to English (Bermuda) based on your location.");
      put(country, messages, "BN", "ms-BN", "Bahasa anda telah ditetapkan secara automatik kepada Bahasa Melayu (Brunei) berdasarkan lokasi anda.");
      put(country, messages, "BO", "es-BO", "Tu idioma se configuró automáticamente en Español (Bolivia) según tu ubicación.");
      put(country, messages, "BQ", "nl-BQ", "Je taal is automatisch ingesteld op Nederlands (Caribisch Nederland) op basis van je locatie.");
      put(country, messages, "BR", "pt-BR", "Seu idioma foi definido automaticamente para Português (Brasil) com base na sua localização.");
      put(country, messages, "BS", "en-BS", "Your language was automatically set to English (Bahamas) based on your location.");
      put(country, messages, "BW", "en-BW", "Your language was automatically set to English (Botswana) based on your location.");
      put(country, messages, "BY", "ru-BY", "Ваш язык был автоматически установлен на Русский (Беларусь) на основе вашего местоположения.");
      put(country, messages, "BZ", "en-BZ", "Your language was automatically set to English (Belize) based on your location.");
      put(country, messages, "CA", "en-CA", "Your language was automatically set to English (Canada) based on your location.");
      put(country, messages, "CC", "en-CC", "Your language was automatically set to English (Cocos (Keeling) Islands) based on your location.");
      put(country, messages, "CD", "fr-CD", "Votre langue a été définie automatiquement sur Français (Congo-Kinshasa) en fonction de votre localisation.");
      put(country, messages, "CF", "fr-CF", "Votre langue a été définie automatiquement sur Français (République centrafricaine) en fonction de votre localisation.");
      put(country, messages, "CG", "fr-CG", "Votre langue a été définie automatiquement sur Français (Congo-Brazzaville) en fonction de votre localisation.");
      put(country, messages, "CH", "de-CH", "Ihre Sprache wurde basierend auf Ihrem Standort automatisch auf Deutsch (Schweiz) eingestellt.");
      put(country, messages, "CI", "fr-CI", "Votre langue a été définie automatiquement sur Français (Côte d’Ivoire) en fonction de votre localisation.");
      put(country, messages, "CK", "en-CK", "Your language was automatically set to English (Cook Islands) based on your location.");
      put(country, messages, "CL", "es-CL", "Tu idioma se configuró automáticamente en Español (Chile) según tu ubicación.");
      put(country, messages, "CM", "fr-CM", "Votre langue a été définie automatiquement sur Français (Cameroun) en fonction de votre localisation.");
      put(country, messages, "CN", "zh-CN", "系统已根据您的位置自动将语言设置为中文（中国）。");
      put(country, messages, "CO", "es-CO", "Tu idioma se configuró automáticamente en Español (Colombia) según tu ubicación.");
      put(country, messages, "CP", "fr-CP", "Votre langue a été définie automatiquement sur Français (Île Clipperton) en fonction de votre localisation.");
      put(country, messages, "CR", "es-CR", "Tu idioma se configuró automáticamente en Español (Costa Rica) según tu ubicación.");
      put(country, messages, "CU", "es-CU", "Tu idioma se configuró automáticamente en Español (Cuba) según tu ubicación.");
      put(country, messages, "CV", "pt-CV", "Seu idioma foi definido automaticamente para Português (Cabo Verde) com base na sua localização.");
      put(country, messages, "CW", "nl-CW", "Je taal is automatisch ingesteld op Nederlands (Curaçao) op basis van je locatie.");
      put(country, messages, "CX", "en-CX", "Your language was automatically set to English (Christmas Island) based on your location.");
      put(country, messages, "CY", "el-CY", "Η γλώσσα σας ορίστηκε αυτόματα σε Ελληνικά (Κύπρος) με βάση την τοποθεσία σας.");
      put(country, messages, "CZ", "cs-CZ", "Váš jazyk byl automaticky nastaven na Čeština (Česko) na základě vaší polohy.");
      put(country, messages, "DE", "de-DE", "Ihre Sprache wurde basierend auf Ihrem Standort automatisch auf Deutsch (Deutschland) eingestellt.");
      put(country, messages, "DG", "en-DG", "Your language was automatically set to English (Diego Garcia) based on your location.");
      put(country, messages, "DJ", "fr-DJ", "Votre langue a été définie automatiquement sur Français (Djibouti) en fonction de votre localisation.");
      put(country, messages, "DK", "da-DK", "Dit sprog blev automatisk indstillet til Dansk (Danmark) baseret på din placering.");
      put(country, messages, "DM", "en-DM", "Your language was automatically set to English (Dominica) based on your location.");
      put(country, messages, "DO", "es-DO", "Tu idioma se configuró automáticamente en Español (República Dominicana) según tu ubicación.");
      put(country, messages, "DZ", "ar-DZ", "تم تعيين لغتك تلقائيًا إلى العربية (الجزائر) بناءً على موقعك.");
      put(country, messages, "EA", "es-EA", "Tu idioma se configuró automáticamente en Español (Ceuta y Melilla) según tu ubicación.");
      put(country, messages, "EC", "es-EC", "Tu idioma se configuró automáticamente en Español (Ecuador) según tu ubicación.");
      put(country, messages, "EE", "et-EE", "Teie keel määrati teie asukoha põhjal automaatselt eesti (Eesti) keeleks.");
      put(country, messages, "EG", "ar-EG", "تم تعيين لغتك تلقائيًا إلى العربية (مصر) بناءً على موقعك.");
      put(country, messages, "EH", "ar-EH", "تم تعيين لغتك تلقائيًا إلى العربية (الصحراء الغربية) بناءً على موقعك.");
      put(country, messages, "ES", "es-ES", "Tu idioma se configuró automáticamente en Español (España) según tu ubicación.");
      put(country, messages, "ET", "am-ET", "ቋንቋዎ በአካባቢዎ መሰረት በራሱ ወደ አማርኛ (ኢትዮጵያ) ተቀናብሯል።");
      put(country, messages, "FI", "fi-FI", "Kielesi asetettiin automaattisesti kieleksi Suomi (Suomi) sijaintisi perusteella.");
      put(country, messages, "FJ", "en-FJ", "Your language was automatically set to English (Fiji) based on your location.");
      put(country, messages, "FK", "en-FK", "Your language was automatically set to English (Falkland Islands) based on your location.");
      put(country, messages, "FM", "en-FM", "Your language was automatically set to English (Micronesia) based on your location.");
      put(country, messages, "FR", "fr-FR", "Votre langue a été définie automatiquement sur Français (France) en fonction de votre localisation.");
      put(country, messages, "GA", "fr-GA", "Votre langue a été définie automatiquement sur Français (Gabon) en fonction de votre localisation.");
      put(country, messages, "GB", "en-GB", "Your language was automatically set to English (United Kingdom) based on your location.");
      put(country, messages, "GD", "en-GD", "Your language was automatically set to English (Grenada) based on your location.");
      put(country, messages, "GE", "ka-GE", "თქვენი მდებარეობის მიხედვით თქვენი ენა ავტომატურად დაყენდა ქართული (საქართველო) ენაზე.");
      put(country, messages, "GF", "fr-GF", "Votre langue a été définie automatiquement sur Français (Guyane française) en fonction de votre localisation.");
      put(country, messages, "GG", "en-GG", "Your language was automatically set to English (Guernsey) based on your location.");
      put(country, messages, "GH", "en-GH", "Your language was automatically set to English (Ghana) based on your location.");
      put(country, messages, "GI", "en-GI", "Your language was automatically set to English (Gibraltar) based on your location.");
      put(country, messages, "GM", "en-GM", "Your language was automatically set to English (Gambia) based on your location.");
      put(country, messages, "GN", "fr-GN", "Votre langue a été définie automatiquement sur Français (Guinée) en fonction de votre localisation.");
      put(country, messages, "GP", "fr-GP", "Votre langue a été définie automatiquement sur Français (Guadeloupe) en fonction de votre localisation.");
      put(country, messages, "GQ", "es-GQ", "Tu idioma se configuró automáticamente en Español (Guinea Ecuatorial) según tu ubicación.");
      put(country, messages, "GR", "el-GR", "Η γλώσσα σας ορίστηκε αυτόματα σε Ελληνικά (Ελλάδα) με βάση την τοποθεσία σας.");
      put(country, messages, "GS", "en-GS", "Your language was automatically set to English (South Georgia and the South Sandwich Islands) based on your location.");
      put(country, messages, "GT", "es-GT", "Tu idioma se configuró automáticamente en Español (Guatemala) según tu ubicación.");
      put(country, messages, "GU", "en-GU", "Your language was automatically set to English (Guam) based on your location.");
      put(country, messages, "GW", "pt-GW", "Seu idioma foi definido automaticamente para Português (Guiné-Bissau) com base na sua localização.");
      put(country, messages, "GY", "en-GY", "Your language was automatically set to English (Guyana) based on your location.");
      put(country, messages, "HK", "zh-Hant-HK", "系統已根據您的位置自動將語言設定為中文（香港）。");
      put(country, messages, "HN", "es-HN", "Tu idioma se configuró automáticamente en Español (Honduras) según tu ubicación.");
      put(country, messages, "HR", "hr-HR", "Vaš jezik je automatski postavljen na Hrvatski (Hrvatska) na temelju vaše lokacije.");
      put(country, messages, "HT", "fr-HT", "Votre langue a été définie automatiquement sur Français (Haïti) en fonction de votre localisation.");
      put(country, messages, "HU", "hu-HU", "A nyelvedet a tartózkodási helyed alapján automatikusan Magyar (Magyarország) nyelvre állítottuk.");
      put(country, messages, "IC", "es-IC", "Tu idioma se configuró automáticamente en Español (Islas Canarias) según tu ubicación.");
      put(country, messages, "ID", "id-ID", "Bahasa Anda otomatis disetel ke Bahasa Indonesia (Indonesia) berdasarkan lokasi Anda.");
      put(country, messages, "IE", "en-IE", "Your language was automatically set to English (Ireland) based on your location.");
      put(country, messages, "IM", "en-IM", "Your language was automatically set to English (Isle of Man) based on your location.");
      put(country, messages, "IN", "hi-IN", "आपके स्थान के आधार पर आपकी भाषा स्वतः हिन्दी (भारत) पर सेट कर दी गई है।");
      put(country, messages, "IO", "en-IO", "Your language was automatically set to English (British Indian Ocean Territory) based on your location.");
      put(country, messages, "IQ", "ar-IQ", "تم تعيين لغتك تلقائيًا إلى العربية (العراق) بناءً على موقعك.");
      put(country, messages, "IR", "fa-IR", "زبان شما بر اساس موقعیت مکانی‌تان به‌طور خودکار روی فارسی (ایران) تنظیم شده است.");
      put(country, messages, "IS", "is-IS", "Tungumálið þitt var sjálfkrafa stillt á Íslenska (Ísland) miðað við staðsetningu þína.");
      put(country, messages, "IT", "it-IT", "La tua lingua è stata impostata automaticamente su Italiano (Italia) in base alla tua posizione.");
      put(country, messages, "JE", "en-JE", "Your language was automatically set to English (Jersey) based on your location.");
      put(country, messages, "JM", "en-JM", "Your language was automatically set to English (Jamaica) based on your location.");
      put(country, messages, "JO", "ar-JO", "تم تعيين لغتك تلقائيًا إلى العربية (الأردن) بناءً على موقعك.");
      put(country, messages, "JP", "ja-JP", "お住まいの地域に基づいて、言語が自動的に日本語（日本）に設定されました。");
      put(country, messages, "KE", "sw-KE", "Lugha yako imewekwa kiotomatiki kuwa Kiswahili (Kenya) kulingana na mahali ulipo.");
      put(country, messages, "KI", "en-KI", "Your language was automatically set to English (Kiribati) based on your location.");
      put(country, messages, "KM", "ar-KM", "تم تعيين لغتك تلقائيًا إلى العربية (جزر القمر) بناءً على موقعك.");
      put(country, messages, "KN", "en-KN", "Your language was automatically set to English (Saint Kitts and Nevis) based on your location.");
      put(country, messages, "KP", "ko-KP", "위치 정보를 기반으로 언어가 한국어(조선민주주의인민공화국)로 자동 설정되었습니다.");
      put(country, messages, "KR", "ko-KR", "위치 정보를 기반으로 언어가 한국어(대한민국)로 자동 설정되었습니다.");
      put(country, messages, "KW", "ar-KW", "تم تعيين لغتك تلقائيًا إلى العربية (الكويت) بناءً على موقعك.");
      put(country, messages, "KY", "en-KY", "Your language was automatically set to English (Cayman Islands) based on your location.");
      put(country, messages, "KZ", "kk-KZ", "Орналасқан жеріңізге қарай тіліңіз автоматты түрде Қазақ тілі (Қазақстан) болып орнатылды.");
      put(country, messages, "LB", "ar-LB", "تم تعيين لغتك تلقائيًا إلى العربية (لبنان) بناءً على موقعك.");
      put(country, messages, "LC", "en-LC", "Your language was automatically set to English (Saint Lucia) based on your location.");
      put(country, messages, "LI", "de-LI", "Ihre Sprache wurde basierend auf Ihrem Standort automatisch auf Deutsch (Liechtenstein) eingestellt.");
      put(country, messages, "LR", "en-LR", "Your language was automatically set to English (Liberia) based on your location.");
      put(country, messages, "LT", "lt-LT", "Jūsų kalba buvo automatiškai nustatyta į Lietuvių (Lietuva) pagal jūsų buvimo vietą.");
      put(country, messages, "LU", "fr-LU", "Votre langue a été définie automatiquement sur Français (Luxembourg) en fonction de votre localisation.");
      put(country, messages, "LV", "lv-LV", "Jūsu valoda tika automātiski iestatīta uz Latviešu (Latvija), pamatojoties uz jūsu atrašanās vietu.");
      put(country, messages, "LY", "ar-LY", "تم تعيين لغتك تلقائيًا إلى العربية (ليبيا) بناءً على موقعك.");
      put(country, messages, "MA", "ar-MA", "تم تعيين لغتك تلقائيًا إلى العربية (المغرب) بناءً على موقعك.");
      put(country, messages, "MC", "fr-MC", "Votre langue a été définie automatiquement sur Français (Monaco) en fonction de votre localisation.");
      put(country, messages, "MD", "ro-MD", "Limba dvs. a fost setată automat la Română (Republica Moldova) pe baza locației dvs.");
      put(country, messages, "ME", "sr-Latn-ME", "Vaš jezik je automatski podešen na Srpski (Crna Gora) na osnovu vaše lokacije.");
      put(country, messages, "MF", "fr-MF", "Votre langue a été définie automatiquement sur Français (Saint-Martin) en fonction de votre localisation.");
      put(country, messages, "MH", "en-MH", "Your language was automatically set to English (Marshall Islands) based on your location.");
      put(country, messages, "MK", "mk-MK", "Вашиот јазик беше автоматски поставен на Македонски (Северна Македонија) врз основа на вашата локација.");
      put(country, messages, "ML", "fr-ML", "Votre langue a été définie automatiquement sur Français (Mali) en fonction de votre localisation.");
      put(country, messages, "MM", "my-MM", "သင့်တည်နေရာအပေါ်မူတည်ပြီး သင့်ဘာသာစကားကို မြန်မာ (မြန်မာနိုင်ငံ) သို့ အလိုအလျောက် သတ်မှတ်ထားပါသည်။");
      put(country, messages, "MN", "mn-MN", "Таны байршилд үндэслэн таны хэл автоматаар Монгол (Монгол улс) болж тохируулагдлаа.");
      put(country, messages, "MO", "zh-Hant-MO", "系統已根據您的位置自動將語言設定為中文（澳門）。");
      put(country, messages, "MP", "en-MP", "Your language was automatically set to English (Northern Mariana Islands) based on your location.");
      put(country, messages, "MQ", "fr-MQ", "Votre langue a été définie automatiquement sur Français (Martinique) en fonction de votre localisation.");
      put(country, messages, "MR", "ar-MR", "تم تعيين لغتك تلقائيًا إلى العربية (موريتانيا) بناءً على موقعك.");
      put(country, messages, "MS", "en-MS", "Your language was automatically set to English (Montserrat) based on your location.");
      put(country, messages, "MX", "es-MX", "Tu idioma se configuró automáticamente en Español (México) según tu ubicación.");
      put(country, messages, "MY", "ms-MY", "Bahasa anda telah ditetapkan secara automatik kepada Bahasa Melayu (Malaysia) berdasarkan lokasi anda.");
      put(country, messages, "MZ", "pt-MZ", "Seu idioma foi definido automaticamente para Português (Moçambique) com base na sua localização.");
      put(country, messages, "NC", "fr-NC", "Votre langue a été définie automatiquement sur Français (Nouvelle-Calédonie) en fonction de votre localisation.");
      put(country, messages, "NE", "fr-NE", "Votre langue a été définie automatiquement sur Français (Niger) en fonction de votre localisation.");
      put(country, messages, "NF", "en-NF", "Your language was automatically set to English (Norfolk Island) based on your location.");
      put(country, messages, "NG", "en-NG", "Your language was automatically set to English (Nigeria) based on your location.");
      put(country, messages, "NI", "es-NI", "Tu idioma se configuró automáticamente en Español (Nicaragua) según tu ubicación.");
      put(country, messages, "NL", "nl-NL", "Je taal is automatisch ingesteld op Nederlands (Nederland) op basis van je locatie.");
      put(country, messages, "NO", "nb-NO", "Språket ditt ble automatisk satt til Norsk bokmål (Norge) basert på posisjonen din.");
      put(country, messages, "NR", "en-NR", "Your language was automatically set to English (Nauru) based on your location.");
      put(country, messages, "NU", "en-NU", "Your language was automatically set to English (Niue) based on your location.");
      put(country, messages, "NZ", "en-NZ", "Your language was automatically set to English (New Zealand) based on your location.");
      put(country, messages, "OM", "ar-OM", "تم تعيين لغتك تلقائيًا إلى العربية (عُمان) بناءً على موقعك.");
      put(country, messages, "PA", "es-PA", "Tu idioma se configuró automáticamente en Español (Panamá) según tu ubicación.");
      put(country, messages, "PE", "es-PE", "Tu idioma se configuró automáticamente en Español (Perú) según tu ubicación.");
      put(country, messages, "PF", "fr-PF", "Votre langue a été définie automatiquement sur Français (Polynésie française) en fonction de votre localisation.");
      put(country, messages, "PG", "en-PG", "Your language was automatically set to English (Papua New Guinea) based on your location.");
      put(country, messages, "PH", "fil-PH", "Awtomatikong itinakda ang iyong wika sa Filipino (Pilipinas) batay sa iyong lokasyon.");
      put(country, messages, "PK", "ur-PK", "آپ کے مقام کی بنیاد پر آپ کی زبان خود بخود اردو (پاکستان) پر سیٹ کر دی گئی ہے۔");
      put(country, messages, "PL", "pl-PL", "Twój język został automatycznie ustawiony na Polski (Polska) na podstawie Twojej lokalizacji.");
      put(country, messages, "PM", "fr-PM", "Votre langue a été définie automatiquement sur Français (Saint-Pierre-et-Miquelon) en fonction de votre localisation.");
      put(country, messages, "PN", "en-PN", "Your language was automatically set to English (Pitcairn Islands) based on your location.");
      put(country, messages, "PR", "es-PR", "Tu idioma se configuró automáticamente en Español (Puerto Rico) según tu ubicación.");
      put(country, messages, "PS", "ar-PS", "تم تعيين لغتك تلقائيًا إلى العربية (فلسطين) بناءً على موقعك.");
      put(country, messages, "PT", "pt-PT", "O seu idioma foi definido automaticamente para Português (Portugal) com base na sua localização.");
      put(country, messages, "PW", "en-PW", "Your language was automatically set to English (Palau) based on your location.");
      put(country, messages, "PY", "es-PY", "Tu idioma se configuró automáticamente en Español (Paraguay) según tu ubicación.");
      put(country, messages, "QA", "ar-QA", "تم تعيين لغتك تلقائيًا إلى العربية (قطر) بناءً على موقعك.");
      put(country, messages, "RE", "fr-RE", "Votre langue a été définie automatiquement sur Français (La Réunion) en fonction de votre localisation.");
      put(country, messages, "RO", "ro-RO", "Limba dvs. a fost setată automat la Română (România) pe baza locației dvs.");
      put(country, messages, "RS", "sr-RS", "Ваш језик је аутоматски подешен на Српски (Србија) на основу ваше локације.");
      put(country, messages, "RU", "ru-RU", "Ваш язык был автоматически установлен на Русский (Россия) на основе вашего местоположения.");
      put(country, messages, "SA", "ar-SA", "تم تعيين لغتك تلقائيًا إلى العربية (المملكة العربية السعودية) بناءً على موقعك.");
      put(country, messages, "SB", "en-SB", "Your language was automatically set to English (Solomon Islands) based on your location.");
      put(country, messages, "SC", "fr-SC", "Votre langue a été définie automatiquement sur Français (Seychelles) en fonction de votre localisation.");
      put(country, messages, "SD", "ar-SD", "تم تعيين لغتك تلقائيًا إلى العربية (السودان) بناءً على موقعك.");
      put(country, messages, "SE", "sv-SE", "Ditt språk har automatiskt ställts in på Svenska (Sverige) baserat på din plats.");
      put(country, messages, "SG", "en-SG", "Your language was automatically set to English (Singapore) based on your location.");
      put(country, messages, "SH", "en-SH", "Your language was automatically set to English (Saint Helena) based on your location.");
      put(country, messages, "SI", "sl-SI", "Vaš jezik je bil samodejno nastavljen na Slovenščina (Slovenija) glede na vašo lokacijo.");
      put(country, messages, "SJ", "nb-SJ", "Språket ditt ble automatisk satt til Norsk bokmål (Svalbard og Jan Mayen) basert på posisjonen din.");
      put(country, messages, "SK", "sk-SK", "Váš jazyk bol automaticky nastavený na Slovenčina (Slovensko) na základe vašej polohy.");
      put(country, messages, "SL", "en-SL", "Your language was automatically set to English (Sierra Leone) based on your location.");
      put(country, messages, "SM", "it-SM", "La tua lingua è stata impostata automaticamente su Italiano (San Marino) in base alla tua posizione.");
      put(country, messages, "SN", "fr-SN", "Votre langue a été définie automatiquement sur Français (Sénégal) en fonction de votre localisation.");
      put(country, messages, "SO", "so-SO", "Luqaddaada si toos ah ayaa loogu dejiyay Soomaali (Soomaaliya) iyadoo lagu salaynayo goobtaada.");
      put(country, messages, "SR", "nl-SR", "Je taal is automatisch ingesteld op Nederlands (Suriname) op basis van je locatie.");
      put(country, messages, "SS", "en-SS", "Your language was automatically set to English (South Sudan) based on your location.");
      put(country, messages, "ST", "pt-ST", "Seu idioma foi definido automaticamente para Português (São Tomé e Príncipe) com base na sua localização.");
      put(country, messages, "SV", "es-SV", "Tu idioma se configuró automáticamente en Español (El Salvador) según tu ubicación.");
      put(country, messages, "SX", "nl-SX", "Je taal is automatisch ingesteld op Nederlands (Sint Maarten) op basis van je locatie.");
      put(country, messages, "SY", "ar-SY", "تم تعيين لغتك تلقائيًا إلى العربية (سوريا) بناءً على موقعك.");
      put(country, messages, "SZ", "en-SZ", "Your language was automatically set to English (Eswatini) based on your location.");
      put(country, messages, "TA", "en-TA", "Your language was automatically set to English (Tristan da Cunha) based on your location.");
      put(country, messages, "TC", "en-TC", "Your language was automatically set to English (Turks and Caicos Islands) based on your location.");
      put(country, messages, "TD", "fr-TD", "Votre langue a été définie automatiquement sur Français (Tchad) en fonction de votre localisation.");
      put(country, messages, "TF", "fr-TF", "Votre langue a été définie automatiquement sur Français (Terres australes françaises) en fonction de votre localisation.");
      put(country, messages, "TG", "fr-TG", "Votre langue a été définie automatiquement sur Français (Togo) en fonction de votre localisation.");
      put(country, messages, "TH", "th-TH", "ระบบได้ตั้งค่าภาษาของคุณเป็น ภาษาไทย (ประเทศไทย) โดยอัตโนมัติตามตำแหน่งของคุณ");
      put(country, messages, "TK", "en-TK", "Your language was automatically set to English (Tokelau) based on your location.");
      put(country, messages, "TL", "pt-TL", "Seu idioma foi definido automaticamente para Português (Timor-Leste) com base na sua localização.");
      put(country, messages, "TN", "ar-TN", "تم تعيين لغتك تلقائيًا إلى العربية (تونس) بناءً على موقعك.");
      put(country, messages, "TR", "tr-TR", "Konumunuza göre diliniz otomatik olarak Türkçe (Türkiye) olarak ayarlandı.");
      put(country, messages, "TT", "en-TT", "Your language was automatically set to English (Trinidad and Tobago) based on your location.");
      put(country, messages, "TV", "en-TV", "Your language was automatically set to English (Tuvalu) based on your location.");
      put(country, messages, "TW", "zh-Hant-TW", "系統已根據您的位置自動將語言設定為中文（台灣）。");
      put(country, messages, "TZ", "sw-TZ", "Lugha yako imewekwa kiotomatiki kuwa Kiswahili (Tanzania) kulingana na mahali ulipo.");
      put(country, messages, "UA", "uk-UA", "Вашу мову автоматично встановлено на Українську (Україна) на основі вашого місцезнаходження.");
      put(country, messages, "UG", "en-UG", "Your language was automatically set to English (Uganda) based on your location.");
      put(country, messages, "UM", "en-UM", "Your language was automatically set to English (U.S. Outlying Islands) based on your location.");
      put(country, messages, "US", "en-US", "Your language was automatically set to English (United States) based on your location.");
      put(country, messages, "UY", "es-UY", "Tu idioma se configuró automáticamente en Español (Uruguay) según tu ubicación.");
      put(country, messages, "VA", "it-VA", "La tua lingua è stata impostata automaticamente su Italiano (Città del Vaticano) in base alla tua posizione.");
      put(country, messages, "VC", "en-VC", "Your language was automatically set to English (Saint Vincent and the Grenadines) based on your location.");
      put(country, messages, "VE", "es-VE", "Tu idioma se configuró automáticamente en Español (Venezuela) según tu ubicación.");
      put(country, messages, "VG", "en-VG", "Your language was automatically set to English (British Virgin Islands) based on your location.");
      put(country, messages, "VI", "en-VI", "Your language was automatically set to English (U.S. Virgin Islands) based on your location.");
      put(country, messages, "VN", "vi-VN", "Ngôn ngữ của bạn đã được tự động đặt thành Tiếng Việt (Việt Nam) dựa trên vị trí của bạn.");
      put(country, messages, "WF", "fr-WF", "Votre langue a été définie automatiquement sur Français (Wallis-et-Futuna) en fonction de votre localisation.");
      put(country, messages, "XK", "sq-XK", "Gjuha juaj u caktua automatikisht në Shqip (Kosovë) bazuar në vendndodhjen tuaj.");
      put(country, messages, "YE", "ar-YE", "تم تعيين لغتك تلقائيًا إلى العربية (اليمن) بناءً على موقعك.");
      put(country, messages, "YT", "fr-YT", "Votre langue a été définie automatiquement sur Français (Mayotte) en fonction de votre localisation.");
      put(country, messages, "ZA", "en-ZA", "Your language was automatically set to English (South Africa) based on your location.");
      put(country, messages, "ZM", "en-ZM", "Your language was automatically set to English (Zambia) based on your location.");

      COUNTRY_TO_LANGUAGE = Collections.unmodifiableMap(country);
      LANGUAGE_TO_MESSAGE = Collections.unmodifiableMap(messages);
   }

   private AutoLanguageMappings() {
   }

   public static String getLanguageForCountry(String countryCode) {
      if (countryCode == null) {
         return null;
      }
      return COUNTRY_TO_LANGUAGE.get(countryCode.trim().toUpperCase(Locale.ROOT));
   }

   public static String getMessageForLanguage(String languageCode) {
      if (languageCode == null) {
         return null;
      }
      String key = languageCode.trim();
      String message = LANGUAGE_TO_MESSAGE.get(key);
      if (message != null) {
         return message;
      }
      return LANGUAGE_TO_MESSAGE.get(key.toLowerCase(Locale.ROOT));
   }

   private static void put(Map<String, String> country, Map<String, String> messages, String code, String lang, String message) {
      if (code == null || lang == null || message == null) {
         return;
      }
      country.put(code, lang);
      messages.put(lang, message);
      messages.put(lang.toLowerCase(Locale.ROOT), message);
   }
}
