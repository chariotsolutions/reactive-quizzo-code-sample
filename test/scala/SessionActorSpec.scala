import java.util.UUID

import actors.SessionActor
import actors.SessionActor._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.TestActorRef
import akka.util.Timeout
import model.{UserSession, Role}
import org.apache.commons.codec.digest.DigestUtils.md5Hex
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}
import scala.concurrent.duration._
import scala.util.Success

class SessionActorSpec extends WordSpecLike with MustMatchers with BeforeAndAfterAll {

  implicit val system = ActorSystem("session-actor-spec")
  implicit val timeout = Timeout(5, SECONDS)

  override def afterAll = system.shutdown()

  "SessionActor" must {
    "respond with LoginOk to LoginRequest with valid credentials" in {
      val sessionActor = TestActorRef[SessionActor]
      val response = sessionActor ? LoginRequest("michael", md5Hex("michael"))
      val Success(Some(session: UserSession)) = response.value.get
      session.userId mustBe "michael"
      session.roles mustBe Set(Role("player"))
    }
    "create a session on LoginRequest with valid credentials" in {
      val sessionActor = TestActorRef[SessionActor]
      val response = sessionActor ? LoginRequest("michael", md5Hex("michael"))
      val testUser = sessionActor.underlyingActor.users.find(_.id == "michael").get
      sessionActor.underlyingActor.sessions.head.userId mustBe testUser.id
    }
    "respond with LoginRejected to LoginRequest with invalid credentials" in {
      val sessActor = TestActorRef[SessionActor]
      val response = sessActor ? LoginRequest("michael", "foo")
      response.value mustBe Some(Success(None))
    }
    "respond to LookupSession with SessionResponse when session exists" in {
      val sessionActor = TestActorRef[SessionActor]
      val testUser = sessionActor.underlyingActor.users.find(_.id == "michael").get
      val testSessionId = UUID.randomUUID().toString
      sessionActor.underlyingActor.sessions = Set(UserSession(testSessionId, testUser.id, testUser.roles))
      val Success(response) = (sessionActor ? LookupSession(testSessionId)).value.get
      response mustBe Some(UserSession(testSessionId, testUser.id, testUser.roles))
    }
    "respond to LookupSession with InvalidSession when session does not exist" in {
      val sessionActor = TestActorRef[SessionActor]
      val testUser = sessionActor.underlyingActor.users.find(_.id == "michael").get
      val testSessionId = UUID.randomUUID().toString
      sessionActor.underlyingActor.sessions = Set(UserSession(testSessionId, testUser.id, testUser.roles))
      val Success(response) = (sessionActor ? LookupSession("foo")).value.get
      response mustBe None
    }
  }
}
