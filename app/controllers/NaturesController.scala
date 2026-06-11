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

  def index(q: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    ws.url(s"$baseUrl/api/v2/nature?limit=100").get().flatMap { listResp =>
      val all      = listResp.body[String].parseJson.convertTo[PaginatedResponse]
      val filtered = q.map(_.trim).filter(_.nonEmpty) match {
        case Some(query) => all.results.filter(_.name.contains(query.toLowerCase.replace(" ", "-")))
        case None        => all.results
      }
      Future.sequence(filtered.map(r => fetchNature(r.name))).map { opts =>
        val natures = opts.flatten.sortBy(_.id)
        Ok(views.html.natures(natures, q))
      }
    }
  }

  // Acción JSON para el agente MCP (sin paginación; el catálogo es pequeño).
  // Filtra por substring (espacio->guion) y serializa con el writer (.toJson).
  def apiIndex(q: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    ws.url(s"$baseUrl/api/v2/nature?limit=100").get().flatMap { listResp =>
      val all      = listResp.body[String].parseJson.convertTo[PaginatedResponse]
      val filtered = q.map(_.trim).filter(_.nonEmpty) match {
        case Some(query) => all.results.filter(_.name.contains(query.toLowerCase.replace(" ", "-")))
        case None        => all.results
      }
      Future.sequence(filtered.map(r => fetchNature(r.name))).map { opts =>
        Ok(opts.flatten.sortBy(_.id).toJson.compactPrint).as("application/json")
      }
    }
  }

  private def fetchNature(name: String): Future[Option[Nature]] =
    ws.url(s"$baseUrl/api/v2/nature/$name").get().map { r =>
      if (r.status == 200) Some(r.body[String].parseJson.convertTo[Nature]) else None
    }
}
