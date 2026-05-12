package models

import spray.json.*

case class NamedResource(name: String, url: String)
case class PaginatedResponse(count: Int, results: List[NamedResource])
case class Sprites(frontDefault: Option[String])
case class PokemonType(slot: Int, typeName: String)
case class PokemonAbility(name: String, isHidden: Boolean, slot: Int)
case class PokemonStat(name: String, baseStat: Int, effort: Int)
case class Pokemon(
  id: Int,
  name: String,
  baseExperience: Option[Int],
  height: Int,
  weight: Int,
  abilities: List[PokemonAbility],
  stats: List[PokemonStat],
  types: List[PokemonType],
  sprites: Sprites
)

object PokemonModels extends DefaultJsonProtocol {
  implicit val namedResourceFmt: RootJsonFormat[NamedResource] = jsonFormat2(NamedResource.apply)
  implicit val paginatedFmt: RootJsonFormat[PaginatedResponse]  = jsonFormat2(PaginatedResponse.apply)

  implicit val spritesFmt: RootJsonFormat[Sprites] = new RootJsonFormat[Sprites] {
    def write(s: Sprites): JsValue = JsObject("front_default" -> s.frontDefault.toJson)
    def read(v: JsValue): Sprites =
      Sprites(v.asJsObject.fields.get("front_default").collect { case JsString(s) => s })
  }

  implicit val pokemonTypeFmt: RootJsonFormat[PokemonType] = new RootJsonFormat[PokemonType] {
    def write(t: PokemonType): JsValue = JsObject(
      "slot" -> JsNumber(t.slot),
      "type" -> JsObject("name" -> JsString(t.typeName))
    )
    def read(v: JsValue): PokemonType = {
      val obj = v.asJsObject.fields
      PokemonType(
        slot = obj("slot").convertTo[Int],
        typeName = obj("type").asJsObject.fields("name").convertTo[String]
      )
    }
  }

  implicit val pokemonAbilityFmt: RootJsonFormat[PokemonAbility] = new RootJsonFormat[PokemonAbility] {
    def write(a: PokemonAbility): JsValue = JsObject(
      "ability"   -> JsObject("name" -> JsString(a.name)),
      "is_hidden" -> JsBoolean(a.isHidden),
      "slot"      -> JsNumber(a.slot)
    )
    def read(v: JsValue): PokemonAbility = {
      val obj = v.asJsObject.fields
      PokemonAbility(
        name = obj("ability").asJsObject.fields("name").convertTo[String],
        isHidden = obj("is_hidden").convertTo[Boolean],
        slot = obj("slot").convertTo[Int]
      )
    }
  }

  implicit val pokemonStatFmt: RootJsonFormat[PokemonStat] = new RootJsonFormat[PokemonStat] {
    def write(s: PokemonStat): JsValue = JsObject(
      "stat"      -> JsObject("name" -> JsString(s.name)),
      "base_stat" -> JsNumber(s.baseStat),
      "effort"    -> JsNumber(s.effort)
    )
    def read(v: JsValue): PokemonStat = {
      val obj = v.asJsObject.fields
      PokemonStat(
        name     = obj("stat").asJsObject.fields("name").convertTo[String],
        baseStat = obj("base_stat").convertTo[Int],
        effort   = obj("effort").convertTo[Int]
      )
    }
  }

  implicit val pokemonFmt: RootJsonFormat[Pokemon] = new RootJsonFormat[Pokemon] {
    def write(p: Pokemon): JsValue = JsObject()
    def read(v: JsValue): Pokemon = {
      val obj = v.asJsObject.fields
      Pokemon(
        id             = obj("id").convertTo[Int],
        name           = obj("name").convertTo[String],
        baseExperience = obj.get("base_experience").collect { case JsNumber(n) => n.toInt },
        height         = obj("height").convertTo[Int],
        weight         = obj("weight").convertTo[Int],
        abilities      = obj("abilities").convertTo[List[PokemonAbility]],
        stats          = obj("stats").convertTo[List[PokemonStat]],
        types          = obj("types").convertTo[List[PokemonType]],
        sprites        = obj("sprites").convertTo[Sprites]
      )
    }
  }
}
