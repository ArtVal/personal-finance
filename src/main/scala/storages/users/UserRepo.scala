package storages.users

import zio.{Task, ZIO}

trait UserRepo {
  def register(user: User): Task[Int]

  def lookup(id: Int): Task[Option[User]]

  def lookupByLogin(login: String): Task[Option[User]]

  def users: Task[List[User]]
}

object UserRepo {
  def register(user: User): ZIO[UserRepo, Throwable, Int] =
    ZIO.serviceWithZIO[UserRepo](_.register(user))

  def lookup(id: Int): ZIO[UserRepo, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepo](_.lookup(id))

  def lookupByLogin(login: String): ZIO[UserRepo, Throwable, Option[User]] =
    ZIO.serviceWithZIO[UserRepo](_.lookupByLogin(login))

  def users: ZIO[UserRepo, Throwable, List[User]] =
    ZIO.serviceWithZIO[UserRepo](_.users)
}
