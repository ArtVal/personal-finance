package storages.operations

import io.getquill.{EntityQuery, Quoted}
import storages.Ctx
import zio.{Task, ZLayer}

import java.time.Instant
import javax.sql.DataSource


case class PersistentOperationRepo(ds: DataSource) extends OperationRepo {

  val ctx: Ctx.type = Ctx
  import ctx._
  private lazy val operationSchema: Quoted[EntityQuery[Operation]] = quote {
    querySchema[Operation]("public.account_operation")
  }

  override def list(userId: Int, from: Instant, to: Instant): Task[Seq[Operation]] =
    ctx.run{
      operationSchema.filter(o => o.accountId == lift(userId) && sql"${o.created} BETWEEN ${lift(from)} AND ${lift(to)}".as[Boolean])
    }.provide(ZLayer.succeed(ds))

  override def add(operation: Operation): Task[Operation] =
    ctx.run{
      operationSchema
        .insertValue(operation)
        .returningGenerated(o => (o.id, o.created))
    }.provide(ZLayer.succeed(ds))
      .map{
        case (id, created) => operation.copy(id = id, created = created)
      }

  override def delete(operationId: Int, userId: Int): Task[Unit] =
    ctx.run{
        operationSchema
          .filter(o => o.id == lift(operationId) && o.accountId == userId)
          .delete
      }.provide(ZLayer.succeed(ds))
      .map(_ => ())

  override def lookup(operationId: Int): Task[Option[Operation]] = {
    ctx.run {
        operationSchema.filter(_.id == lift(operationId))
      }
      .provide(ZLayer.succeed(ds))
      .map(_.headOption)
  }
}

object PersistentOperationRepo {
  def layer: ZLayer[DataSource, Nothing, PersistentOperationRepo] =
    ZLayer.fromFunction(PersistentOperationRepo(_))
}
