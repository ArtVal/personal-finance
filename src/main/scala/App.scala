import auth.Authentication
import config.HttpServerConfig
import db.{MigrationService, MigrationServiceImpl}
import io.getquill.jdbczio.Quill
import users.{PersistentUserRepo, User, UserRepo}
import zio.config.typesafe.TypesafeConfigProvider
import zio.crypto.hash.Hash
import zio.http.netty.NettyConfig
import zio.http.{HttpApp, Response, Server}
import zio.{Config, Console, Ref, Runtime, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

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
  val routes: HttpApp[Hash with UserRepo] = Authentication()
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
        PersistentUserRepo.layer


        // A layer responsible for storing the state of the `counterApp`
//        ZLayer.fromZIO(Ref.make(0)),

        // To use the persistence layer, provide the `PersistentUserRepo.layer` layer instead
        //      InmemoryUserRepo.layer
      )
  }
//    app
//    .provide(MigrationServiceImpl.layer, zioDS)

}
