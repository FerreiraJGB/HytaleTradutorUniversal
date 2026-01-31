package com.jogandobem;

import com.jogandobem.TranslationModels.TranslationTarget;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public final class SocketModels {
   private SocketModels() {
   }

   public static final class ChatPayload {
      @SerializedName("type")
      public String type;

      @SerializedName("server_id")
      public String serverId;

      @SerializedName("message_id")
      public String messageId;

      @SerializedName("texto_original")
      public String textoOriginal;

      @SerializedName("idioma_original")
      public String idiomaOriginal;

      @SerializedName("jogador")
      public String jogador;

      @SerializedName("jogador_uuid")
      public String jogadorUuid;

      @SerializedName("jogadores_online")
      public List<TranslationTarget> jogadoresOnline;
   }
}
