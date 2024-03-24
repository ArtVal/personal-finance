package db

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.jdbczio.Quill
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.{ClassLoaderResourceAccessor, CompositeResourceAccessor, DirectoryResourceAccessor}
import zio.{Scope, ZIO, ZLayer}

import java.nio.file.Paths
import javax.sql.DataSource

trait MigrationService {

  def RunMigrate: ZIO[Any, Throwable, Unit]
}

case class MigrationServiceImpl(dataSource: DataSource) extends MigrationService {
  override def RunMigrate: ZIO[Any, Throwable, Unit] = {
    ZIO.log("migration started") *> ZIO.scoped {
      mkLiquibase.map(_.update())
    } *> ZIO.log("migration completed")
  }

  private def mkLiquibase: ZIO[Any with Scope, Throwable, Liquibase] = {
    for {
      classLoaderAccessor <- ZIO.acquireRelease(ZIO.attempt(new ClassLoaderResourceAccessor()))(accessor => ZIO.succeed(accessor.close()))
      fileOpener <- ZIO.acquireRelease(
        ZIO.attempt(
          new CompositeResourceAccessor(
            classLoaderAccessor,
            new DirectoryResourceAccessor(Paths.get("")))))(accessor => ZIO.succeed(accessor.close()))
      jdbcConn <- ZIO.acquireRelease(
        ZIO.log("liquibase opened a connection to the database") *>
          ZIO.attempt(new JdbcConnection(dataSource.getConnection()))) { conn =>
        ZIO.log("liquibase closed the connection to the database") *>
          ZIO.succeed(conn.close())
      }
      liqui <- ZIO.attempt(new Liquibase(LiquibaseConfig("src/main/migrations/changelog.xml").changeLog, fileOpener, jdbcConn))
    } yield liqui
  }
}

object MigrationServiceImpl {

  val layer: ZLayer[DataSource, Nothing, MigrationServiceImpl] =
    ZLayer.fromFunction(MigrationServiceImpl(_))
}

object MigrationService {
  def RunMigrate: ZIO[MigrationService, Throwable, Unit] = ZIO.serviceWithZIO[MigrationService](_.RunMigrate)
}