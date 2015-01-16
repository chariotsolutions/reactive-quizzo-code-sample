package actors

import java.util.UUID

import akka.actor.{Actor, ActorSystem, Props}
import model.{UserSession, Role, User}
import org.apache.commons.codec.digest.DigestUtils

object SessionActor {
  def apply(implicit system: ActorSystem) = system.actorOf(Props[SessionActor], "session-actor")
  case class LoginRequest(user: String, password: String)
  case class LookupSession(sessionId: String)
  case class LogoutRequest(sessionId: String)
  case class CreatePlayerSession(nickname: String, email: String)
}

class SessionActor extends Actor {
  val users = Set(User("michael", DigestUtils.md5Hex("michael"), Set(Role("player"))), User("ken", DigestUtils.md5Hex("ken"), Set(Role("moderator"))))
  var sessions: Set[UserSession] = Set.empty

  import SessionActor._
  def receive = {
    case LoginRequest(user, pwd) =>
      users.find(u => u.id == user) match {
        case Some(u @ User(id, password, roles)) if pwd == password =>
          sender ! Some(sessions.find(s => s.userId == id && s.roles == roles).getOrElse {
            val session = UserSession(UUID.randomUUID().toString, id, roles)
            sessions = sessions + session
            session
           })
        case _ =>
          sender ! None
      }
    case CreatePlayerSession(nickname, email) =>
      sender ! Some(sessions.find(s => s.userId == nickname && s.roles == Set(Role("player"))).getOrElse {
        val session = UserSession(UUID.randomUUID().toString, nickname, Set(Role("player")))
        sessions = sessions + session
        session
        })
    case LookupSession(sid) =>
      sender ! sessions.find(v => v.id == sid)
    case LogoutRequest(sid) =>
      sessions.find(s => s.id == sid).foreach(s => sessions = sessions - s )
  }
}
