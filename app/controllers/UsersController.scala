package controllers

import java.util.{Date, UUID}

import com.nimbusds.jose._
import com.nimbusds.jose.crypto._
import com.nimbusds.jose.jwk.gen._
import com.nimbusds.jwt._
import javax.inject._
import play.api.db.Database
import play.api.mvc.{AnyContent, _}

import scala.concurrent.Future

class UsersController @Inject
(usersService: UsersService, db: Database, val controllerComponents: ControllerComponents)
(implicit ec: DatabaseExecutionContext) extends BaseController {

  private val rsaJWK = new RSAKeyGenerator(2048).generate

  private val rsaPublicJWK = rsaJWK.toPublicJWK

  private val signer = new RSASSASigner(rsaJWK)

  def register: Action[User] = Action.async(parse.json[User]) { request =>

    val user = request.body

    usersService.addUser(user.email, user.username, user.password)
      .map(isAdded =>
        if (isAdded) Ok("Cool")
        else BadRequest("Nooo")
      )
  }

  def login: Action[Credentials] = Action.async(parse.json[Credentials]) { request =>

    val credentials = request.body

    usersService.login(credentials.email, credentials.password)
        .map(usrIdOpt => {
          usrIdOpt
            .map(usrId => {

              val claimsSet = new JWTClaimsSet.Builder()
                .claim("id", usrId.toString)
                .expirationTime(new Date(new Date().getTime + 600 * 1000))
                .build

              val signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID).build(),
                claimsSet
              )

              signedJWT.sign(signer)

              val serialized = signedJWT.serialize()

              Ok(serialized)
            })
            .getOrElse(BadRequest("Wrong email"))
        })
  }

  def invite: Action[Invitation] = Action.async(parse.json[Invitation]) { request =>

      val invitation = request.body

      val auth = request.headers.get(AUTHORIZATION).get

      val signedJWT = SignedJWT.parse(auth)

      val verifier = new RSASSAVerifier(rsaPublicJWK)

      if (signedJWT.verify(verifier)) {

        val user_id = UUID.fromString(signedJWT.getJWTClaimsSet.getStringClaim("id"))
        val friend_id = UUID.fromString(invitation.id)

        usersService.addInvitation(user_id, friend_id, invitation.msg)
            .map(_ => Ok("asd"))

      } else {
        Future.successful(BadRequest(s"invalid token ! $auth"))
      }
  }

  def acceptInvite = TODO

  def getToken: Action[AnyContent] = Action { request =>
    Ok(rsaPublicJWK.toJSONObject.toJSONString)
  }

}