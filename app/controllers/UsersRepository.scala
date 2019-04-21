package controllers

import java.time.{LocalDateTime, ZoneId}
import java.util.UUID

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

        val SQL = "INSERT INTO users(id, created_at_utc, time_zone, email, username, password_hash, password_salt, privilege, status)" +
          " VALUES (?, ?, ?, ?, ?, ?, ?, ?::USER_PRIVILEGE, ?::ACCOUNT_STATUS)"

        val stmt = conn.prepareStatement(SQL)

        val uuid = UUID.randomUUID()
        val createdAt = LocalDateTime.now()
        val timeZone = ZoneId.systemDefault().toString
        val passSalt = BCrypt.gensalt()
        val passHash = BCrypt.hashpw(password, passSalt)
        val privilege = "USER"
        val status = "CREATED"

        stmt.setObject(1, uuid)
        stmt.setObject(2, createdAt)
        stmt.setString(3, timeZone)
        stmt.setString(4, email)
        stmt.setString(5, username)
        stmt.setString(6, passHash)
        stmt.setString(7, passSalt)
        stmt.setString(8, privilege)
        stmt.setString(9, status)

        stmt.executeUpdate()

        stmt.close()
        true
      }

    } finally {
      conn.close()
    }

  }


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

      val createdAt = LocalDateTime.now()
      val timeZone = ZoneId.systemDefault().toString

      val stmt = conn.prepareStatement("INSERT INTO invitations(from_user, to_user, message, created_at_utc, time_zone) VALUES (?, ?, ?, ?, ?)")
      stmt.setObject(1, from)
      stmt.setObject(2, to)
      stmt.setObject(3, msg)
      stmt.setObject(4, createdAt)
      stmt.setString(5, timeZone)

      stmt.execute()

    } finally {
      conn.close()
    }
  }

  def addFriendship(recipient: UUID, sender: UUID): Future[Unit] = Future {
    db.withConnection { conn =>

      // checking if there is invitation from friend to user
      val invStmt = conn.createStatement
      val invResult = invStmt.executeQuery(s"SELECT * FROM invitations WHERE from_user='$sender' AND to_user='$recipient'")
      if (invResult.next()) {

        // adding the friendship in the friends table and updating the invitation with accepted status
        val sql = "INSERT INTO friends(user_id, friend_id, created_at_utc, time_zone) VALUES (?, ?, ?, ?)"

        val createdAt = LocalDateTime.now()
        val timeZone = ZoneId.systemDefault().toString

        conn.setAutoCommit(false)

        val frStmt = conn.prepareStatement(sql)

        frStmt.setObject(1, recipient)
        frStmt.setObject(2, sender)
        frStmt.setObject(3, createdAt)
        frStmt.setString(4, timeZone)
        frStmt.addBatch()

        frStmt.setObject(1, sender)
        frStmt.setObject(2, recipient)
        frStmt.setObject(3, createdAt)
        frStmt.setString(4, timeZone)
        frStmt.addBatch()

        frStmt.executeBatch()

        val updateSql = "UPDATE invitations SET status='ACCEPTED'::INVITATION_STATUS" +
          s" WHERE from_user='$sender' AND to_user='$recipient'"

        conn.createStatement.executeUpdate(updateSql)

        conn.commit()

      } else {
        // no invitation found
        Unit
      }
    }
  }

  def setInvitationRejected(recipient: UUID, sender: UUID): Future[Unit] = Future {
    db.withConnection { conn =>

      val updateSql = "UPDATE invitations SET status='REJECTED'::INVITATION_STATUS" +
        s" WHERE from_user='$sender' AND to_user='$recipient'"

      conn.createStatement.executeUpdate(updateSql)

      Unit

    }

  }



}
