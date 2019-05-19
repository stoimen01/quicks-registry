package controllers

import java.util.concurrent.ConcurrentHashMap
import java.util.{Date, UUID}

import com.nimbusds.jose._
import com.nimbusds.jose.crypto._
import com.nimbusds.jose.jwk.gen._
import com.nimbusds.jwt._
import javax.inject._
import play.api.db.Database
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{AnyContent, _}

import scala.concurrent.Future

class UsersController @Inject
(usersService: UsersService, db: Database, val controllerComponents: ControllerComponents)
(implicit ec: DatabaseExecutionContext) extends BaseController {

  private val rsaJWK = new RSAKeyGenerator(2048).generate

  private val rsaPublicJWK = rsaJWK.toPublicJWK

  private val signer = new RSASSASigner(rsaJWK)

  private val map = new ConcurrentHashMap[UUID, UUID]()


  def register: Action[User] = Action.async(parse.json[User]) { request =>

    val user = request.body

    usersService.addUser(user.email, user.username, user.password)
      .map(usrIdOpt =>
        usrIdOpt
          .map(usrId => {
            val wsId = UUID.randomUUID()
            map.put(wsId, usrId)
            val response = WelcomeResponse(tokenOf(usrId), wsId.toString)
            Ok(Json.toJson(response))
          })
          .getOrElse(InternalServerError)
      )
  }


  def login: Action[Credentials] = Action.async(parse.json[Credentials]) { request =>

    val credentials = request.body

    usersService.login(credentials.email, credentials.password)
        .map(usrIdOpt => {
          usrIdOpt
            .map(usrId => {
              val wsId = UUID.randomUUID()
              map.put(wsId, usrId)
              val response = WelcomeResponse(tokenOf(usrId), wsId.toString)
              Ok(Json.toJson(response))
            })
            .getOrElse(BadRequest("Wrong email"))
        })
  }


  def invite: Action[Invitation] = authAction { request =>

    val userId = request.principal.id
    val friendId = UUID.fromString(request.body.id)

    usersService.addInvitation(userId, friendId, request.body.msg)
        .map(_ => Ok("Invitation success !"))
  }


  def acceptInvitation: Action[InvitationAnswer] = authAction { request =>

    // recipient wants to accept invitation from sender
    val recipient = request.principal.id
    val sender = UUID.fromString(request.body.id)

    usersService.acceptInvitation(recipient, sender)
        .map(_ => Ok("Happy friendship !"))
  }


  def rejectInvitation: Action[InvitationAnswer] = authAction { request =>

    // recipient wants to reject invitation from sender
    val recipient = request.principal.id
    val sender = UUID.fromString(request.body.id)

    usersService.rejectInvitation(recipient, sender)
      .map(_ => Ok("Happy friendship !"))
  }


  def getToken: Action[AnyContent] = Action { request =>
    Ok(rsaPublicJWK.toJSONObject.toJSONString)
  }


  def createDevice: Action[CreateDeviceRequest] = authAction { request =>
    val creator = request.principal.id
    usersService.createDevice(creator, request.body.name)
      .map(deviceOpt => {
        deviceOpt
          .map(d => {
            val response = CreateDeviceResponse(d.id.toString, d.secret.toString)
            Ok(Json.toJson(response))
          })
          .getOrElse(BadRequest("Bad Data"))
      })
  }




  case class Principal(id: UUID)

  case class AuthRequest[A](principal: Principal, request: Request[A])
    extends WrappedRequest[A](request)

  def authAction[T](block: AuthRequest[T] => Future[Result])
                   (implicit reader: Reads[T]): Action[T] = Action.async(parse.json[T]) { request =>

    val token = request.headers.get(AUTHORIZATION).get

    val signedJWT = SignedJWT.parse(token)

    val verifier = new RSASSAVerifier(rsaPublicJWK)

    if (signedJWT.verify(verifier)) {
      val userId = UUID.fromString(signedJWT.getJWTClaimsSet.getStringClaim("id"))
      val principal = Principal(userId)
      val authRequest = new AuthRequest[T](principal, request)
      block(authRequest)
    } else {
      Future.successful(BadRequest("Invalid token !"))
    }
  }

  def tokenOf(id: UUID): String = {
    val claimsSet = new JWTClaimsSet.Builder()
      .claim("id", id.toString)
      .expirationTime(new Date(new Date().getTime + 600 * 1000))
      .build

    val signedJWT = new SignedJWT(
      new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID).build(),
      claimsSet
    )

    signedJWT.sign(signer)

    signedJWT.serialize()
  }

}