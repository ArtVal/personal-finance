package http

import java.time.Clock

import zio._

import zio.http._
import zio.http.Middleware.bearerAuth
import zio.http.codec.PathCodec.string

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

class Authentication {

}
