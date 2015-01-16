package controllers

import actors.SessionActor
import akka.pattern.ask
import akka.util.Timeout
import model.{Role, UserSession}
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.Play.current

import scala.concurrent.Future
import scala.concurrent.duration._

object SecurityActions {
  class SessionRequest[A](val sessionId: String, request: Request[A]) extends WrappedRequest[A](request)

  object SessionRequiredAction extends ActionBuilder[SessionRequest] with ActionRefiner[Request, SessionRequest] with Results {
    def refine[A](request: Request[A]) = Future.successful {
      request.headers.get("session").map(new SessionRequest(_, request)).toRight(Unauthorized)
    }
  }

  class AuthenticatedRequest[A](val userSession: UserSession, sessionRequest: SessionRequest[A]) extends WrappedRequest[A](sessionRequest)
  object UserRequiredAction extends ActionRefiner[SessionRequest, AuthenticatedRequest] with Results {
    implicit val timeout = Timeout(5, SECONDS)
    implicit val ec = Akka.system.dispatcher
    def refine[A](sessionRequest: SessionRequest[A]) = Akka.system.actorSelection("/user/session-actor").ask(
      SessionActor.LookupSession(sessionRequest.sessionId)).map {
      case Some(session: UserSession) =>
        Right(new AuthenticatedRequest(session, sessionRequest))
      case any =>
        Left(Unauthorized)
    }
  }

  object UserRoleRequiredAction {
    def apply(requiredRole: Role) = new UserRoleRequiredAction(requiredRole)
  }

  class UserRoleRequiredAction(requiredRole: Role) extends ActionFilter[AuthenticatedRequest] with Results {
    def filter[A](authenticatedRequest: AuthenticatedRequest[A]) = Future.successful {
      if (authenticatedRequest.userSession.roles.contains(requiredRole)) None else Some(Forbidden)
    }
  }

  object RoleRequiredAction {
    def apply(role: Role) = SessionRequiredAction andThen UserRequiredAction andThen UserRoleRequiredAction(role)
  }
}
