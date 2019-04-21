package controllers

import java.time.{LocalDateTime, ZoneId}
import java.util.{Date, UUID}

import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import javax.inject.{Inject, Singleton}
import org.mindrot.jbcrypt.BCrypt
import play.api.db.Database

import scala.concurrent.Future

@Singleton
class UsersRepository @Inject()(db: Database)
                               (implicit ec: DatabaseExecutionContext) {

  def addUser(email: String, username: String, password: String) : Future[Boolean] = Future {

    val conn = db.getConnection()

    try {

      val stm1 = conn.createStatement
      val rs = stm1.executeQuery(s"SELECT * FROM users WHERE email='$email'")

      if (rs.next()) {
        false
      } else {

        val SQL = "INSERT INTO users(id, deleted, created_at_utc, time_zone, email, username, password_hash, password_salt, privilege, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::PRIVILEGE, ?::STATUS)"

        val stmt = conn.prepareStatement(SQL)

        val uuid = UUID.randomUUID()
        val deleted = false
        val createdAt = LocalDateTime.now()
        val timeZone = ZoneId.systemDefault().toString
        val passSalt = BCrypt.gensalt()
        val passHash = BCrypt.hashpw(password, passSalt)
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
        true
      }

    } finally {
      conn.close()
    }

  }(ec)


  def getUserData(email: String): Future[Option[UserInfo]] = Future {

    val conn = db.getConnection()

    try {

      val stm1 = conn.createStatement
      val rs = stm1.executeQuery(s"SELECT * FROM users WHERE email='$email'")

      if (rs.next()) {

        val passHash = rs.getString("password_hash")

        val id = rs.getObject("id", classOf[UUID])

        Option.apply(UserInfo(id, passHash))

      } else {
        Option.empty
      }

    } finally {
      conn.close()
    }

  }

  def addInvitation(from: UUID, to: UUID, msg: String): Future[Unit] = Future {

    val conn = db.getConnection()

    try {

      val stmt = conn.prepareStatement("INSERT INTO invitations(from_user, to_user, message) VALUES (?, ?, ?)")
      stmt.setObject(1, from)
      stmt.setObject(2, to)
      stmt.setObject(3, msg)

      stmt.execute()

    } finally {
      conn.close()
    }


  }




}
