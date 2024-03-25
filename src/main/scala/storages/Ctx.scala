package storages

import io.getquill.{Escape, NamingStrategy, PostgresZioJdbcContext, SnakeCase}

object Ctx extends PostgresZioJdbcContext(NamingStrategy(Escape, SnakeCase))
