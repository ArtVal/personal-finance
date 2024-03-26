package storages.operations

import zio.{Task, ZIO}

import java.time.Instant

trait OperationRepo {
  def list(userId: Int, from: Instant, to: Instant): Task[Seq[Operation]]
  def add(operation: Operation): Task[Operation]
  def delete(id: Int): Task[Unit]
}
object OperationRepo {
  def list(userId: Int, from: Instant, to: Instant): ZIO[OperationRepo, Throwable, Seq[Operation]] =
    ZIO.serviceWithZIO[OperationRepo](_.list(userId, from, to))

  def add(operation: Operation): ZIO[OperationRepo, Throwable, Operation] =
    ZIO.serviceWithZIO[OperationRepo](_.add(operation))

  def delete(id: Int): ZIO[OperationRepo, Throwable, Unit] =
    ZIO.serviceWithZIO[OperationRepo](_.delete(id))
}
