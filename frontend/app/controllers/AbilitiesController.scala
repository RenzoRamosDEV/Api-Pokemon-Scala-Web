package controllers

import javax.inject.*
import play.api.mvc.*
import play.api.libs.ws.*
import play.api.Configuration
import scala.concurrent.*
import spray.json.*
import models.*
import models.PokemonModels.*
import models.GameModels.*

@Singleton
class AbilitiesController @Inject() (
  cc: ControllerComponents,
  ws: WSClient,
  config: Configuration
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val baseUrl  = config.get[String]("pokeapi.base-url")
  private val pageSize = 20

  def index(page: Int): Action[AnyContent] = Action.async { implicit request =>
    val offset = (page - 1) * pageSize
    ws.url(s"$baseUrl/api/v2/ability?limit=$pageSize&offset=$offset").get().flatMap { listResp =>
      val paginated  = listResp.body.parseJson.convertTo[PaginatedResponse]
      val totalPages = math.ceil(paginated.count.toDouble / pageSize).toInt
      val fetches    = paginated.results.map(r => fetchAbility(r.name))
      Future.sequence(fetches).map { opts =>
        val abilities = opts.flatten.sortBy(_.id)
        Ok(views.html.abilities(abilities, page, totalPages))
      }
    }
  }

  private def fetchAbility(name: String): Future[Option[Ability]] =
    ws.url(s"$baseUrl/api/v2/ability/$name").get().map { r =>
      if (r.status == 200) Some(r.body.parseJson.convertTo[Ability]) else None
    }
}
