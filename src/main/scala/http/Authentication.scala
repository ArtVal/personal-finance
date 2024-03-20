package http

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.http.Middleware.customAuthProviding
import zio.http._
import zio.http.codec.PathCodec.string
import zio.json._

import java.time.Clock

object Authentication {

  // Secret Authentication key
  val SECRET_KEY = "secretKey"

  implicit val clock: Clock = Clock.systemUTC

  case class AuthData(login: String)
  object AuthData {
    implicit val decoder: JsonDecoder[AuthData] =
      DeriveJsonDecoder.gen[AuthData]
  }

  // Helper to encode the JWT token
  def jwtEncode(username: String): String = {
    val json  = s"""{"login": "${username}"}"""
    val claim = JwtClaim {
      json
    }.issuedNow.expiresIn(300)
    Jwt.encode(claim, SECRET_KEY, JwtAlgorithm.HS512)
  }

  // Helper to decode the JWT token
  def jwtDecode(token: String): Option[JwtClaim] = {
    Jwt.decode(token, SECRET_KEY, Seq(JwtAlgorithm.HS512)).toOption
  }

  def login: HttpApp[Any] =
    Routes(
      Method.GET / "login" / string("username") / string("password") ->
        handler { (username: String, password: String, req: Request) =>
          if (password.reverse.hashCode == username.hashCode) Response.text("login success")
            .addCookie(
              Cookie.Response(
                name = "session_token",
                content = jwtEncode(username),
                path = Some(Path("/")),
                isHttpOnly = true))
          else Response.text("Invalid username or password.").status(Status.Unauthorized)
        },
    ).toHttpApp

  def auth: HandlerAspect[Any, AuthData] = {
    customAuthProviding[AuthData](authData)
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
  def user: HttpApp[Any] = Routes(
    Method.GET / "user" / string("name") / "greet" -> auth -> handler { (name: String, auth: AuthData, req: Request) =>
      Response.text(s"Welcome to the ZIO party! ${name} ${auth.login}")
    },
  ).toHttpApp

}
