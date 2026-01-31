import asyncio
import json
import logging
import os
import time
from typing import Any, Dict, List, Optional

from dotenv import load_dotenv
from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from openai import OpenAI

load_dotenv()

MODEL = os.getenv("MODELO", "gpt-5-nano")
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")

LOG_LEVEL = os.getenv("LOG_LEVEL", "DEBUG").upper()
logging.basicConfig(level=LOG_LEVEL)
logger = logging.getLogger("tradutor-universal-api")

client = OpenAI(api_key=OPENAI_API_KEY) if OPENAI_API_KEY else None

app = FastAPI()

def truncate(text: str, limit: int = 500) -> str:
    if not isinstance(text, str):
        return ""
    return text if len(text) <= limit else text[:limit] + "...(truncated)"


def build_prompt(texto_original: str, jogadores_online: List[Dict[str, Any]]) -> str:
    jogadores_json = json.dumps(jogadores_online, ensure_ascii=False, indent=2)
    return f"""
Você é um tradutor de chat que traduz o chat de um servidor de Hytale. Traduza a mensagem recebida para os idiomas selecionados.

Realize a tradução da melhor forma possível adaptando gírias e expressões únicas para uma compatível para o idioma destino quando necessário.

O texto para jogadores falantes do mesmo idioma deve ser enviado **EXATAMENTE** igual ao original sem nenhuma alteração.

Retorne SOMENTE um JSON válido no formato:
{{"traducao":[{{"jogador":"Nome","texto_traduzido":"Mensagem"}}]}}

---

**Texto Original:**
"{texto_original}"

**Jogadores Online:**
{jogadores_json}
""".strip()


def parse_json_safe(text: str) -> Optional[Dict[str, Any]]:
    if not text:
        return None
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        start = text.find("{")
        end = text.rfind("}")
        if start >= 0 and end > start:
            try:
                return json.loads(text[start : end + 1])
            except json.JSONDecodeError:
                return None
    return None


def normalize_response(
    data: Optional[Dict[str, Any]],
    jogadores_online: List[Dict[str, Any]],
    texto_original: str,
    idioma_original: str,
) -> Dict[str, Any]:
    traducao_list = []
    data_list = data.get("traducao") if isinstance(data, dict) else None
    if isinstance(data_list, list):
        for item in data_list:
            if not isinstance(item, dict):
                continue
            jogador = item.get("jogador")
            texto = item.get("texto_traduzido")
            if isinstance(jogador, str) and jogador:
                traducao_list.append({"jogador": jogador, "texto_traduzido": texto if isinstance(texto, str) else ""})

    by_name = {i["jogador"]: i["texto_traduzido"] for i in traducao_list if i.get("jogador")}
    output = []

    for p in jogadores_online:
        jogador = p.get("jogador")
        idioma = p.get("idioma")
        if not isinstance(jogador, str) or not jogador:
            continue
        if isinstance(idioma, str) and isinstance(idioma_original, str) and idioma.strip().lower() == idioma_original.strip().lower():
            texto = texto_original
        else:
            texto = by_name.get(jogador, "")
            if not isinstance(texto, str) or not texto:
                texto = texto_original
        output.append({"jogador": jogador, "texto_traduzido": texto})

    return {"traducao": output}


def translate_with_openai(
    texto_original: str,
    jogadores_online: List[Dict[str, Any]],
    idioma_original: str,
) -> Dict[str, Any]:
    if not OPENAI_API_KEY or client is None:
        logger.warning("OPENAI_API_KEY nao configurada; retornando texto original.")
        return normalize_response({}, jogadores_online, texto_original, idioma_original)

    prompt = build_prompt(texto_original, jogadores_online)
    response_format = {
        "type": "json_schema",
        "name": "chat_translation",
        "description": "Lista de traducoes por jogador",
        "schema": {
            "type": "object",
            "properties": {
                "traducao": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "jogador": {"type": "string"},
                            "texto_traduzido": {"type": "string"},
                        },
                        "required": ["jogador", "texto_traduzido"],
                        "additionalProperties": False,
                    },
                }
            },
            "required": ["traducao"],
            "additionalProperties": False,
        },
        "strict": True,
    }
    logger.info("OpenAI request: model=%s, jogadores=%d, idioma_original=%s", MODEL, len(jogadores_online), idioma_original)
    start = time.time()
    try:
        result = client.responses.create(
            model=MODEL,
            instructions="Responda somente com JSON valido.",
            input=prompt,
            text={"format": response_format},
        )
        output_text = result.output_text if hasattr(result, "output_text") else ""
        logger.debug("OpenAI raw output: %s", truncate(output_text, 2000))
        parsed = parse_json_safe(output_text)
        if parsed is None:
            logger.warning("OpenAI output nao e JSON valido; usando fallback.")
        normalized = normalize_response(parsed or {}, jogadores_online, texto_original, idioma_original)
        logger.info("OpenAI ok em %.2fs, traducoes=%d", time.time() - start, len(normalized.get("traducao", [])))
        return normalized
    except Exception as e:
        logger.exception("OpenAI erro: %s", e)
        return normalize_response({}, jogadores_online, texto_original, idioma_original)


class ConnectionManager:
    def __init__(self) -> None:
        self.connections: Dict[str, WebSocket] = {}

    async def register(self, server_id: str, websocket: WebSocket) -> None:
        self.connections[server_id] = websocket

    def remove(self, server_id: Optional[str]) -> None:
        if server_id and server_id in self.connections:
            self.connections.pop(server_id, None)


manager = ConnectionManager()


@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket) -> None:
    await websocket.accept()
    server_id: Optional[str] = None
    logger.info("WS connect: %s", websocket.client)
    try:
        while True:
            raw = await websocket.receive_text()
            logger.debug("WS recv raw: %s", truncate(raw, 1000))
            try:
                payload = json.loads(raw)
            except json.JSONDecodeError:
                logger.warning("WS JSON invalido recebido")
                continue

            msg_type = payload.get("type")
            if msg_type == "hello":
                server_id = payload.get("server_id") or "unknown"
                await manager.register(server_id, websocket)
                await websocket.send_text(json.dumps({"type": "hello_ack", "server_id": server_id, "ok": True}))
                logger.info("Servidor conectado: %s", server_id)
                continue

            if msg_type == "chat":
                asyncio.create_task(handle_chat_message(server_id, websocket, payload))
    except WebSocketDisconnect:
        manager.remove(server_id)
        logger.info("Servidor desconectado: %s", server_id)


async def handle_chat_message(server_id: Optional[str], websocket: WebSocket, payload: Dict[str, Any]) -> None:
    texto_original = payload.get("texto_original") or ""
    idioma_original = payload.get("idioma_original") or ""
    jogadores_online = payload.get("jogadores_online") or []
    message_id = payload.get("message_id") or ""
    jogador = payload.get("jogador") or ""

    logger.info(
        "Chat recebido: server_id=%s message_id=%s jogador=%s idioma_original=%s jogadores=%d texto_len=%d",
        server_id,
        message_id,
        jogador,
        idioma_original,
        len(jogadores_online) if isinstance(jogadores_online, list) else 0,
        len(texto_original),
    )

    traducao = await asyncio.to_thread(
        translate_with_openai,
        texto_original,
        jogadores_online,
        idioma_original,
    )

    response = {
        "type": "translations",
        "server_id": server_id or "",
        "message_id": message_id,
        "traducao": traducao.get("traducao", []),
    }
    logger.debug("WS send translations: %s", truncate(json.dumps(response, ensure_ascii=False), 1000))
    await websocket.send_text(json.dumps(response, ensure_ascii=False))


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("tradutor:app", host="127.0.0.1", port=5521, log_level="info")
