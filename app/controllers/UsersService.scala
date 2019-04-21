package controllers


import java.util.{Date, UUID}

import javax.inject.{Inject, Singleton}
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.{ExecutionContext, Future}

case class UserInfo(id: UUID, passwordHash: String)

@Singleton
class UsersService @Inject()(usersRepo: UsersRepository) {

  def addUser(email: String, username: String, password: String): Future[Boolean] = {
    usersRepo.addUser(email, username, password)
  }

  def login(email: String, password: String)
           (implicit ec: ExecutionContext): Future[Option[UUID]] = {

    usersRepo.getUserData(email)
      .map(usrDataOpt => {
        usrDataOpt.flatMap(usrData => {
          val passes = BCrypt.checkpw(password, usrData.passwordHash)
          if (passes) {
            Option.apply(usrData.id)
          } else {
            Option.empty
          }
        })
      })
  }

  def addInvitation(from: UUID, to: UUID, msg: String): Future[Unit] = {
    usersRepo.addInvitation(from, to, msg)
  }



}
