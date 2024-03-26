package storages.categories

import io.getquill.{EntityQuery, Quoted}
import storages.Ctx
import zio.{Task, ZLayer}

import javax.sql.DataSource

case class PersistentCategoryRepo(ds: DataSource) extends CategoryRepo {

  val ctx: Ctx.type = Ctx
  import ctx._
  private lazy val categorySchema: Quoted[EntityQuery[Category]] = quote {
    querySchema[Category]("public.category")
  }

  override def list(): Task[Seq[Category]] =
    ctx.run(categorySchema)
    .provide(ZLayer.succeed(ds))

  override def lookup(id: Int): Task[Option[Category]] =
    ctx.run{
      categorySchema
        .filter(c => c.id == lift(id))
    }
      .provide(ZLayer.succeed(ds))
      .map(_.headOption)

  override def lookupByName(name: String): Task[Option[Category]] =
    ctx.run{
        categorySchema
          .filter(c => c.categoryName == lift(name))
      }
      .provide(ZLayer.succeed(ds))
      .map(_.headOption)

  override def add(category: Category): Task[Category] =
    ctx.run {
      categorySchema.insertValue {
        lift(category)
      }.returningGenerated(_.id)
    }
      .provide(ZLayer.succeed(ds))
      .map(catId => category.copy(id = catId))

  override def delete(id: Int): Task[Unit] =
    ctx.run {
        categorySchema
          .filter(c => c.id == lift(id) && c.readOnly == lift(false))
          .delete
      }
      .provide(ZLayer.succeed(ds))
      .map(_ => ())
}
object PersistentCategoryRepo {
  def layer: ZLayer[DataSource, Nothing, PersistentCategoryRepo] =
    ZLayer.fromFunction(PersistentCategoryRepo(_))
}
