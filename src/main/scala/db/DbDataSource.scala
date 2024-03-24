package db

import com.zaxxer.hikari.HikariDataSource
import io.getquill.JdbcContextConfig
import io.getquill.context.ZioJdbc
import io.getquill.util.LoadConfig
import zio.ZLayer

import javax.sql.DataSource

object DbDataSource {
//  def hikariDS: HikariDataSource = JdbcContextConfig(LoadConfig("db")).dataSource
//  val zioDS: ZLayer[Any, Throwable, DataSource] = ZLayer.succeed(hikariDS)
}
