package accounts

import auth.Authentication.{AuthData, auth}
import users.{User, UserRepo}
import zio.ZIO
import zio.http.{HttpApp, Method, Request, Response, Routes, handler}
import zio.http.codec.PathCodec.string
import zio.json.EncoderOps

object Accounts {
  def apply(): HttpApp[AccountRepo with UserRepo] = Routes(
    Method.GET / "account"  -> auth -> handler { (auth: AuthData, req: Request) =>
      for {
        user <- UserRepo.lookupByLogin(auth.login)
          .flatMap{
            case Some(value) => ZIO.succeed(value)
            case None => ZIO.fail("user not found")
          }
        balance <- AccountRepo.lookup(user.id)
          .flatMap{
            case Some(value) => ZIO.succeed(value)
            case None => ZIO.fail("Not found!")
        }
      } yield Response.json(balance.toJson)
    },
    Method.POST / "account" -> auth -> handler {  (auth: AuthData, req: Request) =>
      ???
    }
  ).handleError(err => Response.notFound(err.toString)).toHttpApp

}
