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
  private val pageSize = 8

  def index(page: Int, q: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    q.map(_.trim).filter(_.nonEmpty) match {
      case Some(query) =>
        ws.url(s"$baseUrl/api/v2/ability?limit=100000").get().flatMap { listResp =>
          val all      = listResp.body[String].parseJson.convertTo[PaginatedResponse]
          val filtered = all.results.filter(_.name.contains(query.toLowerCase))
          val slice    = filtered.slice((page - 1) * pageSize, page * pageSize)
          val total    = math.ceil(filtered.size.toDouble / pageSize).toInt
          Future.sequence(slice.map(r => fetchAbility(r.name))).map { opts =>
            Ok(views.html.abilities(opts.flatten.sortBy(_.id), page, total, q))
          }
        }
      case None =>
        val offset = (page - 1) * pageSize
        ws.url(s"$baseUrl/api/v2/ability?limit=$pageSize&offset=$offset").get().flatMap { listResp =>
          val paginated  = listResp.body[String].parseJson.convertTo[PaginatedResponse]
          val totalPages = math.ceil(paginated.count.toDouble / pageSize).toInt
          Future.sequence(paginated.results.map(r => fetchAbility(r.name))).map { opts =>
            Ok(views.html.abilities(opts.flatten.sortBy(_.id), page, totalPages, None))
          }
        }
    }
  }

  private def fetchAbility(name: String): Future[Option[Ability]] =
    ws.url(s"$baseUrl/api/v2/ability/$name").get().map { r =>
      if (r.status == 200) Some(r.body[String].parseJson.convertTo[Ability]) else None
    }
}
