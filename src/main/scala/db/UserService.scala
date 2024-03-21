package db

import zio.ZIO

import java.sql.SQLException

case class User(login: String, password: String)

trait UserService {
  def getPeople: ZIO[Any, SQLException, List[User]]
}
