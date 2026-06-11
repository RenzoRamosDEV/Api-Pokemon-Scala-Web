# Servidor MCP — Pokémon

Servidor [MCP](https://modelcontextprotocol.io) que expone los datos Pokémon de
esta app Scala/Play como herramientas que cualquier cliente MCP puede consumir.

Envuelve los endpoints `/api/*` de la app Play, así que **la app debe estar
corriendo** (`sbt run`, por defecto en `http://localhost:9000`).

## Herramientas expuestas

`get_pokemon`, `get_movimientos`, `get_naturalezas`, `get_objetos`,
`get_bayas`, `get_tipos`, `get_habilidades` — cada una recibe `search: str`.

## Ejecutar

El servidor habla por **stdio**: normalmente lo lanza un cliente MCP como
subproceso, no se ejecuta a mano. Para probarlo de forma aislada:

```bash
pip install -r requirements.txt
SCALA_APP_URL=http://localhost:9000 python server.py
```

## Conectarlo a un cliente MCP

Cualquier cliente MCP puede usarlo declarando el comando que lo lanza. Ejemplo
para Claude Desktop (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "pokemon": {
      "command": "python",
      "args": ["/ruta/abs/poke-api-scala-web/mcp_server/server.py"],
      "env": { "SCALA_APP_URL": "http://localhost:9000" }
    }
  }
}
```

El chatbot ReAct de este repo hermano lo declara igual en su `mcp_servers.json`.

## Variables de entorno

| Variable | Descripción |
|---|---|
| `SCALA_APP_URL` | URL base de la app Play (def. `http://localhost:9000`). |
