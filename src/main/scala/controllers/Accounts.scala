package controllers


import common.Common.{AuthData, auth}
import services.AccountService
import storages.accounts.{Account, AccountRepo}
import storages.categories.{Category, CategoryRepo}
import storages.operations.{Operation, OperationRepo}
import storages.users.{User, UserRepo}
import zio.ZIO
import zio.http.codec.PathCodec.{int, string}
import zio.http.{HttpApp, Method, Request, Response, Routes, handler}
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}

import java.net.URLDecoder
import java.time.Instant

object Accounts {
  def apply(): HttpApp[CategoryRepo with AccountService with UserRepo with OperationRepo with AccountRepo] = Routes(
    Method.GET / "account"  -> auth -> handler { (auth: AuthData, req: Request) =>
      for {
        user <- UserRepo.lookupByLogin(auth.login).flatMap(ZIO.getOrFail[User](_))
        balance <- AccountRepo.lookup(user.id).flatMap(ZIO.getOrFail[Account](_))
      } yield Response.json(balance.toJson)
    },
    Method.GET / "account" / "operation" / string("from") / string("to") -> auth -> handler{
      (from: String, to: String, auth: AuthData, req: Request) =>
        for {
          begin <- ZIO.attempt(Instant.parse(URLDecoder.decode(from, "utf-8")))
          end <- ZIO.attempt(Instant.parse(URLDecoder.decode(to, "utf-8")))
          user <- UserRepo.lookupByLogin(auth.login).flatMap(ZIO.getOrFail[User](_))
          operations <- OperationRepo.list(user.id, begin, end)
        } yield Response.json(operations.toJson)
    },
    Method.POST / "account" / "operation" -> auth -> handler{(auth: AuthData, req: Request) =>
      for {
        body <- req.body.asString.orDie
        entity <- ZIO.fromEither(body.fromJson[UserOperation]).mapError(new Exception(_))
        user <- UserRepo.lookupByLogin(auth.login).flatMap(ZIO.getOrFail[User](_))
        account <- AccountService.addOperation(Operation(0,user.id, entity.categoryId, entity.amount, Instant.now()))
      } yield Response.json(account.toJson)
    },
    Method.DELETE / "account" / "operation" / int("operationId") -> auth -> handler{
      (operationId: Int, auth: AuthData, req: Request) =>
        for {
          user <- UserRepo.lookupByLogin(auth.login).flatMap(ZIO.getOrFail[User](_))
          _ <- AccountService.deleteOperation(operationId, user.id)
        } yield Response.ok
    },
    Method.GET / "category" -> auth -> handler{(auth: AuthData, req: Request)  =>
      for {
        categories <- CategoryRepo.list()
      } yield Response.json(categories.toJson)
    },
    Method.POST / "category" -> auth -> handler{(auth: AuthData, req: Request)  =>
      for {
        body <- req.body.asString.orDie
        entity <- ZIO.fromEither(body.fromJson[AddCategory]).mapError(new Exception(_))
        category <- AccountService.addCategory(Category(0, entity.name))
      } yield Response.json(category.toJson)
    },
    Method.DELETE / "category" / int("categoryId") -> auth -> handler{(categoryId: Int, auth: AuthData, req: Request) =>
      for {
        _ <- AccountService.deleteCategory(categoryId)
      } yield Response.ok
    }
  ).handleError((err: Throwable) => Response.badRequest(err.toString)).toHttpApp
}

case class UserOperation(categoryId: Int, amount: Double)
object UserOperation {
  implicit val encoder: JsonEncoder[UserOperation] =
    DeriveJsonEncoder.gen[UserOperation]
  implicit val decoder: JsonDecoder[UserOperation] =
    DeriveJsonDecoder.gen[UserOperation]
}

case class AddCategory(name: String) extends AnyVal
object AddCategory {
  implicit val encoder: JsonEncoder[AddCategory] =
    DeriveJsonEncoder.gen[AddCategory]
  implicit val decoder: JsonDecoder[AddCategory] =
    DeriveJsonDecoder.gen[AddCategory]
}
