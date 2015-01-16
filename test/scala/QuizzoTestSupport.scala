package scala

import java.net.URI
import java.util.UUID

import akka.actor.Actor._
import akka.actor._
import akka.pattern.ask
import model.Games
import org.apache.commons.codec.digest.DigestUtils
import play.api.Logger
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers._
import spray.http.HttpHeaders.RawHeader
import spray.http._

import scala.QuizzoModerator.{StartGame, Login}
import scala.QuizzoPlayer.Join
import scala.util.{Failure, Success}

object QuizzoTestSupport extends QuizzoTestSupport

trait QuizzoTestSupport {
  type SessionId = String
  type GameId = String
  type GameInstanceId = String

  val PlayerOne = "player1"
  val PlayerOneEmail = "player1@gmail.com"
  val PlayerOneJoin = (gameId: String) => Join(gameId, PlayerOne, PlayerOneEmail)

  def json(result: Result) =
    Json.parse(new String(await(result.body |>>> Iteratee.consume[Array[Byte]]())))

}
abstract class Test {
  implicit val testSystem = ActorSystem(UUID.randomUUID().toString)
  implicit val ec = testSystem.dispatcher
}

case object TestSuccess




object QuizzoModerator {
  import scala.QuizzoTestSupport._
  case class Login(user: String = "ken", pwd: String = "ken")
  case class StartGame(gameId: GameId = Games.ScalaQuiz.id)
  case class NextQuestion(gameInstanceId: GameInstanceId)
  case class GetQuestion(gameInstanceId: GameInstanceId)
  case object Successful
}

class QuizzoModerator(port: Int) extends Actor {
  import spray.client.pipelining._
  import context.dispatcher
  import scala.QuizzoModerator._
  import scala.QuizzoTestSupport._

  val baseUrl: String = s"http://localhost:$port/api/v1.0/moderator/games"

  override def receive = {
    case Login(user, pwd) =>
      val requester = sender
      val loginReq = sendReceive
      loginReq(Post(s"http://localhost:$port/login").withEntity(HttpEntity(ContentTypes.`application/json`, Json.stringify(Json.obj("user" -> user, "password" -> DigestUtils.md5Hex(pwd)))))).onComplete {
    case Success(hr) if hr.status.intValue == 200 =>
      val sid = (Json.parse(hr.entity.asString) \ "id").as[String]
      requester ! sid
      context.become(new LoggedIn(sid).receive)
    case other => throw new RuntimeException(s"failed to login : $other")
  }
  }

  class LoggedIn(val sid: SessionId) {
    def receive: Receive = {
      case StartGame(gameId) =>
        val requester = sender
        val startReq = sendReceive
        Logger.debug(s"Attempt to start $gameId with session $sid")
        startReq(
        Post(s"$baseUrl/$gameId").withEntity(HttpEntity(ContentTypes.`application/json`,
          Json.stringify(Json.obj("command" -> "start")))).withHeaders(RawHeader("session", sid))).onComplete {
          case Success(hr) if hr.status.isSuccess =>
            val gameId = hr.entity.asString
            requester ! gameId
            context.become(new InGame(sid, hr.entity.asString).receive)
          case other => throw new RuntimeException(s"failed to start game: $other")
        }

    }
  }
  class InGame(sid: SessionId, gameId: GameInstanceId) {
    def receive: Receive = {
      case 'GetSession =>
        sender ! sid
      case NextQuestion(gameId) =>
        val requester = sender
        val nqReq = sendReceive
        nqReq(Post(s"$baseUrl/$gameId").withEntity(HttpEntity(ContentTypes.`application/json`, Json.stringify(Json.obj("command" -> "nextQuestion")))).withHeaders(RawHeader("session", sid))).onComplete {
          case Success(hr) if hr.status.isSuccess =>
            requester ! Successful
          case other => throw new RuntimeException(s"failed to go to next question: $other")
        }
      case GetQuestion(gameId) =>
        val requester = sender
        val qReq = sendReceive
        qReq(Post(s"$baseUrl/$gameId").withHeaders(RawHeader("session", sid))).onComplete {
          case Success(hr) if hr.status.isSuccess =>
            requester ! hr.entity.asString
          case other => throw new RuntimeException(s"failed to go to next question: $other")
        }
    }
  }
}

object QuizzoPlayer {
  import scala.QuizzoTestSupport._
  case class Join(gameId: GameInstanceId, nickname: String, email: String)
  case class PlayerJoined(response: JsValue)
  case class QuestionOpen(question: JsValue)
  case class ReviewingQuestion(question: JsValue)
  case class GameOver(scores: JsValue)
  case class AnswerQuestion(answer: String)
  case object ConnectionClosed
}

class QuizzoPlayer(port: Int) extends Actor with ActorLogging {
  import spray.client.pipelining._
  import context.dispatcher
  import scala.QuizzoPlayer._
  import scala.WebSocketClient.Messages._

  val baseUrl: String = s"http://localhost:$port/api/v1.0/player/games"

  override def receive = {
    case Join(gameId, nickname, email) =>
      val joinReq = sendReceive
      val listener = sender()
      Logger.debug(s"listener is $listener")
      joinReq(Post(s"$baseUrl/$gameId").withEntity(HttpEntity(ContentTypes.`application/json`, Json.stringify(Json.obj("command" -> "join", "nickname" -> nickname, "email" -> email))))).onComplete {
        case Success(resp) if resp.status == StatusCodes.OK =>
          val psid = (Json.parse(resp.entity.asString) \ "id").as[String]
          context.watch(listener)
          val wsc = WebSocketClient(new URI(s"ws://localhost:$port/api/v1.0/player/games/ws/$gameId")) {
            case msg => self ! msg
          }
          context.become(joined(wsc, psid, listener))
          wsc.connect
        case Success(resp) => throw new RuntimeException(s"failed to join game $gameId: $resp")
        case Failure(t) => throw new RuntimeException(s"failed to join game $gameId: $t")
      }
  }

  // This is so we can override the default behavior
  def playerSession(sessionId: String) = sessionId

  def joined(webSocket: WebSocketClient, playerSessionId: String, listener: ActorRef): Receive = {
    case Connecting => log.debug("web socket connecting...")
    case Disconnected(client, reason) =>
      Logger.info(s"Web socket disconnected: $reason")
      listener ! ConnectionClosed
    case Disconnecting => log.debug("web socket disconnecting...")
    case ConnectionFailed(client, reason) => Logger.error(s"Web socket failed to connect $reason")
    case Connected(client) =>
      Logger.info(s"Web socket connected to ${client.url.toString}")
      client.send(Json.stringify(Json.obj("sessionId" -> playerSession(playerSessionId))))
      listener ! 'JoinSuccessful
    case TextMessage(client, msg) =>
      val json = Json.parse(msg)
      (json \ "messageType").as[String] match {
        case "playerJoined" => listener ! PlayerJoined
        case "questionOpen" => listener ! QuestionOpen(json)
        case "reviewingQuestion" => listener ! ReviewingQuestion(json)
        case "gameOver" => listener ! GameOver(json)
        case msg => Logger.warn(s"Unknown message from server $msg")
      }
    case AnswerQuestion(answer) =>
      webSocket.send(Json.stringify(Json.obj("command" -> "answerQuestion", "answer" -> answer)))
    case WriteFailed(client, msg, reason) => Logger.error(s"Web socket write $msg failed $reason")
    case Error(client, th) => Logger.error(s"Unexpected web socket error $th")
    case Terminated(`listener`) =>
      webSocket.disconnect
      self ! PoisonPill
  }

}



