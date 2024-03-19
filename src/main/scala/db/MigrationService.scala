package db

import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.{ClassLoaderResourceAccessor, CompositeResourceAccessor, DirectoryResourceAccessor, FileSystemResourceAccessor, ZipResourceAccessor}
import liquibase.servicelocator.LiquibaseService
import zio.{Scope, ULayer, ZIO, ZLayer}

import java.nio.file.Paths
import javax.sql.DataSource

trait MigrationService {

  def RunMigrate: ZIO[Any, Throwable, Unit]
}

case class MigrationServiceImpl(liqui: Liquibase) extends MigrationService {
  override def RunMigrate: ZIO[Any, Throwable, Unit] = ZIO.scoped(
    ZIO.attempt(liqui.update()))
}

object MigrationServiceImpl {
  val layer: ZLayer[Liquibase, Nothing, MigrationServiceImpl] = ZLayer.scoped{
    for {
      liqui <- ZIO.service[Liquibase]
    } yield MigrationServiceImpl(liqui)
  }
}

object MigrationService {
  def migrate: ZIO[MigrationService, Throwable, Unit] = ZIO.serviceWithZIO[MigrationService](_.RunMigrate)

  val liquibaseLayer: ZLayer[DataSource, Throwable, Liquibase] = ZLayer.scoped(
    for {
      liquibase <- mkLiquibase(LiquibaseConfig("src/main/migrations/changelog.xml"))
    } yield (liquibase)
  )
  def mkLiquibase(config: LiquibaseConfig): ZIO[DataSource with Scope, Throwable, Liquibase] = for {
    ds <- ZIO.environment[DataSource].map(_.get)
    classLoaderAccessor <- ZIO.acquireRelease(ZIO.attempt(new ClassLoaderResourceAccessor()))(accessor => ZIO.succeed(accessor.close()))
    fileOpener <- ZIO.acquireRelease(
      ZIO.attempt(
        new CompositeResourceAccessor(
          classLoaderAccessor,
          new DirectoryResourceAccessor(Paths.get("")))))(accessor => ZIO.succeed(accessor.close()))
    jdbcConn <- ZIO.acquireRelease(ZIO.attempt(new JdbcConnection(ds.getConnection()))){conn =>
      ZIO.succeed(conn.close())
    }
    liqui <- ZIO.attempt(new Liquibase(config.changeLog, fileOpener, jdbcConn))
  } yield liqui
}