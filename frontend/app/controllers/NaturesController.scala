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
class NaturesController @Inject() (
  cc: ControllerComponents,
  ws: WSClient,
  config: Configuration
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  private val baseUrl = config.get[String]("pokeapi.base-url")

  def index(): Action[AnyContent] = Action.async { implicit request =>
    ws.url(s"$baseUrl/api/v2/nature?limit=100").get().flatMap { listResp =>
      val paginated = listResp.body.parseJson.convertTo[PaginatedResponse]
      val fetches   = paginated.results.map(r => fetchNature(r.name))
      Future.sequence(fetches).map { opts =>
        val natures = opts.flatten.sortBy(_.id)
        Ok(views.html.natures(natures))
      }
    }
  }

  private def fetchNature(name: String): Future[Option[Nature]] =
    ws.url(s"$baseUrl/api/v2/nature/$name").get().map { r =>
      if (r.status == 200) Some(r.body.parseJson.convertTo[Nature]) else None
    }
}
