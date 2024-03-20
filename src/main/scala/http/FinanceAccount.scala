package http

import http.Authentication.{AuthData, auth}
import zio.http.{HttpApp, Method, Request, Response, Routes, handler}

object FinanceAccount {

  def routes: HttpApp[Any] = Routes(
    Method.GET / "hello" -> auth -> handler{ (authData: AuthData, _: Request)  =>
      Response.text(s"hello ${authData.login}")}
  ).toHttpApp

}
