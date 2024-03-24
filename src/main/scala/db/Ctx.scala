package db

import io.getquill.{Escape, NamingStrategy, PostgresZioJdbcContext, SnakeCase}
object Ctx extends PostgresZioJdbcContext(NamingStrategy(Escape, SnakeCase))
