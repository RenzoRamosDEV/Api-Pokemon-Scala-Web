"""Servidor MCP del proyecto Pokémon (Scala/Play).

Expone las 7 entidades Pokémon como herramientas MCP. Cada herramienta envuelve
un endpoint JSON `/api/*` de la app Play. Cualquier cliente MCP (este chatbot,
Claude Desktop, Claude Code, etc.) puede conectarse y usarlas.

Transporte: stdio (el cliente lanza este script como subproceso).

Patrón replicable: para dar MCP a otro proyecto tuyo, copia este archivo,
cambia el nombre del servidor, la URL base y define sus @mcp.tool().
"""

import os

import requests
from mcp.server.fastmcp import FastMCP

SCALA_APP_URL = os.getenv("SCALA_APP_URL", "http://localhost:9000")
TIMEOUT = 30

mcp = FastMCP("pokemon")


def _get(path: str, search: str) -> str:
    """GET a la app Scala; devuelve el cuerpo JSON (o un error JSON) como string."""
    url = f"{SCALA_APP_URL}{path}"
    try:
        resp = requests.get(url, params={"q": search}, timeout=TIMEOUT)
        resp.raise_for_status()
        return resp.text
    except requests.RequestException as exc:
        return f'{{"error": "fallo al consultar {url}: {exc}"}}'


@mcp.tool()
def get_pokemon(search: str) -> str:
    """Busca Pokémon por nombre (inglés, minúsculas, guiones: 'gengar', 'mr-mime').
    Devuelve id, tipos, habilidades, estadísticas base, altura, peso,
    experiencia base y sprite."""
    return _get("/api/pokemon", search)


@mcp.tool()
def get_movimientos(search: str) -> str:
    """Busca movimientos por nombre (p. ej. 'thunderbolt', 'ice-beam').
    Devuelve potencia, precisión, PP, prioridad, clase de daño, tipo y efecto."""
    return _get("/api/movimientos", search)


@mcp.tool()
def get_naturalezas(search: str) -> str:
    """Busca naturalezas por nombre (p. ej. 'adamant').
    Devuelve la estadística que sube, la que baja y los sabores que gusta/disgusta."""
    return _get("/api/naturalezas", search)


@mcp.tool()
def get_objetos(search: str) -> str:
    """Busca objetos por nombre (p. ej. 'leftovers', 'choice-band').
    Devuelve coste, categoría, sprite y efecto."""
    return _get("/api/objetos", search)


@mcp.tool()
def get_bayas(search: str) -> str:
    """Busca bayas por nombre (p. ej. 'sitrus').
    Devuelve tiempo de cultivo, cosecha máxima, poder/tipo de Don Natural,
    suavidad, firmeza y sabores."""
    return _get("/api/bayas", search)


@mcp.tool()
def get_tipos(search: str) -> str:
    """Busca tipos por nombre (p. ej. 'ghost', 'dragon').
    Devuelve las relaciones de daño ofensivas y defensivas."""
    return _get("/api/tipos", search)


@mcp.tool()
def get_habilidades(search: str) -> str:
    """Busca habilidades por nombre (p. ej. 'cursed-body', 'levitate').
    Devuelve su efecto y cuántos Pokémon la tienen."""
    return _get("/api/habilidades", search)


if __name__ == "__main__":
    mcp.run()
