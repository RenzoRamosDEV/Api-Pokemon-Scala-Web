# API REST JSON — Pokédex Retro

Endpoints JSON que expone la app Play, además de las vistas HTML. Existen para
ser consumidos por programas (en concreto, el [servidor MCP](../mcp_server/) que
alimenta al chatbot ReAct), pero cualquier cliente HTTP puede usarlos.

- **Base URL:** `http://localhost:9000`
- **Método:** todos `GET`
- **Content-Type de respuesta:** `application/json`
- **Parámetro común:** `q` (opcional) — término de búsqueda por *substring* sobre
  el nombre. Se normaliza a minúsculas y los espacios se convierten en guiones
  (`mr mime` → `mr-mime`). Sin `q` se devuelve el catálogo (con tope, ver abajo).
- **Cuerpo:** siempre un **array JSON** de objetos (vacío `[]` si no hay match).

> ⚠️ **Tope de resultados (`apiLimit = 60`)**: las entidades grandes
> (pokémon, movimientos, objetos, bayas, habilidades) limitan a 60 ítems por
> respuesta para no disparar miles de peticiones de detalle contra PokéAPI.
> Tipos y naturalezas no lo necesitan (catálogo pequeño).

---

## Índice de endpoints

| Endpoint | Entidad | Paginado en web | Tope API |
|---|---|---|---|
| `GET /api/pokemon` | Pokémon | sí | 60 |
| `GET /api/movimientos` | Movimientos | sí | 60 |
| `GET /api/objetos` | Objetos | sí | 60 |
| `GET /api/bayas` | Bayas | sí | 60 |
| `GET /api/habilidades` | Habilidades | sí | 60 |
| `GET /api/tipos` | Tipos | no | — |
| `GET /api/naturalezas` | Naturalezas | no | — |

---

## `GET /api/pokemon`

```bash
curl 'http://localhost:9000/api/pokemon?q=pikachu'
```

```json
[
  {
    "id": 25,
    "name": "pikachu",
    "types": ["electric"],
    "abilities": [{ "name": "static", "isHidden": false },
                  { "name": "lightning-rod", "isHidden": true }],
    "stats": { "hp": 35, "attack": 55, "defense": 40,
               "special-attack": 50, "special-defense": 50, "speed": 90 },
    "height": 4,
    "weight": 60,
    "baseExperience": 112,
    "sprite": "https://raw.githubusercontent.com/PokeAPI/sprites/.../25.png"
  }
]
```

| Campo | Tipo | Notas |
|---|---|---|
| `id` | int | |
| `name` | string | |
| `types` | string[] | ordenado por slot |
| `abilities` | {name, isHidden}[] | ordenado por slot |
| `stats` | objeto | nombre de stat → valor base |
| `height`, `weight` | int | unidades PokéAPI (decímetros / hectogramos) |
| `baseExperience` | int \| null | |
| `sprite` | string \| null | URL del sprite frontal |

---

## `GET /api/movimientos`

```bash
curl 'http://localhost:9000/api/movimientos?q=tackle'
```

```json
[
  { "id": 33, "name": "tackle", "power": 40, "accuracy": 100, "pp": 35,
    "priority": 0, "damageClass": "physical", "moveType": "normal",
    "shortEffect": "Inflicts regular damage with no additional effect." }
]
```

| Campo | Tipo | Notas |
|---|---|---|
| `power`, `accuracy` | int \| null | `null` en movimientos de estado (p.ej. `growth`) |
| `damageClass` | string | `physical` / `special` / `status` |
| `moveType` | string | tipo del movimiento |
| `shortEffect` | string | efecto corto **en inglés** (ver nota de idioma) |

---

## `GET /api/tipos`

```bash
curl 'http://localhost:9000/api/tipos?q=fire'
```

```json
[
  { "id": 10, "name": "fire",
    "doubleDamageTo":   ["grass","ice","bug","steel"],
    "halfDamageTo":     ["fire","water","rock","dragon"],
    "noDamageTo":       [],
    "doubleDamageFrom": ["water","ground","rock"],
    "halfDamageFrom":   ["fire","grass","ice","bug","steel","fairy"],
    "noDamageFrom":     [],
    "pokemonCount": 86 }
]
```

> Los pseudo-tipos `unknown` y `shadow` se excluyen siempre.

---

## `GET /api/habilidades`

```bash
curl 'http://localhost:9000/api/habilidades?q=cursed-body'
```

```json
[
  { "id": 130, "name": "cursed-body", "pokemonCount": 15,
    "shortEffect": "Has a 30% chance of Disabling any move that hits the Pokémon." }
]
```

---

## `GET /api/naturalezas`

```bash
curl 'http://localhost:9000/api/naturalezas?q=adamant'
```

```json
[
  { "id": 11, "name": "adamant",
    "increasedStat": "attack", "decreasedStat": "special-attack",
    "likesFlavor": "spicy", "hatesFlavor": "dry" }
]
```

> Los cuatro campos de stat/sabor son `null` en naturalezas neutras.

---

## `GET /api/objetos`

```bash
curl 'http://localhost:9000/api/objetos?q=potion'
```

```json
[
  { "id": 17, "name": "potion", "cost": 200, "category": "healing",
    "sprite": "https://.../items/potion.png", "shortEffect": "Restores 20 HP." }
]
```

---

## `GET /api/bayas`

```bash
curl 'http://localhost:9000/api/bayas?q=cheri'
```

```json
[
  { "id": 1, "name": "cheri", "growthTime": 3, "maxHarvest": 5,
    "naturalGiftPower": 60, "naturalGiftType": "fire", "smoothness": 25,
    "firmness": "soft",
    "flavors": [ { "flavor": "spicy", "potency": 10 } ] }
]
```

---

## Nota de idioma

PokéAPI devuelve los `effect_entries` en varios idiomas. Los readers de `Move`,
`Ability` e `Item` usan el helper `englishShortEffect` que **prioriza la entrada
en inglés** (con fallback a la primera disponible). Por eso `shortEffect` sale en
inglés y no, p.ej., en francés.

## Detalles de implementación

- **Acción:** cada controlador tiene `apiIndex(q)` junto a su `index` HTML.
  Reutiliza la misma lógica de búsqueda pero serializa con `.toJson` (writers de
  spray-json) en vez de renderizar Twirl.
- **Rutas:** definidas en [`conf/routes`](../conf/routes) bajo el prefijo `/api`.
- **Writers:** ver [`app/models/PokemonModels.scala`](../app/models/PokemonModels.scala)
  y [`app/models/GameModels.scala`](../app/models/GameModels.scala).
