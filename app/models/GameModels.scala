package models

import spray.json.*

case class Move(
  id: Int,
  name: String,
  accuracy: Option[Int],
  pp: Int,
  priority: Int,
  power: Option[Int],
  damageClass: String,
  moveType: String,
  shortEffect: String
)

case class Ability(
  id: Int,
  name: String,
  shortEffect: String,
  pokemonCount: Int
)

case class Nature(
  id: Int,
  name: String,
  decreasedStat: Option[String],
  increasedStat: Option[String],
  hatesFlavor: Option[String],
  likesFlavor: Option[String]
)

case class Item(
  id: Int,
  name: String,
  cost: Int,
  category: String,
  sprite: Option[String],
  shortEffect: String
)

case class BerryFlavorMap(potency: Int, flavor: String)

case class Berry(
  id: Int,
  name: String,
  growthTime: Int,
  maxHarvest: Int,
  naturalGiftPower: Int,
  naturalGiftType: String,
  smoothness: Int,
  firmness: String,
  flavors: List[BerryFlavorMap]
)

case class GameType(
  id: Int,
  name: String,
  doubleDamageTo: List[String],
  halfDamageTo: List[String],
  noDamageTo: List[String],
  doubleDamageFrom: List[String],
  halfDamageFrom: List[String],
  noDamageFrom: List[String],
  pokemonCount: Int
)

// Formatos spray-json del resto de entidades del juego (Move, Ability, Nature,
// Item, Berry, GameType). Igual que PokemonModels: `read` parsea PokéAPI y
// `write` emite el JSON plano de /api/* para el agente MCP.
object GameModels extends DefaultJsonProtocol {
  import PokemonModels.namedResourceFmt

  // Aplana una relación de NamedResource a una lista de nombres.
  // Ej: damage_relations.double_damage_to -> ["water","ground","rock"]
  private def namedList(v: JsValue, key: String): List[String] =
    v.asJsObject.fields
      .get(key)
      .toList
      .flatMap(_.convertTo[List[NamedResource]])
      .map(_.name)

  /** Extrae el `short_effect` en inglés de `effect_entries`. PokéAPI devuelve
    * entradas en varios idiomas; preferimos la inglesa y, si no existe, la
    * primera disponible. */
  private def englishShortEffect(fields: Map[String, JsValue]): String = {
    val entries = fields.get("effect_entries").toList.flatMap(_.convertTo[List[JsValue]])
    def isEnglish(e: JsValue): Boolean =
      e.asJsObject.fields.get("language")
        .flatMap(_.asJsObject.fields.get("name"))
        .contains(JsString("en"))
    entries.find(isEnglish).orElse(entries.headOption)
      .flatMap(_.asJsObject.fields.get("short_effect"))
      .collect { case JsString(s) => s }
      .getOrElse("")
  }

  implicit val moveFmt: RootJsonFormat[Move] = new RootJsonFormat[Move] {
    def write(m: Move): JsValue = JsObject(
      "id"          -> JsNumber(m.id),
      "name"        -> JsString(m.name),
      "power"       -> m.power.toJson,
      "accuracy"    -> m.accuracy.toJson,
      "pp"          -> JsNumber(m.pp),
      "priority"    -> JsNumber(m.priority),
      "damageClass" -> JsString(m.damageClass),
      "moveType"    -> JsString(m.moveType),
      "shortEffect" -> JsString(m.shortEffect)
    )
    def read(v: JsValue): Move = {
      val obj = v.asJsObject.fields
      Move(
        id          = obj("id").convertTo[Int],
        name        = obj("name").convertTo[String],
        accuracy    = obj.get("accuracy").collect { case JsNumber(n) => n.toInt },
        pp          = obj("pp").convertTo[Int],
        priority    = obj("priority").convertTo[Int],
        power       = obj.get("power").collect { case JsNumber(n) => n.toInt },
        damageClass = obj("damage_class").asJsObject.fields("name").convertTo[String],
        moveType    = obj("type").asJsObject.fields("name").convertTo[String],
        shortEffect = englishShortEffect(obj)
      )
    }
  }

  implicit val abilityFmt: RootJsonFormat[Ability] = new RootJsonFormat[Ability] {
    def write(a: Ability): JsValue = JsObject(
      "id"           -> JsNumber(a.id),
      "name"         -> JsString(a.name),
      "shortEffect"  -> JsString(a.shortEffect),
      "pokemonCount" -> JsNumber(a.pokemonCount)
    )
    def read(v: JsValue): Ability = {
      val obj = v.asJsObject.fields
      Ability(
        id           = obj("id").convertTo[Int],
        name         = obj("name").convertTo[String],
        shortEffect  = englishShortEffect(obj),
        pokemonCount = obj.get("pokemon").map(_.convertTo[List[JsValue]].length).getOrElse(0)
      )
    }
  }

  implicit val natureFmt: RootJsonFormat[Nature] = new RootJsonFormat[Nature] {
    def write(n: Nature): JsValue = JsObject(
      "id"            -> JsNumber(n.id),
      "name"          -> JsString(n.name),
      "increasedStat" -> n.increasedStat.toJson,
      "decreasedStat" -> n.decreasedStat.toJson,
      "hatesFlavor"   -> n.hatesFlavor.toJson,
      "likesFlavor"   -> n.likesFlavor.toJson
    )
    def read(v: JsValue): Nature = {
      val obj = v.asJsObject.fields
      def optName(key: String): Option[String] =
        obj.get(key).collect { case o: JsObject => o.fields.get("name").collect { case JsString(s) => s } }.flatten
      Nature(
        id            = obj("id").convertTo[Int],
        name          = obj("name").convertTo[String],
        decreasedStat = optName("decreased_stat"),
        increasedStat = optName("increased_stat"),
        hatesFlavor   = optName("hates_flavor"),
        likesFlavor   = optName("likes_flavor")
      )
    }
  }

  implicit val itemFmt: RootJsonFormat[Item] = new RootJsonFormat[Item] {
    def write(i: Item): JsValue = JsObject(
      "id"          -> JsNumber(i.id),
      "name"        -> JsString(i.name),
      "cost"        -> JsNumber(i.cost),
      "category"    -> JsString(i.category),
      "sprite"      -> i.sprite.toJson,
      "shortEffect" -> JsString(i.shortEffect)
    )
    def read(v: JsValue): Item = {
      val obj = v.asJsObject.fields
      Item(
        id          = obj("id").convertTo[Int],
        name        = obj("name").convertTo[String],
        cost        = obj("cost").convertTo[Int],
        category    = obj("category").asJsObject.fields("name").convertTo[String],
        sprite      = obj.get("sprites").flatMap(_.asJsObject.fields.get("default")).collect { case JsString(s) => s },
        shortEffect = englishShortEffect(obj)
      )
    }
  }

  implicit val berryFlavorFmt: RootJsonFormat[BerryFlavorMap] = new RootJsonFormat[BerryFlavorMap] {
    def write(b: BerryFlavorMap): JsValue = JsObject(
      "flavor"  -> JsString(b.flavor),
      "potency" -> JsNumber(b.potency)
    )
    def read(v: JsValue): BerryFlavorMap = {
      val obj = v.asJsObject.fields
      BerryFlavorMap(
        potency = obj("potency").convertTo[Int],
        flavor  = obj("flavor").asJsObject.fields("name").convertTo[String]
      )
    }
  }

  implicit val berryFmt: RootJsonFormat[Berry] = new RootJsonFormat[Berry] {
    def write(b: Berry): JsValue = JsObject(
      "id"               -> JsNumber(b.id),
      "name"             -> JsString(b.name),
      "growthTime"       -> JsNumber(b.growthTime),
      "maxHarvest"       -> JsNumber(b.maxHarvest),
      "naturalGiftPower" -> JsNumber(b.naturalGiftPower),
      "naturalGiftType"  -> JsString(b.naturalGiftType),
      "smoothness"       -> JsNumber(b.smoothness),
      "firmness"         -> JsString(b.firmness),
      "flavors"          -> b.flavors.toJson
    )
    def read(v: JsValue): Berry = {
      val obj = v.asJsObject.fields
      Berry(
        id               = obj("id").convertTo[Int],
        name             = obj("name").convertTo[String],
        growthTime       = obj("growth_time").convertTo[Int],
        maxHarvest       = obj("max_harvest").convertTo[Int],
        naturalGiftPower = obj("natural_gift_power").convertTo[Int],
        naturalGiftType  = obj("natural_gift_type").asJsObject.fields("name").convertTo[String],
        smoothness       = obj("smoothness").convertTo[Int],
        firmness         = obj("firmness").asJsObject.fields("name").convertTo[String],
        flavors          = obj("flavors").convertTo[List[BerryFlavorMap]]
      )
    }
  }

  implicit val gameTypeFmt: RootJsonFormat[GameType] = new RootJsonFormat[GameType] {
    def write(t: GameType): JsValue = JsObject(
      "id"               -> JsNumber(t.id),
      "name"             -> JsString(t.name),
      "doubleDamageTo"   -> t.doubleDamageTo.toJson,
      "halfDamageTo"     -> t.halfDamageTo.toJson,
      "noDamageTo"       -> t.noDamageTo.toJson,
      "doubleDamageFrom" -> t.doubleDamageFrom.toJson,
      "halfDamageFrom"   -> t.halfDamageFrom.toJson,
      "noDamageFrom"     -> t.noDamageFrom.toJson,
      "pokemonCount"     -> JsNumber(t.pokemonCount)
    )
    def read(v: JsValue): GameType = {
      val obj = v.asJsObject.fields
      val dmg = obj("damage_relations").asJsObject
      GameType(
        id              = obj("id").convertTo[Int],
        name            = obj("name").convertTo[String],
        doubleDamageTo  = namedList(dmg, "double_damage_to"),
        halfDamageTo    = namedList(dmg, "half_damage_to"),
        noDamageTo      = namedList(dmg, "no_damage_to"),
        doubleDamageFrom = namedList(dmg, "double_damage_from"),
        halfDamageFrom  = namedList(dmg, "half_damage_from"),
        noDamageFrom    = namedList(dmg, "no_damage_from"),
        pokemonCount    = obj.get("pokemon").map(_.convertTo[List[JsValue]].length).getOrElse(0)
      )
    }
  }
}
