package actors

import java.util.UUID

import akka.actor._
import model.{GameInstance, Games}

object GamesActor {
  def apply(actorSystem: ActorSystem) = actorSystem.actorOf(Props[GamesActor], "games-actor")

  case class StartGame(id: String, moderator: String)
  case object GetGames
  case object UnknownGame
}

class GamesActor extends Actor {
  import actors.GamesActor._

  override def receive = handler(Set.empty)

  def handler(games: Set[GameInstance]): Receive = {
    case StartGame(id: String, moderator: String) =>
     Games.AllGames.find(g => g.id == id) match {
       case Some(game) =>
         val gameId = UUID.randomUUID().toString
         val gameActor = context.actorOf(Props(new GameActor()), s"game-$gameId")
         val instance = GameInstance(gameId, game, moderator, Map.empty)
         gameActor.tell(GameActor.StartGame(instance), sender)
         context.become(handler(games + instance))
       case None =>
         sender ! UnknownGame
     }
    case GetGames =>
      sender ! games
  }
}
