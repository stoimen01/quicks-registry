package controllers

import java.time.{LocalDateTime, ZoneId}
import java.util.UUID

import javax.inject._
import org.mindrot.jbcrypt.BCrypt
import play.api.db.Database
import play.api.libs.json._
import play.api.mvc.{AnyContent, _}
import java.util.Date

import com.nimbusds.jose._
import com.nimbusds.jose.crypto._
import com.nimbusds.jose.jwk.gen._
import com.nimbusds.jwt._

import scala.concurrent.Future


case class User(email: String, username: String, password: String)
case class Credentials(email: String, password: String)

class IndexController @Inject
(db: Database, val controllerComponents: ControllerComponents)
(implicit ec: DatabaseExecutionContext) extends BaseController {

  implicit val userReads: Reads[User] = Json.reads[User]

  implicit val credentialsReads: Reads[Credentials] = Json.reads[Credentials]

  implicit val userWrites: Writes[User] = Json.writes[User]

  private val rsaJWK = new RSAKeyGenerator(2048).generate //.keyID("1010")

  private val rsaPublicJWK = rsaJWK.toPublicJWK

  private val signer = new RSASSASigner(rsaJWK)

  def register: Action[JsValue] = Action.async(parse.json) { request =>

    Future {

      val user = Json.fromJson[User](request.body)

      user match {

        case JsSuccess(user: User, _) =>

          val conn = db.getConnection()

          try {

            val stm1 = conn.createStatement
            val rs = stm1.executeQuery(s"SELECT * FROM users WHERE email=${user.email}")

            if (rs.next()) {

              BadRequest("User with this email already exists")

            } else {

              val SQL = "INSERT INTO users(id, deleted, created_at_utc, time_zone, email, username, password_hash, password_salt, privilege, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::PRIVILEGE, ?::STATUS)"

              val stmt = conn.prepareStatement(SQL)

              val uuid = UUID.randomUUID()
              val deleted = false
              val createdAt = LocalDateTime.now()
              val timeZone = ZoneId.systemDefault().toString
              val email = user.email
              val username = user.username
              val passSalt = BCrypt.gensalt()
              val passHash = BCrypt.hashpw(user.password, passSalt)
              val privilege = "USER"
              val status = "CREATED"

              stmt.setObject(1, uuid)
              stmt.setBoolean(2, deleted)
              stmt.setObject(3, createdAt)
              stmt.setString(4, timeZone)
              stmt.setString(5, email)
              stmt.setString(6, username)
              stmt.setString(7, passHash)
              stmt.setString(8, passSalt)
              stmt.setString(9, privilege)
              stmt.setString(10, status)

              stmt.executeUpdate()

              stmt.close()

              Ok("")
            }

          } finally {
            conn.close()
          }

        case e @ JsError(_) =>
          BadRequest("Errors: " + JsError.toJson(e).toString())
      }
    }(ec)
  }

  def login: Action[JsValue] = Action.async(parse.json) { request =>

    Future {

      val credentials = Json.fromJson[Credentials](request.body)

      credentials match {

        case JsSuccess(credentials: Credentials, _) =>

          val conn = db.getConnection()

          try {

            val stm1 = conn.createStatement
            val rs = stm1.executeQuery(s"SELECT * FROM users WHERE email='${credentials.email}'")

            if (rs.next()) {

              val passHash = rs.getString("password_hash")

              val passes = BCrypt.checkpw(credentials.password, passHash)

              if (passes) {

                val id = rs.getObject("id", classOf[UUID])

                val claimsSet = new JWTClaimsSet.Builder()
                  .claim("id", id.toString)
                  .expirationTime(new Date(new Date().getTime + 60 * 1000))
                  .build

                val signedJWT = new SignedJWT(
                  new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID).build(),
                  claimsSet
                )

                signedJWT.sign(signer)

                val serialized = signedJWT.serialize()

                Ok(serialized)
              } else {
                BadRequest("Wrong password")
              }

            } else {

              BadRequest("Wrong email")
            }

          } finally {
            conn.close()
          }

        case e @ JsError(_) =>
          BadRequest("Errors: " + JsError.toJson(e).toString())
      }
    }(ec)

  }

  def getToken: Action[AnyContent] = Action { request =>
    Ok(rsaPublicJWK.toJSONObject.toJSONString)
  }

  def getUserData: Action[AnyContent] = Action.async { request =>

    Future {

      val auth = request.headers.get("Authorization").get

      val signedJWT = SignedJWT.parse(auth)

      val verifier = new RSASSAVerifier(rsaPublicJWK)

      if (signedJWT.verify(verifier)) {

        var user: String = null

        val conn = db.getConnection()

        try {

          val id = signedJWT.getJWTClaimsSet.getStringClaim("id")

          val stmt = conn.createStatement
          val rs = stmt.executeQuery(s"SELECT * FROM users WHERE id='$id'")

          if (rs.next()) {
            user = rs.getString("username")
          }

          rs.close();

        } finally {
          conn.close()
        }

        if (user != null) {
          Ok(s"Hi $user")
        } else {
          BadRequest("invalid user id !")
        }

      } else {
        BadRequest(s"invalid token ! $auth")
      }

    }(ec)

  }

}