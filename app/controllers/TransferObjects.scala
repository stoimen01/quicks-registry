package controllers

import play.api.libs.json.{Json, Reads}

case class User(email: String, username: String, password: String)
object User {
  implicit val userReads: Reads[User] = Json.reads[User]
}

case class Credentials(email: String, password: String)
object Credentials {
  implicit val credentialsReads: Reads[Credentials] = Json.reads[Credentials]
}

case class Invitation(id: String, msg: String)
object Invitation {
  implicit val invitationReads: Reads[Invitation] = Json.reads[Invitation]
}

case class InvitationAnswer(id: String)
object InvitationAnswer {
  implicit val invitationAnswerReads: Reads[InvitationAnswer] = Json.reads[InvitationAnswer]
}