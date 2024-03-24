package users

import accounts.AccountRepo
import db.Ctx
import db.Ctx.querySchema
import io.getquill.{EntityQuery, Quoted}
import zio.{Task, ZLayer}

import javax.sql.DataSource

case class UserTable(id: Int, login: String, password: String)
case class PersistentUserRepo(ds: DataSource, account: AccountRepo) extends UserRepo {
  val ctx: Ctx.type = Ctx
  import ctx._
  private lazy val userSchema: Quoted[EntityQuery[UserTable]] = quote {
    querySchema[UserTable]("public.user")
  }


  override def register(user: User): Task[Int] = {
    for {
      id <- ctx.run {
        userSchema.insertValue {
          lift(UserTable(0, user.login, user.password))
        }.returningGenerated(_.id)
      }
    } yield id
  }.provide(ZLayer.succeed(ds))

  override def lookup(id: Int): Task[Option[User]] =
    ctx
      .run {
        userSchema
          .filter(p => p.id == lift(id))
          .map(u => User(u.id, u.login, u.password))
      }
      .provide(ZLayer.succeed(ds))
      .map(_.headOption)

  override def users: Task[List[User]] =
    ctx
      .run {
          userSchema.map(u => User(u.id, u.login, u.password))
      }
      .provide(ZLayer.succeed(ds))

  override def lookupByLogin(login: String): Task[Option[User]] =
    ctx
      .run {
          userSchema
            .filter(p => p.login == lift(login))
            .map(u => User(u.id, u.login, u.password))
      }
      .provide(ZLayer.succeed(ds))
      .map(_.headOption)
}

object PersistentUserRepo {
  def layer: ZLayer[DataSource with AccountRepo, Nothing, PersistentUserRepo] =
      ZLayer.fromFunction(PersistentUserRepo(_, _))
}