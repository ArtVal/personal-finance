package accounts

import zio.{Task, ZIO}

trait AccountRepo {
  def register(userId: Int): Task[Int]

  def lookup(userId: Int): Task[Option[Account]]

  def updateAccount(account: Account): Task[Account]
}

object AccountRepo {

  def register(userId: Int): ZIO[AccountRepo, Throwable, Int] =
    ZIO.serviceWithZIO[AccountRepo](_.register(userId))

  def lookup(userId: Int): ZIO[AccountRepo, Throwable, Option[Account]] =
    ZIO.serviceWithZIO[AccountRepo](_.lookup(userId))

  def updateAccount(account: Account): ZIO[AccountRepo, Throwable, Account] =
    ZIO.serviceWithZIO[AccountRepo](_.updateAccount(account))
}
