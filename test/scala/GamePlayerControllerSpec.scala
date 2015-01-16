package scala

import java.util.UUID

import akka.actor.Actor.Receive
import akka.actor._
import akka.pattern.ask
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, MustMatchers}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Logger
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{JsArray, Json}
import play.api.test.{Helpers, FakeRequest}
import play.api.test.Helpers._

import scala.QuizzoModerator.{StartGame, Login, NextQuestion}
import scala.QuizzoPlayer._

class GamePlayerControllerSpec extends PlaySpec with OneServerPerSuite with MustMatchers with BeforeAndAfterAll with ScalaFutures {
  trait PlayerTest extends Test {
    val moderator = testSystem.actorOf(Props(new QuizzoModerator(port)), "quizzo-moderator")
    await(moderator ? Login())
    val player = testSystem.actorOf(Props(new QuizzoPlayer(port)), "quizzo-player")

  }

  trait StartedGameTest extends PlayerTest {
    val gameId = await((moderator ? StartGame()).mapTo[String])
  }

  trait WebSocketPlayerTest extends StartedGameTest {
    def startFunc(implicit sender: ActorRef): Unit
    def testFunc(respondTo: ActorRef)(implicit context: ActorContext): Receive
    def testResult = testSystem.actorOf(Props(new Actor {
      def receive = {
        case 'Test =>
          val respondTo = sender()
          startFunc
          context.become {
            testFunc(respondTo)
          }
      }
    })) ? 'Test
  }

  "GamePlayer controller" must {
    "provide a listing of active games" in new StartedGameTest {
      val testPort = port
      Logger.info(s"test port is $port")
      whenReady(route(FakeRequest(GET, "/api/v1.0/player/games")).get) { result =>
        result.header.status mustBe 200
        val games = Json.parse(new String(await(result.body |>>> Iteratee.consume[Array[Byte]]()))).as[JsArray]
        games.value.map(jv => (jv \ "id").as[String]).contains(gameId) mustBe true
      }
    }
    "support a player joining a game" in new StartedGameTest  {
      whenReady(route(FakeRequest(POST, s"/api/v1.0/player/games/$gameId").withJsonBody(Json.obj("command" -> "join", "nickname" -> "jim", "email" -> "jkirk@starfleet.mil"))).get) { result =>
        result.header.status mustBe 200
      }
    }
    "open a player web socket" in new WebSocketPlayerTest  {
      def startFunc(implicit sender: ActorRef) = player ! Join(gameId, "player1", "player1@gmail.com")
      def testFunc(respondTo: ActorRef)(implicit ac: ActorContext): Receive = {
        case 'JoinSuccessful =>
          Logger.info("Got JoinSuccessful!")
          respondTo ! TestSuccess
      }
      whenReady(testResult) { r => r mustBe TestSuccess }
      }
    }
    "close player web socket when invalid session ID is provided" in new WebSocketPlayerTest  {
      val confusedPlayer = testSystem.actorOf(Props(new QuizzoPlayer(port) {
        override def playerSession(sessionId: String) = "foobar"
      }))

      def startFunc(implicit sender: ActorRef) = confusedPlayer ! Join(gameId, "player1", "player1@gmail.com")
      def testFunc(respondTo: ActorRef)(implicit ac: ActorContext): Receive = {
        case 'JoinSuccessful =>
          ac.become {
            case ConnectionClosed =>
              respondTo ! TestSuccess
          }
      }
      whenReady(testResult) { r => r mustBe TestSuccess }
    }
    "notifies player when other players join" in new WebSocketPlayerTest  {
      val player2 = testSystem.actorOf(Props(new QuizzoPlayer(port)), "quizzo-player-2")

      def startFunc(implicit sender: ActorRef) = player ! Join(gameId, "player1", "player1@gmail.com")
      def testFunc(respondTo: ActorRef)(implicit context: ActorContext) = {
        case 'JoinSuccessful =>
          player2.tell(Join(gameId, "player2", "player2@gmail.com"), context.self)
          context.become {
            case PlayerJoined(gid) =>
              respondTo ! TestSuccess
          }
      }
      pending
      whenReady(testResult) { r => r mustBe TestSuccess }

    }
    "notifies player when question is opened" in new WebSocketPlayerTest  {
      def startFunc(implicit sender: ActorRef) = player ! Join(gameId, "player1", "player1@gmail.com")
      def testFunc(respondTo: ActorRef)(implicit context: ActorContext) = {
        case 'JoinSuccessful =>
          moderator ! NextQuestion(gameId)
          context.become {
            case QuestionOpen(question) =>
              respondTo ! TestSuccess
          }
      }
      whenReady(testResult) { r => r mustBe TestSuccess }
    }
    "notifies player when question is closed" in new WebSocketPlayerTest  {
      def startFunc(implicit sender: ActorRef) = player ! Join(gameId, "player1", "player1@gmail.com")
      def testFunc(respondTo: ActorRef)(implicit context: ActorContext) = {
        case 'JoinSuccessful =>
          moderator ! NextQuestion(gameId)
          context.become {
            case QuestionOpen(question) =>
              moderator ! NextQuestion(gameId)
              context.become {
                case ReviewingQuestion(question) =>
                  respondTo ! TestSuccess
              }
          }
      }
      whenReady(testResult) { r => r mustBe TestSuccess }

    }
    "notifies player when game is over" in pending
    "allows player to answer question when in open state" in new WebSocketPlayerTest  {
      def startFunc(implicit sender: ActorRef) = player ! Join(gameId, "player1", "player1@gmail.com")
      def testFunc(respondTo: ActorRef)(implicit context: ActorContext) = {
        case 'JoinSuccessful =>
          moderator ! NextQuestion(gameId)
          context.become {
            case QuestionOpen(question) =>
              player ! AnswerQuestion("1-2-3-4")
              // Close question and verify we get next event
              moderator ! NextQuestion(gameId)
              context.become {
                case ReviewingQuestion(question) =>
                  respondTo ! TestSuccess
              }
          }
      }
      whenReady(testResult) { r => r mustBe TestSuccess }
    }
}
