package accounts

import db.Ctx
import db.Ctx.querySchema
import io.getquill.{EntityQuery, Quoted}
import zio.{Task, ZLayer}

import javax.sql.DataSource

case class PersistentAccountRepo(ds: DataSource) extends AccountRepo {
  val ctx: Ctx.type = Ctx
  import ctx._
  private lazy val accountSchema: Quoted[EntityQuery[Account]] = quote {
    querySchema[Account]("public.account", _.userId -> "user_id")
  }


  override def register(userId: Int): Task[Int] = {
    for {
      id <- ctx.run {
        accountSchema.insertValue {
          lift(Account(userId, 0.0))
        }.returning(_.userId)
      }
    } yield id
  }.provide(ZLayer.succeed(ds))

  override def lookup(userId: Int): Task[Option[Double]] =
    ctx
      .run {
        accountSchema
          .filter(p => p.userId == lift(userId))
          .map(a => a.balance)
      }
      .provide(ZLayer.succeed(ds))
      .map(_.headOption)

  override def updateAccount(userId: Int, balance: Double): Task[Double] = {
    for {
      balance <- ctx.run {
        accountSchema.updateValue {
          lift(Account(userId, balance))
        }.returning(_.balance)
      }
    } yield balance
  }.provide(ZLayer.succeed(ds))
}

object PersistentAccountRepo {
  def layer: ZLayer[DataSource, Nothing, PersistentAccountRepo] =
      ZLayer.fromFunction(PersistentAccountRepo(_))
}