package accounts

import zio.{Task, ZIO}

trait AccountRepo {
  def register(userId: Int): Task[Int]

  def lookup(userId: Int): Task[Option[Double]]

  def updateAccount(userId: Int, balance: Double): Task[Double]
}

object AccountRepo {

  def register(userId: Int): ZIO[AccountRepo, Throwable, Int] =
    ZIO.serviceWithZIO[AccountRepo](_.register(userId))

  def lookup(userId: Int): ZIO[AccountRepo, Throwable, Option[Double]] =
    ZIO.serviceWithZIO[AccountRepo](_.lookup(userId))

  def updateAccount(userId: Int, balance: Double): ZIO[AccountRepo, Throwable, Double] =
    ZIO.serviceWithZIO[AccountRepo](_.updateAccount(userId, balance))
}
