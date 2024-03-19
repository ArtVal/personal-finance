package http

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.http.Middleware.customAuth
import zio.http._
import zio.http.codec.PathCodec.string

import java.time.Clock

object Authentication {

  // Secret Authentication key
  val SECRET_KEY = "secretKey"

  implicit val clock: Clock = Clock.systemUTC

  // Helper to encode the JWT token
  def jwtEncode(username: String): String = {
    val json  = s"""{"user": "${username}"}"""
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

  def cookieAuth(f: String => Boolean): HandlerAspect[Any, Unit] =
    customAuth(
      _.cookie("session_token") match {
        case Some(cookie) => f(cookie.content)
        case _                                        => false
      }
    )

  // Http app that is accessible only via a jwt token
  def user: HttpApp[Any] = Routes(
    Method.GET / "user" / string("name") / "greet" -> handler { (name: String, req: Request) =>
      Response.text(s"Welcome to the ZIO party! ${name}")
    },
  ).toHttpApp @@ cookieAuth(jwtDecode(_).isDefined)

}
