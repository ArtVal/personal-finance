package http

import http.Authentication.jwtDecode
import zio.http.{HttpApp, Method, Response, Routes, handler}

object FinanceAccount {

  def routes: HttpApp[Any] = Routes(
    Method.GET / "hello" -> handler(Response.text("hello"))
  ).toHttpApp@@Authentication.cookieAuth(jwtDecode(_).isDefined)

}
