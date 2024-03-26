package storages.categories

import zio.{Task, ZIO}

trait CategoryRepo {

  def list(): Task[Seq[Category]]
  def lookup(id: Int): Task[Option[Category]]
  def lookupByName(name: String): Task[Option[Category]]
  def add(category: Category): Task[Category]
  def delete(id: Int): Task[Unit]

}

object CategoryRepo {
  def list(): ZIO[CategoryRepo, Throwable, Seq[Category]] =
    ZIO.serviceWithZIO[CategoryRepo](_.list())
  def lookup(id: Int): ZIO[CategoryRepo, Throwable, Option[Category]] =
    ZIO.serviceWithZIO[CategoryRepo](_.lookup(id))
  def lookupByName(category: String): ZIO[CategoryRepo, Throwable, Option[Category]] =
    ZIO.serviceWithZIO[CategoryRepo](_.lookupByName(category))
  def add(category: Category): ZIO[CategoryRepo, Throwable, Category] =
    ZIO.serviceWithZIO[CategoryRepo](_.add(category))
  def delete(id: Int): ZIO[CategoryRepo, Throwable, Unit] =
    ZIO.serviceWithZIO[CategoryRepo](_.delete(id))
}
