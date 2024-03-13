import db.{MigrationService, MigrationServiceImpl}
import zio.{Scope, ZIO, ZIOApp, ZIOAppArgs, ZIOAppDefault}

object Hello extends ZIOAppDefault {
  val app: ZIO[MigrationService, Throwable, Unit] = MigrationService.migrate
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = app.provide(MigrationServiceImpl.layer)
}
