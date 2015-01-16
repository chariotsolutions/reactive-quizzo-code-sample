import model.{Role, UserSession}
import org.apache.commons.codec.digest.DigestUtils
import org.scalatestplus.play.{PlaySpec, OneAppPerSuite}
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

class ApplicationSpec extends PlaySpec with OneAppPerSuite {

  "Application" must {

    "send 404 on a bad request" in new WithApplication{
      route(FakeRequest(GET, "/boum")) mustBe None
    }

    "login successfully and return roles" in new WithApplication() {
      val pwd = DigestUtils.md5Hex("michael")
      val result = route(FakeRequest(POST, "/login").withJsonBody(Json.obj("user" -> "michael", "password" -> pwd)))
      result must not be(None)
      val json = contentAsJson(result.get).as[UserSession]
      json.roles mustEqual Set(Role("player"))
      json.userId mustEqual "michael"
    }

    "send 400 when login attempted without proper request" in new WithApplication() {
      val result = route(FakeRequest(POST, "/login"))
      result must not be(None)
      val f = result.get
      status(f) mustEqual 400
    }

    "send 403 when login attempted with invalid credentials" in new WithApplication() {
      val result = route(FakeRequest(POST, "/login").withJsonBody(Json.parse("""{"user":"bozo","password":"foo"}""")))
      result must not be(None)
      val f = result.get
      status(f) mustEqual 403
    }
  }
}
