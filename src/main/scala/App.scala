import db.DbDataSource.zioDS
import db.{MigrationService, MigrationServiceImpl}
import http.{Authentication, FinanceAccount}
import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import zio.crypto.hash.Hash
import zio.http.Header.Authorization
import zio.http.{HttpApp, Server}
import zio.{Scope, ZIO, ZIOApp, ZIOAppArgs, ZIOAppDefault, ZLayer}

import javax.sql.DataSource

object App extends ZIOAppDefault {
  val quillLayer: ZLayer[DataSource, Nothing, Quill.Postgres[SnakeCase.type]] = Quill.Postgres.fromNamingStrategy(SnakeCase)
  val routes: HttpApp[Hash] = FinanceAccount.routes ++ Authentication.authRoutes ++ Authentication.user
  val app: ZIO[MigrationService, Throwable, Nothing] = MigrationService.RunMigrate *> Server.serve(routes).provide(Server.default, Hash.live)
  override def run: ZIO[Any, Throwable, Unit] = app
    .provide(MigrationServiceImpl.layer, zioDS)
}
