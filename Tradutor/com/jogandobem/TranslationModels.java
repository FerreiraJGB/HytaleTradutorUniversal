package com.jogandobem;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public final class TranslationModels {
   private TranslationModels() {
   }

   public static final class TranslationRequest {
      @SerializedName("api_key")
      public String apiKey;

      @SerializedName("texto_original")
      public String textoOriginal;

      @SerializedName("idioma_original")
      public String idiomaOriginal;

      @SerializedName("jogador")
      public String jogador;

      @SerializedName("jogadores_online")
      public List<TranslationTarget> jogadoresOnline;
   }

   public static final class TranslationTarget {
      @SerializedName("jogador")
      public String jogador;

      @SerializedName("idioma")
      public String idioma;
   }

   public static final class TranslationResponse {
      @SerializedName("jogador")
      public String jogador;

      @SerializedName("jogador_uuid")
      public String jogadorUuid;

      @SerializedName("traducao")
      public List<TranslationResult> traducao;
   }

   public static final class TranslationResult {
      @SerializedName("jogador")
      public String jogador;

      @SerializedName("texto_traduzido")
      public String textoTraduzido;
   }
}
