package controllers

import play.api.GlobalSettings
import play.api.http.HeaderNames
import play.api.mvc._

import scala.concurrent.Future

trait CorsImplicits {
  implicit def resultWithCorsHeaders(result: Result) = new Result(result.header, result.body, result.connection) with CorsHeaders
}

object CorsImplicits extends CorsImplicits

trait CorsHeaders extends HeaderNames {
  self: Result =>

  val AllowedMethods = "POST, GET"
  val AllowedHeaders = "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent, session"

  def withCorsHeaders(allowedOrigin: String) = self.withHeaders(ACCESS_CONTROL_ALLOW_ORIGIN -> allowedOrigin,
    ACCESS_CONTROL_ALLOW_METHODS -> AllowedMethods,
    ACCESS_CONTROL_ALLOW_HEADERS -> AllowedHeaders)
}

object CorsController extends Controller with GlobalSettings {
  import CorsImplicits._
  def preflight(path: String) = Action {
    Ok.withCorsHeaders(configuration.getString("cors.allowed-origin").getOrElse("*"))
  }
}

class CorsFilter(allowedOrigin: String) extends Filter with HeaderNames {
  import CorsImplicits._
  import scala.concurrent.ExecutionContext.Implicits.global
  def apply(nextFilter: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map(result =>
      result.withCorsHeaders(allowedOrigin)
    )
  }
}
