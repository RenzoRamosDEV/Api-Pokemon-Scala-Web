"""Gestor de clientes MCP.

Lanza uno o varios servidores MCP (por stdio), agrega todas sus herramientas en
un único catálogo y enruta cada llamada al servidor correcto. Convierte los
schemas MCP al formato de tool-calling de OpenAI para que el agente los use.

Las herramientas se exponen con el nombre `"{servidor}__{herramienta}"` para
evitar colisiones cuando hay varios proyectos conectados.
"""

import os
from contextlib import AsyncExitStack
from dataclasses import dataclass, field

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

SEP = "__"


@dataclass
class MCPManager:
    servers: list[dict]
    _stack: AsyncExitStack = field(default_factory=AsyncExitStack)
    # nombre namespaced -> (sesión, nombre original de la herramienta)
    _routes: dict = field(default_factory=dict)
    openai_tools: list = field(default_factory=list)

    async def __aenter__(self) -> "MCPManager":
        await self.setup()
        return self

    async def __aexit__(self, *exc) -> None:
        await self._stack.aclose()

    async def setup(self) -> None:
        """Arranca todos los servidores configurados y construye el catálogo."""
        for srv in self.servers:
            name = srv["name"]
            # Cada servidor MCP se lanza como subproceso y se habla por stdio.
            # env: heredamos el del proceso + lo que declare el servidor (p.ej. URLs).
            params = StdioServerParameters(
                command=srv["command"],
                args=srv.get("args", []),
                env={**os.environ, **srv.get("env", {})},
            )
            # AsyncExitStack mantiene vivas todas las conexiones hasta aclose().
            read, write = await self._stack.enter_async_context(stdio_client(params))
            session = await self._stack.enter_async_context(ClientSession(read, write))
            await session.initialize()  # handshake MCP (capabilities, versión...)

            # Descubrimiento dinámico: el servidor nos dice qué herramientas tiene.
            listed = await session.list_tools()
            for tool in listed.tools:
                # Prefijamos con el servidor para evitar colisiones entre proyectos
                # (p.ej. "pokemon__get_tipos" vs "tournament__get_tipos").
                namespaced = f"{name}{SEP}{tool.name}"
                self._routes[namespaced] = (session, tool.name)
                # Traducimos el schema MCP -> formato de función de OpenAI. El
                # inputSchema de MCP ya es JSON-Schema, que es justo lo que pide OpenAI.
                self.openai_tools.append({
                    "type": "function",
                    "function": {
                        "name": namespaced,
                        "description": f"[{name}] {tool.description or ''}".strip(),
                        "parameters": tool.inputSchema or {"type": "object", "properties": {}},
                    },
                })

    async def call(self, namespaced_name: str, args: dict) -> str:
        """Ejecuta la herramienta en su servidor MCP y devuelve el texto resultante."""
        route = self._routes.get(namespaced_name)
        if route is None:
            return f'{{"error": "herramienta desconocida: {namespaced_name}"}}'
        session, original = route
        # Llamada MCP real (tools/call). Devolvemos el original, no el namespaced.
        result = await session.call_tool(original, arguments=args or {})
        # El resultado MCP es una lista de bloques de contenido; nos quedamos con
        # el texto (nuestras tools devuelven el JSON de /api/* como texto).
        parts = [c.text for c in result.content if getattr(c, "type", None) == "text"]
        return "\n".join(parts) if parts else "(sin contenido)"

    @property
    def server_names(self) -> list[str]:
        return [s["name"] for s in self.servers]
