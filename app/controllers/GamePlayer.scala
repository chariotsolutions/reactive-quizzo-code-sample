package controllers

import actors.{GameActor, GamesActor, SessionActor}
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import model.{GameInstance, GameQuestion, UserSession}
import play.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.{Action, Controller, Result, WebSocket}

import scala.concurrent.Future
import scala.concurrent.duration._

object GamePlayer extends Controller {
  implicit val ec = Akka.system.dispatcher
  implicit val timeout = Timeout(5, SECONDS)

  def games = Action.async { req =>
    gamesActor.ask(GamesActor.GetGames).mapTo[Set[GameInstance]].map {
      case s: Set[GameInstance] =>
        Ok(Json.toJson(s.map(gi => Json.obj("id" -> JsString(gi.id), "title" -> JsString(gi.game.title), "description" -> JsString(gi.game.description)))))
      case _ => BadRequest
    }
  }

  def gameAction(id: String) = Action.async(parse.json) { req =>
    req.body \ "command" match {
      case JsString("join") =>
        (req.body \ "nickname", req.body \ "email") match {
          case (JsString(nickname), JsString(email)) =>
            joinGame(id, nickname, email)
          case _ => Future.successful(BadRequest)
        }
      case _ => Future.successful(BadRequest("Unknown command"))
    }
  }

  def joinGame(id: String, nickname: String, email: String): Future[Result] = {
    for {
      session <- sessionActor.ask(SessionActor.CreatePlayerSession(nickname, email)).mapTo[Option[UserSession]]
      joinResp <- gameActor(id).ask(GameActor.RegisterPlayer(nickname)).recover({
        case t => Logger.error("join error", t)
      })
    } yield joinResp match {
      case GameActor.PlayerJoined =>
        Ok(Json.obj("id" -> session.get.id))
      case msg =>
        Logger.debug(s"Unexpected response to JoinGame: $msg")
        ExpectationFailed
    }
  }

  def webSocket(gameId: String) = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    PlayerWebSocketActor.props(out, gameId)
  }

}

object PlayerWebSocketActor {
  def props(out: ActorRef, gameId: String) = Props(new PlayerWebSocketActor(out, gameId))
}

class PlayerWebSocketActor(out: ActorRef, gameId: String) extends Actor with ActorLogging {
  implicit val ec = Akka.system.dispatcher
  implicit val timeout = Timeout(5, SECONDS)

  var session: Option[UserSession] = None

  gameActor(gameId) ! GameActor.AddEventSubscriber(self)

  def receive = {
    case JsObject(Seq(("sessionId", JsString(sessionId)))) =>
      val ctx = context
      sessionActor ? SessionActor.LookupSession(sessionId) onSuccess {
        case Some(s: UserSession) =>
          session = Some(s)
          ctx.become(authenticated)
        case o =>
          Logger.error("No session, closing socket")
          self ! PoisonPill
      }
  }

  def authenticated: Receive = {
    case msg: JsObject if msg.keys.contains("command") => (msg \ "command").as[String] match {
      case "answerQuestion" =>
        val JsString(answer) = msg \ "answer"
        gameActor(gameId) ? GameActor.AnswerQuestion(session.get.userId, answer) recover {
          case t => Logger.error(s"Error answering question $t")
        }
      case other => Logger.warn(s"Unknown command $other")
    }
    case GameActor.PlayerJoinedEvent(game) =>
      out ! message("playerJoined", Json.obj("players" -> playerObject(game.players)))
    case GameActor.QuestionOpenEvent(currentQuestion, game) =>
      out ! message("questionOpen", questionWithoutPoints(currentQuestion))
    case GameActor.ReviewingQuestionEvent(currentQuestion, game) =>
      out ! message("reviewingQuestion", Json.obj("question" -> Json.toJson(currentQuestion), "playerScores" -> playerObject(game.players)))
    case GameActor.GameOverEvent(game) =>
      out ! message("gameOver", Json.obj("playerScores" -> playerObject(game.players)))
      gameActor(gameId) ! GameActor.RemoveEventSubscriber(self)
      self ! PoisonPill
  }

  def message(msgType: String, payload: JsValue) = Json.obj("messageType" -> msgType, "payload" -> payload)

  def playerObject(players: Map[String, Int]) = players.map { p =>
    Json.obj("player" -> p._1, "score" -> p._2)
  }

  def questionWithoutPoints(question: GameQuestion) = Json.obj("question" -> question.question, "answers" ->
    question.answers.map(a => Json.obj("id" -> a.id, "answer" -> a.answer)))

  override def unhandled(message: Any) = message match {
    case msg =>
      Logger.debug(s"Ignoring message $msg")
      super.unhandled(msg)
  }
}
