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
  private val pageSize = 18
  private val apiLimit = 60

  def index(page: Int, q: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    q.map(_.trim).filter(_.nonEmpty) match {
      case Some(query) =>
        ws.url(s"$baseUrl/api/v2/pokemon?limit=100000").get().flatMap { listResp =>
          val all      = listResp.body[String].parseJson.convertTo[PaginatedResponse]
          val filtered = all.results.filter(_.name.contains(query.toLowerCase.replace(" ", "-")))
          val slice    = filtered.slice((page - 1) * pageSize, page * pageSize)
          val total    = math.ceil(filtered.size.toDouble / pageSize).toInt
          Future.sequence(slice.map(r => fetchByName(r.name))).map { opts =>
            Ok(views.html.index(opts.flatten.sortBy(_.id), page, total, q))
          }
        }
      case None =>
        val offset = (page - 1) * pageSize
        ws.url(s"$baseUrl/api/v2/pokemon?limit=$pageSize&offset=$offset").get().flatMap { listResp =>
          val paginated  = listResp.body[String].parseJson.convertTo[PaginatedResponse]
          val totalPages = math.ceil(paginated.count.toDouble / pageSize).toInt
          Future.sequence(paginated.results.map(r => fetchByName(r.name))).map { opts =>
            Ok(views.html.index(opts.flatten.sortBy(_.id), page, totalPages, None))
          }
        }
    }
  }

  // Acción JSON para el agente MCP (GET /api/pokemon?q=). A diferencia de `index`
  // (que renderiza HTML y pagina), aquí:
  //   1) traemos la lista completa de nombres de PokéAPI,
  //   2) filtramos por substring (normalizando espacio->guion, p.ej. "mr mime"),
  //   3) limitamos a `apiLimit` para no disparar miles de fetch de detalle,
  //   4) pedimos el detalle de cada uno y lo serializamos con el writer (.toJson).
  def apiIndex(q: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    ws.url(s"$baseUrl/api/v2/pokemon?limit=100000").get().flatMap { listResp =>
      val all      = listResp.body[String].parseJson.convertTo[PaginatedResponse]
      val filtered = (q.map(_.trim).filter(_.nonEmpty) match {
        case Some(query) => all.results.filter(_.name.contains(query.toLowerCase.replace(" ", "-")))
        case None        => all.results
      }).take(apiLimit)
      Future.sequence(filtered.map(r => fetchByName(r.name))).map { opts =>
        Ok(opts.flatten.sortBy(_.id).toJson.compactPrint).as("application/json")
      }
    }
  }

  private def fetchByName(name: String): Future[Option[Pokemon]] =
    ws.url(s"$baseUrl/api/v2/pokemon/$name").get().map { r =>
      if (r.status == 200) Some(r.body[String].parseJson.convertTo[Pokemon]) else None
    }
}
