package controllers

import actors.SessionActor
import akka.pattern.ask
import akka.util.Timeout
import model.UserSession
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.Play.current
import scala.concurrent.Future
import scala.concurrent.duration._

case class LoginRequest(user: String, password: String)

object Application extends Controller {
  implicit val loginRequestFormat: Format[LoginRequest] = format[LoginRequest]
  implicit val timeout = Timeout(5, SECONDS)

  def login = Action.async(BodyParsers.parse.json) { request =>
    println("in the login action")

    implicit val ec = Akka.system.dispatcher
    val loginMaybe = request.body.validate[LoginRequest]
    loginMaybe.map( loginReq => {
      println(loginReq)
      Akka.system.actorSelection("user/session-actor").ask(SessionActor.LoginRequest(loginReq.user, loginReq.password)).map {
        case Some(session: UserSession) => Ok(toJson(session))
        case _ => Forbidden
      }
    }).getOrElse(Future { BadRequest })
  }
}
