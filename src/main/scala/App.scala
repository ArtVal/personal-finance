import config.HttpServerConfig
import controllers.{Accounts, Authentication}
import storages.accounts.{AccountRepo, PersistentAccountRepo}
import storages.users.{PersistentUserRepo, User, UserRepo}
import storages.{MigrationService, MigrationServiceImpl}
import io.getquill.jdbczio.Quill
import services.{UserService, UserServiceImpl}
import zio.config.typesafe.TypesafeConfigProvider
import zio.crypto.hash.Hash
import zio.http.netty.NettyConfig
import zio.http.{HttpApp, Response, Server}
import zio.{Config, Console, Ref, Runtime, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import javax.sql.DataSource

object App extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.setConfigProvider(
      TypesafeConfigProvider
        .fromResourcePath()
    )

  private val serverConfig: ZLayer[Any, Config.Error, Server.Config] =
    ZLayer
      .fromZIO(
        ZIO.config[HttpServerConfig](HttpServerConfig.config).map { c =>
          Server.Config.default.binding(c.host, c.port)
        }
      )

  private val nettyConfig: ZLayer[Any, Config.Error, NettyConfig] =
    ZLayer
      .fromZIO(
        ZIO.config[HttpServerConfig](HttpServerConfig.config).map { c =>
          NettyConfig.default.maxThreads(c.nThreads)
        }
      )
//  val quillLayer: ZLayer[DataSource, Nothing, Quill.Postgres[SnakeCase.type]] = Quill.Postgres.fromNamingStrategy(SnakeCase)
  val routes: HttpApp[AccountRepo with UserRepo with UserService with Hash] = Authentication() ++ Accounts()
//  val app: ZIO[MigrationService, Throwable, Nothing] = MigrationService.RunMigrate *> Server.serve(routes).provide(Server.default, Hash.live)
  override def run: ZIO[Any, Throwable, Nothing] = {
    val httpApp = routes
    (MigrationService.RunMigrate *>
      Server
      .serve(httpApp)
      .flatMap(port =>
        Console.printLine(s"Started server on port: $port")
      ) *> ZIO.never)
      .provide(
        serverConfig,
        nettyConfig,
        Server.live,
        Hash.live,
        MigrationServiceImpl.layer,
        Quill.DataSource.fromPrefix("db"),
        PersistentUserRepo.layer,
        PersistentAccountRepo.layer,
        UserServiceImpl.layer
      )
  }
}
