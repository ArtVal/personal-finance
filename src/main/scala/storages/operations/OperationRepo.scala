package storages.operations

import zio.{Task, ZIO}

import java.time.Instant

trait OperationRepo {
  def list(userId: Int, from: Instant, to: Instant): Task[Seq[Operation]]
  def lookup(operationId: Int): Task[Option[Operation]]
  def add(operation: Operation): Task[Operation]
  def delete(operationId: Int, userId: Int): Task[Unit]
}
object OperationRepo {
  def list(userId: Int, from: Instant, to: Instant): ZIO[OperationRepo, Throwable, Seq[Operation]] =
    ZIO.serviceWithZIO[OperationRepo](_.list(userId, from, to))

  def lookup(operationId: Int): ZIO[OperationRepo, Throwable, Option[Operation]] =
    ZIO.serviceWithZIO[OperationRepo](_.lookup(operationId))

  def add(operation: Operation): ZIO[OperationRepo, Throwable, Operation] =
    ZIO.serviceWithZIO[OperationRepo](_.add(operation))

  def delete(operationId: Int, userId: Int): ZIO[OperationRepo, Throwable, Unit] =
    ZIO.serviceWithZIO[OperationRepo](_.delete(operationId, userId))
}
