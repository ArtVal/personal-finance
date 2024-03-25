package common

import controllers.Authentication.{AuthData, authData}
import zio.http.HandlerAspect
import zio.http.Middleware.customAuthProviding

object Common {
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

  def auth: HandlerAspect[Any, AuthData] = {
    customAuthProviding[AuthData](authData)
  }


}
