package db

import io.getquill.{Escape, Literal, NamingStrategy, PostgresZioJdbcContext}
object Ctx extends PostgresZioJdbcContext(NamingStrategy(Escape, Literal))
