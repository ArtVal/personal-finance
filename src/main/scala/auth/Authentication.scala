package auth

import accounts.AccountRepo
import db.Ctx.transaction
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import users.UserRepo
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

  case class User(login: String, password: String)
  object User {
    implicit val userJsonDecoder: JsonDecoder[User] = DeriveJsonDecoder.gen[User]
    implicit val userJsonEncoder: JsonEncoder[User] = DeriveJsonEncoder.gen[User]
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

  def hashPassword(password: String): RIO[Hash, MessageDigest[Chunk[Byte]]] = Hash.hash[HashAlgorithm.SHA256](
    m = Chunk.fromArray(password.getBytes)
  )

  def verifyPassword(password: String, hash: String): RIO[Hash, Boolean] =
    Hash.verify[HashAlgorithm.SHA256](
      Chunk.fromArray(hex2binary(string2hex(password))),
      MessageDigest(Chunk.fromArray(hex2binary(hash))))


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

  def hashPwd(pwd: String): ZIO[Hash, Throwable, String] =
    hashPassword(pwd).map(digest => binary2hex(digest.value.toArray))
  // Http app that is accessible only via a jwt token
  def apply(): HttpApp[AccountRepo with UserRepo with DataSource with Hash] = Routes(
    Method.POST / "login" ->
      handler { req: Request =>
        for {
          body <- req.body.asString.orDie
          entity <- ZIO.fromEither(body.fromJson[User])
          user <- UserRepo.lookupByLogin(entity.login).flatMap {
            case Some(value) => ZIO.succeed(value)
            case None => ZIO.fail("User not found")
          }
          verified <- verifyPassword(entity.password, user.password)
        } yield {
          if(verified)
            Response.text("login success")
              .addCookie(
                Cookie.Response(
                  name = "session_token",
                  content = jwtEncode(user.login),
                  path = Some(Path("/")),
                  isHttpOnly = true))
          else Response.text("Invalid username or password.").status(Status.Unauthorized)
        }
      },
    Method.POST / "register" -> handler { req: Request =>
     for {
        body <- req.body.asString.orDie
        entity <- ZIO.fromEither(body.fromJson[User])
        hash <- hashPwd(entity.password)
        id <- transaction{
          for {
            id <- UserRepo.register(users.User(0, entity.login, hash))
            _ <- AccountRepo.register(id)
          } yield {
            id
          }
        }
      } yield {
        Response.json(users.User(id, entity.login, hash).toJson)
      }
    },
    Method.GET / "user" / string("name") / "greet" -> auth -> handler { (name: String, auth: AuthData, req: Request) =>
      Response.text(s"Welcome to the ZIO party! $name ${auth.login}")
    },
    Method.GET / "hello" -> auth -> handler{ (authData: AuthData, _: Request)  =>
      Response.text(s"hello ${authData.login}")}
  ).handleError{error => Response.badRequest(error.toString)}.toHttpApp

  def string2hex(str: String): String = {
    str.toList.map(_.toInt.toHexString).mkString
  }

  def hex2string(hex: String): String = {
    hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toChar).mkString
  }

  // convert hex bytes string to normal string
  def hex2binary(hex: String): Array[Byte] = {
    hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).byteValue)
  }

  def binary2hex(bytes: Array[Byte]): String =  {
//    bytes.map(_.toHexString).mkString
      bytes.map("%02X" format _).mkString
  }

}
