# Implementación — Pokémon ReAct Agent + MCP

**Fecha:** 2026-06-11
**Estado:** Implementado y probado

Documento maestro de todo lo construido: **qué** se implementó, **cómo funciona**
y **cómo probarlo**. Spec de origen: [specs/2026-06-11-pokemon-react-agent-design.md](superpowers/specs/2026-06-11-pokemon-react-agent-design.md).

---

## 1. Resumen en una frase

Un chatbot web en Python (patrón **ReAct**, LLM de OpenAI) responde preguntas
sobre Pokémon en lenguaje natural. No accede a los datos directamente: actúa como
**cliente MCP** y los obtiene de un **servidor MCP** que vive dentro de la app
Scala/Play y que envuelve sus endpoints REST `/api/*`, que a su vez consultan
PokéAPI en tiempo real.

---

## 2. Arquitectura

```
 ┌──────────────┐   tool-calling (HTTP)   ┌──────────────────────────┐
 │   OpenAI     │◀───────────────────────▶│  Agente ReAct (cliente)  │
 │   gpt-4o     │                         │  react_agent/agent.py    │
 └──────────────┘                         └────────────┬─────────────┘
                                                       │ catálogo agregado de tools
                                          MCP (JSON-RPC sobre stdio)
                                                       │
                                          ┌────────────▼─────────────┐
                                          │  mcp_client/manager.py    │
                                          │  (lanza y enruta servidores)
                                          └────────────┬─────────────┘
                              ┌────────────────────────┼───────────────────────┐
                              ▼                         ▼                       ▼
                   ┌────────────────────┐     ┌────────────────┐     ┌────────────────┐
                   │ MCP: pokemon       │     │ MCP: (futuro)  │ ... │ MCP: (futuro)  │
                   │ mcp_server/server.py     │ otro proyecto  │     │ otro proyecto  │
                   └─────────┬──────────┘     └────────────────┘     └────────────────┘
                             │ HTTP GET /api/*
                   ┌─────────▼──────────┐
                   │  App Scala / Play   │  controllers.*.apiIndex
                   └─────────┬──────────┘
                             │ HTTP
                   ┌─────────▼──────────┐
                   │     PokéAPI v2      │
                   └────────────────────┘
```

**Idea clave:** el agente es agnóstico al dominio. Para que hable con otro
proyecto tuyo, basta crear un servidor MCP en ese proyecto y declararlo en la
config; no se toca el código del agente.

---

## 3. Dos repositorios

| Repo | Ruta | Rol |
|---|---|---|
| App Scala (este) | `poke-api-scala-web/` | Backend REST + servidor MCP |
| Chatbot Python | `poke-api-scala-web/chatbot/` | Agente ReAct + cliente MCP (web) |

> Todo vive ya en un único proyecto: el chatbot está en la subcarpeta `chatbot/`.

---

## 4. Qué se implementó

### 4.1 Backend Scala — API REST JSON

- **JSON writers** (antes vacíos) en
  [`app/models/PokemonModels.scala`](../app/models/PokemonModels.scala) y
  [`app/models/GameModels.scala`](../app/models/GameModels.scala): serializan cada
  entidad a un JSON plano y limpio (pensado para el LLM). Cada formato spray-json
  tiene `read` (parsea PokéAPI) y `write` (emite nuestro `/api/*`).
- **Helper `englishShortEffect`** en `GameModels`: elige el efecto en inglés de
  `effect_entries` (con fallback). Resuelve que los efectos salían en francés.
- **Acción `apiIndex(q)`** en los 7 controladores: reutiliza la búsqueda, aplica
  normalización espacio→guion, tope `apiLimit = 60` (en los grandes) y serializa
  con `.toJson`.
- **7 rutas** `/api/*` en [`conf/routes`](../conf/routes).
- Documentación de la API: [docs/API.md](API.md).

### 4.2 Servidor MCP

- [`mcp_server/server.py`](../mcp_server/server.py): servidor `FastMCP` que expone
  7 herramientas (`get_pokemon`, `get_movimientos`, …), cada una un wrapper HTTP
  sobre un endpoint `/api/*`. Transporte stdio.
- [`mcp_server/README.md`](../mcp_server/README.md) y `requirements.txt`.

### 4.3 Chatbot Python (cliente MCP + ReAct)

En `chatbot/`:

| Módulo | Responsabilidad |
|---|---|
| `mcp_client/manager.py` | Lanza servidores MCP, descubre tools, las convierte a formato OpenAI, enruta llamadas. |
| `react_agent/agent.py` | Bucle ReAct async con OpenAI; delega tools al manager. |
| `app.py` | Servidor web FastAPI: arranca los MCP y sirve el chat (`/`, `/chat`, `/reset`, `/info`). |
| `static/index.html` | Interfaz de chat minimalista en blanco y negro. |
| `mcp_servers.json` | Declaración de servidores MCP a conectar. |

---

## 5. Cómo funciona (flujo de una pregunta)

1. El usuario escribe en la web (`static/index.html`) → `POST /chat` en `app.py`
   → `agent.chat(pregunta)`.
2. El agente llama a OpenAI pasando el **catálogo de tools** que el manager obtuvo
   de los servidores MCP.
3. Si OpenAI responde `finish_reason == "tool_calls"`:
   - El texto previo se muestra como **💭 Thought**.
   - Por cada tool: **⚡ Act**, el manager llama vía MCP al servidor correcto, que
     hace `GET /api/...`, y el resultado se muestra como **👁️ Observation** y se
     reinyecta al modelo.
   - Se repite el bucle.
4. Cuando OpenAI responde `finish_reason == "stop"`, ese texto es la **🤖
   respuesta final**.

El namespacing `servidor__herramienta` (p.ej. `pokemon__get_tipos`) evita
colisiones cuando hay varios proyectos conectados.

---

## 6. Cómo probarlo

### 6.1 Requisitos

- JDK 17+, SBT (backend)
- Python 3.10+ (chatbot y servidor MCP)
- `OPENAI_API_KEY`

### 6.2 Arrancar el backend

```bash
cd poke-api-scala-web
sbt run                       # http://localhost:9000
```

### 6.3 Probar la API REST directamente (sin IA)

```bash
curl 'http://localhost:9000/api/pokemon?q=gengar'
curl 'http://localhost:9000/api/tipos?q=ghost'
curl 'http://localhost:9000/api/habilidades?q=cursed-body'
```

### 6.4 Preparar y lanzar el chat web

```bash
cd chatbot          # dentro de poke-api-scala-web
uv venv .venv && uv pip install -r requirements.txt     # o python -m venv + pip
cp .env.example .env          # y rellena OPENAI_API_KEY
python app.py                 # sirve en http://localhost:8080
```

Abre **http://localhost:8080** en el navegador. La interfaz es un chat
minimalista en blanco y negro:
- Burbujas usuario (negro) / bot (blanco), respuestas en markdown (sprites, etc.).
- Línea sutil 🔧 con las herramientas usadas en cada respuesta.
- Botón **NUEVO** para reiniciar la conversación.

Endpoints del servidor: `GET /` (UI), `POST /chat`, `POST /reset`, `GET /info`.

---

## 7. Resultados de las pruebas (2026-06-11)

| # | Prueba | Resultado |
|---|---|---|
| 1 | Los 7 endpoints `/api/*` → HTTP 200 + `application/json` | ✅ |
| 2 | Forma del JSON por entidad coincide con el spec | ✅ |
| 3 | Cap `apiLimit=60` con `q` vacía | ✅ (60 ítems) |
| 3 | Normalización `"cursed body"` → `cursed-body` | ✅ |
| 3 | `shortEffect` en inglés (no francés) | ✅ |
| 3 | Campos `null` (move sin power) serializados | ✅ |
| 3 | Query sin resultados → `[]` | ✅ |
| 3 | `unknown`/`shadow` excluidos en tipos | ✅ |
| 4 | Manager MCP lista 7 tools namespaced con schema válido | ✅ |
| 4 | Una llamada por herramienta vía protocolo MCP | ✅ (7/7) |
| 4 | Tool desconocida → error JSON controlado | ✅ |
| 5 | Agente ReAct: pregunta multi-paso (Charizard → tipos) | ✅ |
| 5 | Agente ReAct: naturaleza / baya / habilidad | ✅ |
| 6 | Web: `GET /info` reporta servidores y nº de tools | ✅ |
| 6 | Web: `GET /` sirve el HTML del chat | ✅ |
| 6 | Web: `POST /chat` devuelve answer + tools usadas | ✅ |
| 6 | Web: `POST /reset` reinicia el historial | ✅ |
| — | `sbt compile` | ✅ |
| — | `py_compile` de todo el Python | ✅ |

---

## 8. Cómo añadir otro proyecto al bot (patrón replicable)

1. En el otro proyecto, crea `mcp_server/server.py` con `FastMCP` y sus
   `@mcp.tool()` (usa el de este repo como plantilla).
2. Añade una entrada en `chatbot/mcp_servers.json`:
   ```json
   { "name": "tournament",
     "command": "/ruta/.venv/bin/python",
     "args": ["/ruta/otro-proyecto/mcp_server/server.py"],
     "env": { "API_URL": "http://localhost:8000" } }
   ```
3. Reinicia el chatbot. Las nuevas tools aparecen como `tournament__<tool>`.

---

## 9. Decisiones de diseño

- **Tope `apiLimit = 60`**: el spec decía "sin paginación", pero sin tope una `q`
  vacía dispararía decenas de miles de fetch de detalle. El agente siempre pasa
  un término, así que es transparente.
- **Normalización espacio→guion**: PokéAPI usa guiones; los nombres no llevan
  espacios, así que la conversión solo puede mejorar el match. Se aplicó también a
  la búsqueda web por consistencia.
- **Proveedor OpenAI con puente MCP**: MCP es agnóstico al LLM. El manager traduce
  el `inputSchema` (JSON-Schema) de MCP al formato de función de OpenAI. (Con un
  cliente Claude el acoplamiento sería aún más directo, al ser MCP nativo de
  Anthropic.)
- **stdio como transporte**: estándar para servidores MCP locales; el cliente los
  lanza como subprocesos.

---

## 10. Fuera de alcance (roadmap)

- Servidores MCP para los demás proyectos (tournament-platform, etc.).
- Persistencia del historial de conversación.
- Caché local de respuestas.
- Herramienta `comparar_pokemon(a, b)`.
