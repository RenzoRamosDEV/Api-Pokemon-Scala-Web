# Pokémon ReAct Agent — Diseño

**Fecha:** 2026-06-11  
**Estado:** Aprobado

---

## Resumen

Un chatbot CLI en Python que usa el patrón ReAct (Reasoning + Acting) con la API de OpenAI para responder preguntas sobre Pokémon en lenguaje natural. El agente consulta en tiempo real la app Scala/Play existente mediante nuevos endpoints JSON que se añaden al backend.

---

## Dos proyectos, una sola responsabilidad cada uno

| Proyecto | Ruta | Cambio |
|---|---|---|
| App Scala (existente) | `poke-api-scala-web/` | Se añaden 7 rutas `/api/*` + JSON writers |
| Chatbot Python (nuevo) | `/home/renzo-ramos/Workspace/AI/chat-bot-reactagents-mcp-pokeapi-scala/` | Proyecto nuevo, todo el código AI |

La app Scala **no se modifica estructuralmente** — solo se extiende con serialización JSON de salida y rutas nuevas.

---

## Parte 1 — Cambios en el backend Scala

### 1.1 JSON Writers en los modelos

Los modelos ya usan `spray-json` para deserializar datos de PokéAPI, pero los métodos `write` están vacíos. Se implementan los writers de salida para:

- `Pokemon` — id, name, types, abilities, stats, height, weight, baseExperience, sprites.frontDefault
- `Move` — id, name, power, accuracy, pp, priority, damageClass, moveType, shortEffect
- `Ability` — id, name, shortEffect, pokemonCount
- `Nature` — id, name, increasedStat, decreasedStat, hatesFlavor, likesFlavor
- `Item` — id, name, cost, category, sprite, shortEffect
- `Berry` — id, name, growthTime, maxHarvest, naturalGiftPower, naturalGiftType, smoothness, firmness, flavors
- `GameType` — id, name, doubleDamageTo, halfDamageTo, noDamageTo, doubleDamageFrom, halfDamageFrom, noDamageFrom, pokemonCount

### 1.2 Nuevas acciones en controladores existentes

Cada controlador existente recibe una nueva acción `apiIndex` que reutiliza la lógica de búsqueda y serializa a JSON. Sin paginación — devuelve todos los resultados filtrados por `q`.

```scala
def apiIndex(q: Option[String] = None) = Action.async { implicit request =>
  service.search(q).map(items => Ok(items.toJson))
}
```

### 1.3 Nuevas rutas en `conf/routes`

```
GET  /api/pokemon       controllers.PokedexController.apiIndex(q: Option[String] ?= None)
GET  /api/movimientos   controllers.MovesController.apiIndex(q: Option[String] ?= None)
GET  /api/naturalezas   controllers.NaturesController.apiIndex(q: Option[String] ?= None)
GET  /api/objetos       controllers.ItemsController.apiIndex(q: Option[String] ?= None)
GET  /api/bayas         controllers.BerriesController.apiIndex(q: Option[String] ?= None)
GET  /api/tipos         controllers.TypesController.apiIndex(q: Option[String] ?= None)
GET  /api/habilidades   controllers.AbilitiesController.apiIndex(q: Option[String] ?= None)
```

Todas devuelven `Content-Type: application/json` con un array JSON de los items.

---

## Parte 2 — Chatbot Python

### 2.1 Estructura de directorios

```
chat-bot-reactagents-mcp-pokeapi-scala/
├── tools/
│   ├── __init__.py
│   ├── definitions.py     # 7 tool schemas en formato OpenAI
│   └── executor.py        # HTTP GET a localhost:9000/api/...
├── react_agent/
│   ├── __init__.py
│   └── agent.py           # Bucle ReAct con OpenAI tool use
├── cli.py                 # Entry point: input/output, historial en sesión
├── .env                   # OPENAI_API_KEY + SCALA_APP_URL
├── .env.example
├── .gitignore
└── requirements.txt
```

### 2.2 Responsabilidades por módulo

| Módulo | Responsabilidad única |
|---|---|
| `tools/definitions.py` | Define los 7 schemas JSON para OpenAI. Sin lógica. |
| `tools/executor.py` | Ejecuta la herramienta: hace `requests.get()` a la app Scala y retorna el JSON como string. Sin conocimiento de OpenAI. |
| `react_agent/agent.py` | Gestiona el bucle ReAct con la API de OpenAI. Sin HTTP directo. |
| `cli.py` | Loop de conversación, display de Thought/Act/Observation/Answer. Sin lógica de negocio. |

### 2.3 Herramientas MCP

| Herramienta | Endpoint Scala | Parámetro |
|---|---|---|
| `get_pokemon` | `GET /api/pokemon?q=` | `search: str` |
| `get_movimientos` | `GET /api/movimientos?q=` | `search: str` |
| `get_naturalezas` | `GET /api/naturalezas?q=` | `search: str` |
| `get_objetos` | `GET /api/objetos?q=` | `search: str` |
| `get_bayas` | `GET /api/bayas?q=` | `search: str` |
| `get_tipos` | `GET /api/tipos?q=` | `search: str` |
| `get_habilidades` | `GET /api/habilidades?q=` | `search: str` |

### 2.4 Bucle ReAct (`agent.py`)

```
messages = [system_prompt, {role: user, content: pregunta}]

loop:
  response = openai.chat.completions.create(
      model="gpt-4o", tools=TOOLS, messages=messages
  )

  if finish_reason == "tool_calls":
      mostrar texto previo como 💭 Thought
      por cada tool_call:
          mostrar ⚡ Act: nombre(args)
          resultado = executor.run(tool_name, args)
          mostrar 👁️  Observation: resultado
          append tool_result a messages
      continuar loop

  if finish_reason == "stop":
      mostrar 🤖 respuesta final
      retornar texto
```

### 2.5 Display en terminal

```
🎮 Pokémon ReAct Agent — escribe 'salir' para terminar
──────────────────────────────────────────────────────
Tú: ¿Qué habilidad tiene Gengar?

💭 Necesito buscar a Gengar para ver sus habilidades.
⚡ Act: get_pokemon(search="gengar")
👁️  Observation: [{"name": "gengar", "abilities": [...], ...}]

💭 Ahora busco el detalle de cursed-body.
⚡ Act: get_habilidades(search="cursed-body")
👁️  Observation: [{"name": "cursed-body", "shortEffect": "..."}]

🤖 Gengar tiene la habilidad Cuerpo Maldito...
──────────────────────────────────────────────────────
Tú:
```

### 2.6 Dependencias Python

```
openai>=1.30.0
requests>=2.31.0
python-dotenv>=1.0.0
```

### 2.7 Variables de entorno

```env
OPENAI_API_KEY=sk-...
SCALA_APP_URL=http://localhost:9000
```

---

## Flujo completo de una consulta

```
Usuario escribe pregunta
        │
        ▼
    cli.py agrega a historial y llama agent.chat()
        │
        ▼
    agent.py llama OpenAI con tools definidos
        │
        ├─ tool_calls → executor.py → GET /api/... → App Scala → PokéAPI
        │      └─ resultado inyectado como tool_result → loop
        │
        └─ stop → respuesta final retornada a cli.py → impresa en terminal
```

---

## Fuera de alcance (roadmap)

- Persistencia del historial en disco (JSON)
- Herramienta `comparar_pokemon(a, b)`
- Caché local de respuestas Scala
- Endpoints JSON en Scala para evitar el scraping (ya incluido en este diseño)
