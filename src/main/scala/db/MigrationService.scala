package db

import zio.Console._
import zio.{ULayer, ZIO, ZLayer}

trait MigrationService {

  def migrate: ZIO[Any, Throwable, Unit]
}

case class MigrationServiceImpl() extends MigrationService {
  override def migrate: ZIO[Any, Throwable, Unit] = for {
    _ <- printLine("migration")
  } yield ()
}

object MigrationServiceImpl {
  val layer: ULayer[MigrationServiceImpl] = ZLayer.succeed(MigrationServiceImpl())
}

object MigrationService {
  def migrate: ZIO[MigrationService, Throwable, Unit] = ZIO.serviceWithZIO[MigrationService](_.migrate)
}