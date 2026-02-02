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

A lista de jogadores abaixo contém no máximo 1 jogador por idioma. Traduza para o idioma indicado em cada entrada.

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


def normalize_language(lang: Any) -> str:
    if not isinstance(lang, str):
        return ""
    return lang.strip().lower()


def dedupe_players_by_language(
    jogadores_online: List[Dict[str, Any]]
) -> tuple[list[dict[str, str]], dict[str, list[str]], dict[str, str]]:
    deduped: list[dict[str, str]] = []
    grouped: dict[str, list[str]] = {}
    rep_to_lang: dict[str, str] = {}
    seen_langs: set[str] = set()
    if not isinstance(jogadores_online, list):
        return deduped, grouped, rep_to_lang

    for item in jogadores_online:
        if not isinstance(item, dict):
            continue
        jogador = item.get("jogador")
        if not isinstance(jogador, str) or not jogador:
            continue
        idioma = item.get("idioma")
        lang_key = normalize_language(idioma)
        grouped.setdefault(lang_key, []).append(jogador)
        if lang_key in seen_langs:
            continue
        seen_langs.add(lang_key)
        rep_to_lang[jogador.strip().lower()] = lang_key
        deduped.append({"jogador": jogador, "idioma": idioma if isinstance(idioma, str) else ""})

    return deduped, grouped, rep_to_lang


def validate_translation_output(data: Optional[Dict[str, Any]], expected_names: List[str]) -> bool:
    if not isinstance(data, dict):
        return False
    items = data.get("traducao")
    if not isinstance(items, list):
        return False
    if not expected_names:
        return True
    names: set[str] = set()
    for item in items:
        if not isinstance(item, dict):
            return False
        jogador = item.get("jogador")
        texto = item.get("texto_traduzido")
        if not isinstance(jogador, str) or not jogador:
            return False
        if not isinstance(texto, str):
            return False
        names.add(jogador.strip().lower())
    for expected in expected_names:
        if expected.strip().lower() not in names:
            return False
    return True


def build_translation_maps(
    data: Optional[Dict[str, Any]],
    rep_to_lang: Dict[str, str],
) -> tuple[dict[str, str], dict[str, str]]:
    by_name: dict[str, str] = {}
    by_lang: dict[str, str] = {}
    items = data.get("traducao") if isinstance(data, dict) else None
    if isinstance(items, list):
        for item in items:
            if not isinstance(item, dict):
                continue
            jogador = item.get("jogador")
            texto = item.get("texto_traduzido")
            if not isinstance(jogador, str) or not jogador:
                continue
            if not isinstance(texto, str):
                texto = ""
            name_key = jogador.strip().lower()
            by_name[name_key] = texto
            lang_key = rep_to_lang.get(name_key)
            if lang_key is not None:
                by_lang[lang_key] = texto
    return by_name, by_lang


def normalize_response(
    data: Optional[Dict[str, Any]],
    jogadores_online: List[Dict[str, Any]],
    texto_original: str,
    idioma_original: str,
    rep_to_lang: Optional[Dict[str, str]] = None,
) -> Dict[str, Any]:
    rep_map = rep_to_lang or {}
    by_name, by_lang = build_translation_maps(data, rep_map)
    output = []
    base_lang = normalize_language(idioma_original)

    for p in jogadores_online:
        jogador = p.get("jogador")
        idioma = p.get("idioma")
        if not isinstance(jogador, str) or not jogador:
            continue
        lang_key = normalize_language(idioma)
        if lang_key and base_lang and lang_key == base_lang:
            texto = texto_original
        elif not lang_key and not base_lang:
            texto = texto_original
        else:
            texto = by_name.get(jogador.strip().lower(), "")
            if not isinstance(texto, str) or not texto:
                texto = by_lang.get(lang_key, "")
            if not isinstance(texto, str) or not texto:
                texto = texto_original
        output.append({"jogador": jogador, "texto_traduzido": texto})

    return {"traducao": output}


def translate_with_openai(
    texto_original: str,
    jogadores_online: List[Dict[str, Any]],
    idioma_original: str,
) -> Dict[str, Any]:
    jogadores_online = jogadores_online if isinstance(jogadores_online, list) else []
    deduped, _, rep_to_lang = dedupe_players_by_language(jogadores_online)
    if not OPENAI_API_KEY or client is None:
        logger.warning("OPENAI_API_KEY nao configurada; retornando texto original.")
        return normalize_response({}, jogadores_online, texto_original, idioma_original, rep_to_lang)
    if not deduped:
        return {"traducao": []}

    prompt = build_prompt(texto_original, deduped)
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
    expected_names = [item.get("jogador") for item in deduped if isinstance(item, dict) and item.get("jogador")]
    logger.info(
        "OpenAI request: model=%s, jogadores=%d (dedupe=%d), idioma_original=%s",
        MODEL,
        len(jogadores_online),
        len(deduped),
        idioma_original,
    )

    for attempt in range(1, 3):
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
            if not validate_translation_output(parsed, expected_names):
                logger.warning("OpenAI output invalido (tentativa %d/2)", attempt)
                continue
            normalized = normalize_response(parsed or {}, jogadores_online, texto_original, idioma_original, rep_to_lang)
            logger.info(
                "OpenAI ok em %.2fs, traducoes=%d",
                time.time() - start,
                len(normalized.get("traducao", [])),
            )
            return normalized
        except Exception as e:
            logger.exception("OpenAI erro na tentativa %d/2: %s", attempt, e)

    logger.warning("OpenAI falhou nas tentativas; usando fallback.")
    return normalize_response({}, jogadores_online, texto_original, idioma_original, rep_to_lang)


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
    jogador_uuid = payload.get("jogador_uuid") or ""

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
        "jogador": jogador,
        "jogador_uuid": jogador_uuid,
        "traducao": traducao.get("traducao", []),
    }
    logger.debug("WS send translations: %s", truncate(json.dumps(response, ensure_ascii=False), 1000))
    await websocket.send_text(json.dumps(response, ensure_ascii=False))


if __name__ == "__main__":
    import uvicorn

    uvicorn.run("tradutor:app", host="127.0.0.1", port=5521, log_level="info")
