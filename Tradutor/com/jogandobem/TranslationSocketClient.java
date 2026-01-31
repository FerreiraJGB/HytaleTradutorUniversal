package com.jogandobem;

import com.jogandobem.TranslationModels.TranslationResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TranslationSocketClient implements WebSocket.Listener {
   private final TranslationConfig config;
   private final HytaleLogger logger;
   private final Gson gson = new Gson();
   private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
   private final AtomicBoolean connecting = new AtomicBoolean(false);
   private final Queue<String> outboundQueue = new ConcurrentLinkedQueue<>();
   private final TranslationDispatcher dispatcher;
   private volatile WebSocket webSocket;
   private volatile boolean authenticated;
   private final StringBuilder inboundBuffer = new StringBuilder();

   public TranslationSocketClient(TranslationConfig config, HytaleLogger logger, TranslationDispatcher dispatcher) {
      this.config = config;
      this.logger = logger;
      this.dispatcher = dispatcher;
   }

   public void start() {
      connect();
   }

   public void stop() {
      WebSocket ws = this.webSocket;
      if (ws != null) {
         try {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown");
         } catch (Exception e) {
         }
      }
      this.scheduler.shutdownNow();
   }

   public void sendChat(String jsonPayload) {
      if (jsonPayload == null || jsonPayload.isBlank()) {
         return;
      }
      WebSocket ws = this.webSocket;
      if (ws == null || !this.authenticated) {
         enqueue(jsonPayload);
         if (ws == null) {
            connect();
         }
         return;
      }
      ws.sendText(jsonPayload, true);
   }

   public void reconnectNow() {
      connect();
   }

   private void enqueue(String jsonPayload) {
      if (this.outboundQueue.size() > 500) {
         this.outboundQueue.poll();
      }
      this.outboundQueue.add(jsonPayload);
   }

   private void connect() {
      if (!this.config.isApiConfigured()) {
         return;
      }
      if (this.connecting.compareAndSet(false, true)) {
         try {
            String wsUrl = this.config.wsUrl;
            if (wsUrl == null || wsUrl.isBlank()) {
               this.connecting.set(false);
               return;
            }
            HttpClient client = HttpClient.newBuilder()
                  .connectTimeout(Duration.ofSeconds(5))
                  .build();
            client.newWebSocketBuilder()
                  .connectTimeout(Duration.ofSeconds(5))
                  .buildAsync(URI.create(wsUrl), this)
                  .whenComplete((ws, err) -> {
                     this.connecting.set(false);
                     if (err != null) {
                        scheduleReconnect();
                        ((Api) this.logger.atWarning().withCause(err)).log("ChatTranslation WS connect failed");
                     } else {
                        this.webSocket = ws;
                        this.authenticated = false;
                        sendHello();
                     }
                  });
         } catch (Exception e) {
            this.connecting.set(false);
            scheduleReconnect();
            ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation WS connect error");
         }
      }
   }

   private void sendHello() {
      JsonObject obj = new JsonObject();
      obj.addProperty("type", "hello");
      obj.addProperty("server_id", this.config.serverId == null ? "" : this.config.serverId);
      obj.addProperty("server_secret", this.config.serverSecret == null ? "" : this.config.serverSecret);
      obj.addProperty("plugin", "TradutorUniversal");
      obj.addProperty("version", "1.0.1");
      sendRaw(obj.toString());
   }

   private void sendRaw(String payload) {
      WebSocket ws = this.webSocket;
      if (ws != null) {
         ws.sendText(payload, true);
      }
   }

   private void flushQueue() {
      WebSocket ws = this.webSocket;
      if (ws == null || !this.authenticated) {
         return;
      }
      String msg;
      while ((msg = this.outboundQueue.poll()) != null) {
         ws.sendText(msg, true);
      }
   }

   private void scheduleReconnect() {
      int delay = Math.max(1, this.config.wsReconnectSeconds);
      this.scheduler.schedule(this::connect, delay, TimeUnit.SECONDS);
   }

   @Override
   public void onOpen(WebSocket webSocket) {
      this.webSocket = webSocket;
      this.authenticated = false;
      sendHello();
      webSocket.request(1);
   }

   @Override
   public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
      this.inboundBuffer.append(data);
      if (last) {
         String payload = this.inboundBuffer.toString();
         this.inboundBuffer.setLength(0);
         handleMessage(payload);
      }
      webSocket.request(1);
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
      webSocket.request(1);
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
      this.webSocket = null;
      this.authenticated = false;
      scheduleReconnect();
      return CompletableFuture.completedFuture(null);
   }

   @Override
   public void onError(WebSocket webSocket, Throwable error) {
      this.webSocket = null;
      this.authenticated = false;
      scheduleReconnect();
      ((Api) this.logger.atWarning().withCause(error)).log("ChatTranslation WS error");
   }

   private void handleMessage(String payload) {
      try {
         JsonElement elem = JsonParser.parseString(payload);
         if (!elem.isJsonObject()) {
            return;
         }
         JsonObject obj = elem.getAsJsonObject();
         String type = obj.has("type") ? obj.get("type").getAsString() : "";
         if ("hello_ack".equalsIgnoreCase(type)) {
            boolean ok = obj.has("ok") && obj.get("ok").getAsBoolean();
            if (ok) {
               this.authenticated = true;
               flushQueue();
            } else {
               ((Api) this.logger.atWarning()).log("ChatTranslation WS hello rejected");
            }
         } else if ("translations".equalsIgnoreCase(type)) {
            TranslationResponse response = this.gson.fromJson(obj, TranslationResponse.class);
            String messageId = obj.has("message_id") ? obj.get("message_id").getAsString() : null;
            this.dispatcher.dispatch(messageId, response);
         }
      } catch (Exception e) {
         ((Api) this.logger.atWarning().withCause(e)).log("ChatTranslation WS parse error");
      }
   }
}
