import db.DbDataSource.zioDS
import db.{MigrationService, MigrationServiceImpl}
import http.{Authentication, FinanceAccount}
import zio.http.Header.Authorization
import zio.http.{HttpApp, Server}
import zio.{Scope, ZIO, ZIOApp, ZIOAppArgs, ZIOAppDefault}

object App extends ZIOAppDefault {
  val routes: HttpApp[Any] = FinanceAccount.routes ++ Authentication.login ++ Authentication.user
  val app: ZIO[MigrationService, Throwable, Unit] = MigrationService.migrate *> Server.serve(routes).provide(Server.default)
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = app
    .provide(MigrationServiceImpl.layer, MigrationService.liquibaseLayer, zioDS)
}
