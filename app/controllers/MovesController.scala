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
class MovesController @Inject() (
  cc: ControllerComponents,
  ws: WSClient,
  config: Configuration
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val baseUrl  = config.get[String]("pokeapi.base-url")
  private val pageSize = 16

  def index(page: Int): Action[AnyContent] = Action.async { implicit request =>
    val offset = (page - 1) * pageSize
    ws.url(s"$baseUrl/api/v2/move?limit=$pageSize&offset=$offset").get().flatMap { listResp =>
      val paginated  = listResp.body[String].parseJson.convertTo[PaginatedResponse]
      val totalPages = math.ceil(paginated.count.toDouble / pageSize).toInt
      val fetches    = paginated.results.map(r => fetchMove(r.name))
      Future.sequence(fetches).map { opts =>
        val moves = opts.flatten.sortBy(_.id)
        Ok(views.html.moves(moves, page, totalPages))
      }
    }
  }

  private def fetchMove(name: String): Future[Option[Move]] =
    ws.url(s"$baseUrl/api/v2/move/$name").get().map { r =>
      if (r.status == 200) Some(r.body[String].parseJson.convertTo[Move]) else None
    }
}
