package model

import play.api.libs.json._

object User {
  implicit val roleFormat = Json.format[Role]
  implicit val userFormat = Json.format[User]
}
case class User(id: String, password: String, roles: Set[Role])

object Role {
  implicit val format = Json.format[Role]
}
case class Role(name: String)

object UserSession {
  implicit val format = Json.format[UserSession]
}
case class UserSession(id: String, userId: String, roles: Set[Role])