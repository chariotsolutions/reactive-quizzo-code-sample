import actors.{GamesActor, SessionActor}
import akka.actor.ActorRef
import controllers.CorsFilter
import play.api.mvc.{Filters, EssentialAction}
import play.api.{Application, GlobalSettings}
import play.api.libs.concurrent.Akka
import play.api.Play.current

object Global extends GlobalSettings{
  var sessionActor: Option[ActorRef] = None
  var gamesActor: Option[ActorRef] = None
  override def onStart(app: Application): Unit = {
    sessionActor = Some(SessionActor(Akka.system))
    gamesActor = Some(GamesActor(Akka.system))
  }

  override def onStop(app: Application): Unit = {
    sessionActor.map(ref => Akka.system.stop(ref))
    gamesActor.map(ref => Akka.system.stop(ref))
  }

  override def doFilter(next: EssentialAction): EssentialAction = {
    Filters(super.doFilter(next), new CorsFilter(configuration.getString("cors.allowed-origin").getOrElse("*")))
  }

}


