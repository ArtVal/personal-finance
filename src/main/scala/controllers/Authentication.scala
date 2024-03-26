package controllers

import common.Common.{AuthData, auth, jwtEncode}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import services.UserService
import zio.ZIO
import zio.crypto.hash.Hash
import zio.http._
import zio.http.codec.PathCodec.string
import zio.json._

import java.time.Clock

object Authentication {

  case class LoginCredentials(login: String, password: String)
  object LoginCredentials {
    implicit val userJsonDecoder: JsonDecoder[LoginCredentials] = DeriveJsonDecoder.gen[LoginCredentials]
    implicit val userJsonEncoder: JsonEncoder[LoginCredentials] = DeriveJsonEncoder.gen[LoginCredentials]
  }

  // Http app that is accessible only via a jwt token
  def apply(): HttpApp[UserService with Hash] = Routes(
    Method.POST / "login" ->
      handler { req: Request =>
        for {
          body <- req.body.asString.orDie
          entity <- ZIO.fromEither(body.fromJson[LoginCredentials])
          verified <- UserService.verify(entity)
        } yield {
          if(verified)
            Response.text("login success")
              .addCookie(
                Cookie.Response(
                  name = "session_token",
                  content = jwtEncode(entity.login),
                  path = Some(Path("/")),
                  isHttpOnly = true))
          else Response.text("Invalid username or password.").status(Status.Unauthorized)
        }
      },
    Method.POST / "register" -> handler { req: Request =>
     for {
        body <- req.body.asString.orDie
        entity <- ZIO.fromEither(body.fromJson[LoginCredentials])
        register <-  UserService.register(entity)
      } yield {
        Response.json(register.toJson)
      }
    },
    Method.GET / "user" / string("name") / "greet" -> auth -> handler { (name: String, auth: AuthData, req: Request) =>
      Response.text(s"Welcome to the ZIO party! $name ${auth.login}")
    },
    Method.GET / "hello" -> auth -> handler{ (authData: AuthData, _: Request)  =>
      Response.text(s"hello ${authData.login}")}
  ).handleError{error => Response.badRequest(error.toString)}.toHttpApp



}
