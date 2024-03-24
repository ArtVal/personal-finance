package users

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class User(id: Int, login: String, password: String)

object User {
  implicit val encoder: JsonEncoder[User] =
    DeriveJsonEncoder.gen[User]
  implicit val decoder: JsonDecoder[User] =
    DeriveJsonDecoder.gen[User]
}
