package common

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.http.{HandlerAspect, Request}
import zio.http.Middleware.customAuthProviding
import zio.json.{DecoderOps, DeriveJsonDecoder, JsonDecoder}

import java.time.Clock

object Common {

  // Secret Authentication key
  val SECRET_KEY = "secretKey"

  implicit val clock: Clock = Clock.systemUTC

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

  case class AuthData(login: String)
  object AuthData {
    implicit val decoder: JsonDecoder[AuthData] =
      DeriveJsonDecoder.gen[AuthData]
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

  def auth: HandlerAspect[Any, AuthData] = {
    customAuthProviding[AuthData](authData)
  }


}
