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
class TypesController @Inject() (
  cc: ControllerComponents,
  ws: WSClient,
  config: Configuration
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val baseUrl = config.get[String]("pokeapi.base-url")

  def index(): Action[AnyContent] = Action.async { implicit request =>
    ws.url(s"$baseUrl/api/v2/type?limit=100").get().flatMap { listResp =>
      val paginated = listResp.body.parseJson.convertTo[PaginatedResponse]
      val fetches   = paginated.results.map(r => fetchType(r.name))
      Future.sequence(fetches).map { opts =>
        val types = opts.flatten
          .filterNot(t => t.name == "unknown" || t.name == "shadow")
          .sortBy(_.id)
        Ok(views.html.types(types))
      }
    }
  }

  private def fetchType(name: String): Future[Option[GameType]] =
    ws.url(s"$baseUrl/api/v2/type/$name").get().map { r =>
      if (r.status == 200) Some(r.body.parseJson.convertTo[GameType]) else None
    }
}
