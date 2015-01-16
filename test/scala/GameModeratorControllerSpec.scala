package scala

import akka.actor.Actor._
import akka.actor._
import akka.pattern.ask
import model.Games
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, MustMatchers}
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.QuizzoModerator.{Login, NextQuestion, StartGame}
import scala.QuizzoPlayer.Join
import scala.language.postfixOps

class GameModeratorControllerSpec extends PlaySpec with OneServerPerSuite with MustMatchers with BeforeAndAfterAll with ScalaFutures {

  trait PlayerTest extends Test {
    def login = Login()
    val moderator = testSystem.actorOf(Props(new QuizzoModerator(port)), "quizzo-moderator")
    val session = await((moderator ? login).mapTo[String])
    val player = testSystem.actorOf(Props(new QuizzoPlayer(port)), "quizzo-player")
  }

  trait StartedGameTest extends PlayerTest {
    val gameId = await((moderator ? StartGame()).mapTo[String])
  }

  trait WebSocketPlayerTest extends StartedGameTest {
    def startFunc(implicit sender: ActorRef): Unit = player ! Join(gameId, "player1", "player1@gmail.com")
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

  import scala.QuizzoTestSupport._

  val baseUrl: String = s"http://localhost:$port/api/v1.0/moderator/games"

  "GameModerator controller" must {
    "provide a list of available games" in new StartedGameTest {
      whenReady(route(FakeRequest(GET, "/api/v1.0/moderator/available-games")).get) {
        case r if r.header.status != 200 => fail(s"Unexpected response: $r")
        case r =>
          (json(r) \\ "title").length mustBe 3
      }
    }
    "provide current state of an active game when requested by moderator" in new StartedGameTest {
      await(player ? PlayerOneJoin(gameId))
      await(moderator ? NextQuestion(gameId))
      whenReady(route(FakeRequest(GET, s"/api/v1.0/moderator/games/$gameId").withHeaders("session" -> session )).get) {
        case r if r.header.status != 200 => fail(s"Unexpected response: $r")
        case r =>
          (json(r) \ "question").as[String] must startWith("A mutable variable")
      }
    }
  }
  "GameModerator.gameAction" must {
    "reject requests that have no credentials" in {
      whenReady(route(FakeRequest(POST, s"/api/v1.0/moderator/games/${Games.ScalaQuiz.id}").withJsonBody(Json.obj("command" -> "start"))).get) { result =>
        result.header.status mustBe 401
      }
    }
    "reject requests that have an unsupported command" in new StartedGameTest {
      val sid = await((moderator ? 'GetSession).mapTo[String])
        whenReady(route(FakeRequest(POST, s"/api/v1.0/moderator/games/${Games.ScalaQuiz.id}").withHeaders("session" -> session).withJsonBody(Json.obj("command" -> "iwantitall"))).get) { result =>
          result.header.status mustBe 400
        }
    }
  }
  "GameModerator.gameAction" must {
    "return Forbidden on request to create game where requester does not have proper role" in new PlayerTest {
      override def login = Login("michael", "michael")

      whenReady(route(FakeRequest(POST, s"/api/v1.0/moderator/games/${Games.ScalaQuiz.id}").withHeaders("session" -> session).withJsonBody(Json.obj("command" -> "start"))).get) { result =>
        result.header.status mustBe 403
      }
    }
    "start a new game when requested by moderator" in new PlayerTest {
      whenReady(route(FakeRequest(POST, s"/api/v1.0/moderator/games/${Games.ScalaQuiz.id}").withHeaders("session" -> session).withJsonBody(Json.obj("command" -> "start"))).get) { result =>
        result.header.status mustBe 201
      }
    }
    "advance game to next question when requested by moderator" in new WebSocketPlayerTest {
      def testFunc(respondTo: ActorRef)(implicit context: ActorContext) = {
        case 'JoinSuccessful =>
          whenReady(route(FakeRequest(POST, s"/api/v1.0/moderator/games/$gameId").withHeaders("session" -> session).withJsonBody(Json.obj("command" -> "nextQuestion"))).get) {
            case r if r.header.status != 200 => fail(s"Unexpected result on nextQuestion: $r")
            case r =>
              (json(r) \ "state") \ "state" mustBe "QuestionOpen"
          }
      }
    }
  }
  "GameModerator.getGames" must {
    "reject requests that are not authorized" in new StartedGameTest {
      whenReady(route(FakeRequest(GET, "/api/v1.0/moderator/games")).get) { result =>
        result.header.status mustBe 401
      }
    }
    "provide a list of all active games" in new StartedGameTest {
      whenReady(route(FakeRequest(GET, "/api/v1.0/moderator/games").withHeaders("session" -> session)).get) {
        case r if r.header.status == 200 =>
          val games = (json(r) \\ "id")
          games.find( jv => jv.as[String] == gameId ) mustBe 'defined
        case r => fail(s"Unexpected result listing games: $r")
       }
    }
  }
}
