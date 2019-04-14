package controllers

import javax.inject._
import play.api.db.Database
import play.api.libs.json._
import play.api.mvc._

import scala.collection.mutable
import scala.concurrent.Future


case class User(name: String, years: Int)

class IndexController @Inject()
(db: Database, val controllerComponents: ControllerComponents)
(implicit ec: DatabaseExecutionContext) extends BaseController {

  implicit val userReads: Reads[User] = Json.reads[User]

  implicit val userWrites: Writes[User] = Json.writes[User]

  def getUsers: Action[AnyContent] = Action.async {

    Future {

      val users = mutable.MutableList.empty[User]

      val conn = db.getConnection()

      try {
        val stmt = conn.createStatement
        val rs = stmt.executeQuery("SELECT * from users")

        while (rs.next()) {
          users += User(rs.getString("name"), rs.getInt("years"))
        }

      } finally {
        conn.close()
      }

      Ok(Json.toJson(users.toList))

    }(ec)

  }


  def addUser: Action[JsValue] = Action.async(parse.json) { request =>

    Future {

      val user = Json.fromJson[User](request.body)

      user match {

        case JsSuccess(r: User, _) =>

          val conn = db.getConnection()

          try {

            val stmt = conn.createStatement

            stmt.executeUpdate(s"INSERT INTO users(name, years) VALUES ('${r.name}', ${r.years})")

          } finally {
            conn.close()
          }

          Ok("")

        case e @ JsError(_) =>
          BadRequest("Errors: " + JsError.toJson(e).toString())

      }

    }(ec)

  }

}