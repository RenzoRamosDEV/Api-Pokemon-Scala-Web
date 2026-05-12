package controllers

import javax.inject.*
import play.api.mvc.*
import play.api.libs.ws.*
import play.api.Configuration
import scala.concurrent.*
import spray.json.*
import models.*
import models.PokemonModels.*

@Singleton
class PokedexController @Inject() (
  cc: ControllerComponents,
  ws: WSClient,
  config: Configuration
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val baseUrl  = config.get[String]("pokeapi.base-url")
  private val pageSize = 20

  def index(page: Int): Action[AnyContent] = Action.async { implicit request =>
    val offset = (page - 1) * pageSize
    ws.url(s"$baseUrl/api/v2/pokemon?limit=$pageSize&offset=$offset").get().flatMap { listResp =>
      val paginated  = listResp.body.parseJson.convertTo[PaginatedResponse]
      val totalPages = math.ceil(paginated.count.toDouble / pageSize).toInt
      val fetches    = paginated.results.map(r => fetchByName(r.name))
      Future.sequence(fetches).map { opts =>
        val pokemons = opts.flatten.sortBy(_.id)
        Ok(views.html.index(pokemons, page, totalPages))
      }
    }
  }

  private def fetchByName(name: String): Future[Option[Pokemon]] =
    ws.url(s"$baseUrl/api/v2/pokemon/$name").get().map { r =>
      if (r.status == 200) Some(r.body.parseJson.convertTo[Pokemon]) else None
    }
}
