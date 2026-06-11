"""Servidor web del chatbot Pokémon ReAct (cliente MCP).

Sustituye al antiguo CLI por una interfaz web mínima (chat en blanco y negro).
Reusa exactamente las mismas piezas: el agente ReAct y el gestor MCP.

- Los servidores MCP se arrancan UNA vez al iniciar (lifespan) y se reutilizan
  en todas las peticiones, manteniendo vivas las sesiones stdio.
- El agente es único y compartido, así que conserva el historial de la
  conversación entre mensajes (igual que una sesión de chat).

Ejecutar:  python app.py   (sirve en http://localhost:8080)
"""

import asyncio
import json
import os
from contextlib import asynccontextmanager
from pathlib import Path

import uvicorn
from dotenv import load_dotenv
from fastapi import FastAPI
from fastapi.responses import HTMLResponse, JSONResponse, StreamingResponse
from pydantic import BaseModel

from mcp_client import MCPManager
from react_agent import ReActAgent

load_dotenv()

BASE = Path(__file__).parent
CONFIG_PATH = BASE / "mcp_servers.json"
INDEX_HTML = BASE / "static" / "index.html"

# Estado del proceso: el manager MCP y el agente, creados en el lifespan.
state: dict = {}


@asynccontextmanager
async def lifespan(_app: FastAPI):
    """Arranca los servidores MCP al iniciar y los cierra al apagar."""
    if not os.getenv("OPENAI_API_KEY"):
        raise RuntimeError("Falta OPENAI_API_KEY. Copia .env.example a .env y rellénala.")

    servers = json.loads(CONFIG_PATH.read_text(encoding="utf-8"))["servers"]
    manager = MCPManager(servers)
    await manager.__aenter__()          # lanza subprocesos MCP + descubre tools
    state["manager"] = manager
    state["agent"] = ReActAgent(manager)
    try:
        yield
    finally:
        await manager.__aexit__(None, None, None)


app = FastAPI(lifespan=lifespan, title="Pokémon ReAct Agent")


class ChatIn(BaseModel):
    message: str


@app.get("/")
async def index() -> HTMLResponse:
    """Sirve la página de chat."""
    return HTMLResponse(INDEX_HTML.read_text(encoding="utf-8"))


@app.get("/info")
async def info() -> JSONResponse:
    """Metadatos para la cabecera de la UI (servidores y nº de tools)."""
    manager: MCPManager = state["manager"]
    return JSONResponse({
        "servers": manager.server_names,
        "tools": len(manager.openai_tools),
    })


@app.post("/chat/stream")
async def chat_stream(body: ChatIn) -> StreamingResponse:
    """Procesa un mensaje y emite el flujo ReAct en vivo por SSE.

    Cada paso del agente (thought / act / observation) se empuja a una cola y se
    envía al navegador en cuanto ocurre; al final se manda el evento `answer`.
    El frontend pinta el razonamiento mientras el bot "piensa".
    """
    agent: ReActAgent = state["agent"]
    queue: asyncio.Queue = asyncio.Queue()

    # on_event se invoca de forma síncrona dentro de agent.chat (en este mismo
    # event loop), así que put_nowait es seguro y no bloquea.
    agent.on_event = lambda kind, text: queue.put_nowait({"type": kind, "text": text})

    async def runner() -> None:
        try:
            answer = await agent.chat(body.message)
            queue.put_nowait({"type": "answer", "text": answer})
        except Exception as exc:  # noqa: BLE001 - se reporta al cliente
            queue.put_nowait({"type": "error", "text": str(exc)})
        finally:
            queue.put_nowait(None)  # centinela de fin de stream

    async def gen():
        task = asyncio.create_task(runner())
        while True:
            item = await queue.get()
            if item is None:
                break
            yield f"data: {json.dumps(item, ensure_ascii=False)}\n\n"
        await task

    return StreamingResponse(gen(), media_type="text/event-stream")


@app.post("/reset")
async def reset() -> JSONResponse:
    """Empieza una conversación nueva (borra el historial)."""
    state["agent"] = ReActAgent(state["manager"])
    return JSONResponse({"ok": True})


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8080)
