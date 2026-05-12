# Pokédex Retro

Pokédex interactiva con estética retro inspirada en la Game Boy original (DMG-01) y los videojuegos de Pokémon de primera generación. Construida con Scala y Play Framework, consumiendo datos en tiempo real desde [PokéAPI](https://pokeapi.co).

---

## Capturas

> Tema normal · Tema DMG (Game Boy verde) · Tema MONO (gris)

---

## Características

- **7 secciones**: Pokédex, Movimientos, Tipos, Habilidades, Naturalezas, Objetos y Bayas
- **Buscador** en cada sección con filtrado server-side
- **3 temas visuales**: Normal (rojo retro), DMG (Game Boy verde LCD), MONO (gris monocromático)
- **Paginación** configurable por sección
- **Modal de detalle** por Pokémon con stats, tipos, habilidades y barras de progreso
- **Tabla de efectividad de tipos** completa con matriz interactiva
- **Acordeón de naturalezas** agrupado por stat afectado
- Sin JavaScript para la navegación principal — menú hamburguesa con CSS puro
- Diseño pixel art con fuentes **Press Start 2P** y **VT323**

---

## Stack técnico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Scala 3.4.3 |
| Framework web | Play Framework 3.0.6 |
| Plantillas | Twirl (`.scala.html`) |
| HTTP client | Play WS (WSClient) |
| JSON | spray-json 1.3.6 |
| Build tool | SBT 1.10.11 |
| Fuente de datos | [PokéAPI v2](https://pokeapi.co/api/v2/) |
| Estilos | CSS puro (sin frameworks) |

---

## Requisitos

- JDK 17+
- SBT 1.10.11

---

## Instalación y ejecución

```bash
# Clonar el repositorio
git clone https://github.com/RenzoRamosDEV/Api-Pokemon-Scala-Web.git
cd Api-Pokemon-Scala-Web

# Ejecutar en modo desarrollo
sbt run
```

La aplicación estará disponible en `http://localhost:9000`.

---

## Variables de entorno

| Variable | Descripción | Valor por defecto |
|----------|-------------|-------------------|
| `APPLICATION_SECRET` | Clave secreta de Play | `changeme-in-production-use-env-var` |
| `POKEAPI_BASE_URL` | URL base de PokéAPI | `https://pokeapi.co` |

---

## Estructura del proyecto

```
app/
├── controllers/          # Un controlador por sección
│   ├── PokedexController.scala
│   ├── MovesController.scala
│   ├── TypesController.scala
│   ├── AbilitiesController.scala
│   ├── NaturesController.scala
│   ├── ItemsController.scala
│   └── BerriesController.scala
├── models/
│   ├── PokemonModels.scala   # Case classes + JSON formats para Pokémon
│   └── GameModels.scala      # Case classes para el resto de entidades
└── views/
    ├── main.scala.html        # Layout principal con header, nav y selectores
    ├── index.scala.html       # Grid de Pokémon
    ├── _card.scala.html       # Tarjeta de Pokémon
    ├── _modal.scala.html      # Modal de detalle de Pokémon
    ├── moves.scala.html
    ├── types.scala.html
    ├── abilities.scala.html
    ├── natures.scala.html
    ├── items.scala.html
    └── berries.scala.html
conf/
├── routes                # Definición de rutas
└── application.conf      # Configuración de Play
public/
└── stylesheets/
    └── pokedex.css       # Todos los estilos + temas DMG y MONO
```

---

## Paginación por sección

| Sección | Elementos por página |
|---------|---------------------|
| Pokédex | 18 (3 filas × 6 col) |
| Movimientos | 16 (4 filas × 4 col) |
| Habilidades | 8 |
| Objetos | 12 |
| Bayas | 15 (3 filas × 5 col) |
| Tipos | Sin paginación |
| Naturalezas | Sin paginación |

---

## Temas visuales

| Tema | Botón | Descripción |
|------|-------|-------------|
| Normal | NORMAL | Rojo Pokédex clásico sobre fondo verde oscuro |
| DMG | DMG | Paleta monocromática verde LCD de la Game Boy original |
| MONO | MONO | Escala de grises inspirada en la Game Boy Pocket |

El tema elegido se guarda en `localStorage` y persiste entre páginas.

---

## Autor

**Renzo Ramos** — [GitHub](https://github.com/RenzoRamosDEV)
