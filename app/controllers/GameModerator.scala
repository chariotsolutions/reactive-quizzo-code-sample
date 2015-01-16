package controllers

import actors.{GameActor, GamesActor}
import akka.pattern.ask
import akka.util.Timeout
import model.{GameInstance, GameQuestion, Games, Role}
import play.Logger
import play.api.mvc._
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import SecurityActions.RoleRequiredAction

object GameModerator extends GameModerator

trait GameModerator extends Controller {
  implicit val timeout = Timeout(5, SECONDS)
  implicit val ec: ExecutionContext = Akka.system.dispatcher

  def listGames = Action {
    Ok(toJson(Games.AllGames))
  }

  private val ModeratorRoleAction = RoleRequiredAction(Role("moderator"))

  def getGame(id: String) = ModeratorRoleAction.async { req =>
    gameActor(id) ? GameActor.GetQuestion map {
      case q: GameQuestion => Ok(toJson(q))
      case _ => NotFound
    }
  }

  def getGames = ModeratorRoleAction.async { req =>
    (gamesActor ? GamesActor.GetGames).mapTo[Set[GameInstance]] map {
      case games: Set[GameInstance] => Ok(toJson(games))
      case _ => NotFound
    }
  }

  def gameAction(id: String) = Action.async(parse.json) { req =>
    val JsString(cmd) = req.body \ "command"
    cmd match {
      case "start" => startGame(id).apply(req)
      case "nextQuestion" => nextQuestion(id).apply(req)
      case _ => Future.successful(BadRequest)
    }
  }

  private def nextQuestion(id: String) = ModeratorRoleAction.async(parse.json) { req =>
    gameActor(id) ? GameActor.NextQuestion map {
      case q: actors.NextQuestionResponse => Ok(Json.obj("state" -> q.state, "question" -> q.question, "scores" -> playerObject(q.scores) ))
      case _ => NotFound
    }
  }

  private def playerObject(players: Map[String, Int]) = players.map { p =>
    Json.obj("player" -> p._1, "score" -> p._2)
  }

  private def startGame(id: String) = ModeratorRoleAction.async(parse.json) { req =>
    gamesActor.ask(GamesActor.StartGame(id, req.userSession.userId)).map {
      case GameActor.GameStarted(gid) => Created(gid)
      case _ => NotFound
    }
  }
}
