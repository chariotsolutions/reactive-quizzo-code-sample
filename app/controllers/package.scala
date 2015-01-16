import play.api.libs.concurrent.Akka
import play.api.Play.current

package object controllers {
  def gamesActor = Akka.system.actorSelection(s"/user/games-actor")

  def sessionActor = Akka.system.actorSelection(s"/user/session-actor")

  def gameActor(id: String) = Akka.system.actorSelection(s"/user/games-actor/game-$id")
}
