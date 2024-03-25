package controllers

import common.Common.auth
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import services.UserService
import services.UserServiceImpl.verifyPassword
import storages.Ctx.transaction
import storages.accounts.AccountRepo
import storages.users
import storages.users.UserRepo
import zio.crypto.hash.{Hash, HashAlgorithm, MessageDigest}
import zio.http.Middleware.customAuthProviding
import zio.http._
import zio.http.codec.PathCodec.string
import zio.json._
import zio.{Chunk, RIO, ZIO}

import java.time.Clock
import javax.sql.DataSource

object Authentication {

  // Secret Authentication key
  val SECRET_KEY = "secretKey"

  implicit val clock: Clock = Clock.systemUTC

  case class AuthData(login: String)
  object AuthData {
    implicit val decoder: JsonDecoder[AuthData] =
      DeriveJsonDecoder.gen[AuthData]
  }

  case class LoginCredentials(login: String, password: String)
  object LoginCredentials {
    implicit val userJsonDecoder: JsonDecoder[LoginCredentials] = DeriveJsonDecoder.gen[LoginCredentials]
    implicit val userJsonEncoder: JsonEncoder[LoginCredentials] = DeriveJsonEncoder.gen[LoginCredentials]
  }

  // Helper to encode the JWT token
  def jwtEncode(username: String): String = {
    val json  = s"""{"login": "$username"}"""
    val claim = JwtClaim {
      json
    }.issuedNow.expiresIn(300)
    Jwt.encode(claim, SECRET_KEY, JwtAlgorithm.HS512)
  }

  // Helper to decode the JWT token
  def jwtDecode(token: String): Option[JwtClaim] = {
    Jwt.decode(token, SECRET_KEY, Seq(JwtAlgorithm.HS512)).toOption
  }

  def authData(req: Request): Option[AuthData] = {
    req.cookie("session_token")
      .map(_.content)
      .flatMap(jwtDecode)
      .map(_.content)
      .map(_.fromJson[AuthData])
      .flatMap(_.toOption)
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
