# Pokémon ReAct Agent + MCP

Chatbot **web** en Python que responde preguntas en lenguaje natural usando el
patrón **ReAct** (Reasoning + Acting) con tool-calling de OpenAI. Las herramientas
**no** están aquí: las sirve un **servidor MCP** (en `../mcp_server/`, dentro de
la app Scala que lo aloja).

> Esta carpeta `chatbot/` vive dentro del proyecto `poke-api-scala-web/`. El
> backend Scala es la carpeta padre (`..`).

```
┌─────────────┐   tool-calling   ┌──────────────┐
│   OpenAI    │◀────────────────▶│  Agente ReAct│
└─────────────┘                  │ (cliente MCP)│
                                 └──────┬───────┘
                          MCP (stdio)   │  catálogo agregado de tools
                  ┌───────────────┬─────┴─────────┬───────────────┐
                  ▼               ▼               ▼
          ┌───────────────┐ ┌───────────┐  ┌───────────┐
          │ MCP: pokemon  │ │ MCP: otro │  │ MCP: ...  │   ← un servidor por proyecto
          │ (repo Scala)  │ │ proyecto  │  │           │
          └──────┬────────┘ └───────────┘  └───────────┘
                 ▼ HTTP /api/*
          ┌───────────────┐
          │  App Scala     │
          └───────────────┘
```

El agente es **agnóstico al dominio**: pide el catálogo de herramientas a los
servidores MCP declarados y enruta cada llamada. Para que el bot hable con otro
proyecto tuyo, solo añades su servidor MCP a `mcp_servers.json`.

## Requisitos

- Python 3.10+
- La app Scala corriendo (`sbt run`, por defecto en `http://localhost:9000`)
- Una `OPENAI_API_KEY`

## Instalación

```bash
python -m venv .venv && source .venv/bin/activate   # o: uv venv .venv
pip install -r requirements.txt                     # o: uv pip install -r requirements.txt
cp .env.example .env   # y rellena OPENAI_API_KEY
```

## Uso

1. Arranca la app Scala (carpeta padre): `cd .. && sbt run`
2. Vuelve a `chatbot/` y lanza el chat web: `python app.py`
3. Abre **http://localhost:8080** en el navegador.

`app.py` arranca automáticamente los servidores MCP de `mcp_servers.json` (por
stdio, como subprocesos), agrega sus herramientas y sirve una interfaz de chat
minimalista en blanco y negro.

```
┌────────────────────────────────────────────────────┐
│ POKÉMON ReAct   MCP: pokemon · 7 herramientas [NUEVO]│
├────────────────────────────────────────────────────┤
│                          ¿Qué habilidad tiene Gengar?│  ← tú (negro)
│                                                      │
│ ⚡ pokemon__get_pokemon(search="gengar")          ┐  │  ← flujo ReAct en vivo
│ 👁️ [{"abilities":[{"name":"cursed-body"…          │  │    (plegable: clic para
│ ⚡ pokemon__get_habilidades(search="cursed-body") ┘  │     ver/encoger entero)
│ Gengar tiene la habilidad Cuerpo Maldito...          │  ← respuesta (markdown)
├────────────────────────────────────────────────────┤
│ Escribe tu pregunta…                        [ENVIAR] │
└────────────────────────────────────────────────────┘
```

- **Flujo ReAct en vivo:** mientras el bot piensa, se muestran sus pasos
  (💭 Thought / ⚡ Act / 👁️ Observation) vía streaming SSE (`POST /chat/stream`).
  El bloque está colapsado y truncado; **clic** para verlo entero, **clic** de
  nuevo para encogerlo.
- El historial se mantiene durante la sesión (puedes encadenar preguntas).
- El botón **NUEVO** reinicia la conversación (`POST /reset`).
- Las respuestas se renderizan como markdown (negritas, listas, sprites).

## Añadir otro proyecto (patrón replicable)

1. En el otro proyecto, crea un `mcp_server/server.py` con `FastMCP` y sus
   `@mcp.tool()` (usa el del repo Scala como plantilla:
   `poke-api-scala-web/mcp_server/`).
2. Añade una entrada a `mcp_servers.json`:

   ```json
   {
     "name": "tournament",
     "command": "/ruta/al/.venv/bin/python",
     "args": ["/ruta/al/otro-proyecto/mcp_server/server.py"],
     "env": { "API_URL": "http://localhost:8000" }
   }
   ```

3. Reinicia el chatbot. Las nuevas herramientas aparecen como
   `tournament__<tool>` y el bot ya puede usarlas. Los nombres se prefijan con el
   servidor para evitar colisiones entre proyectos.

## Estructura

| Módulo | Responsabilidad |
|---|---|
| `mcp_client/manager.py` | Lanza servidores MCP, agrega tools, enruta llamadas, convierte schemas a formato OpenAI. |
| `react_agent/agent.py` | Bucle ReAct (async) con OpenAI; delega tools al manager. |
| `app.py` | Servidor web (FastAPI): arranca los MCP y sirve el chat. |
| `static/index.html` | Interfaz de chat minimalista (blanco y negro). |
| `mcp_servers.json` | Declaración de los servidores MCP a conectar. |

## Variables de entorno

| Variable | Descripción |
|---|---|
| `OPENAI_API_KEY` | Clave de API de OpenAI. |

> `SCALA_APP_URL` ya no se pone aquí: se pasa a cada servidor MCP desde su entrada
> `env` en `mcp_servers.json`.
