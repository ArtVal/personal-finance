package accounts

import users.User
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class Account(userId: Int, balance: Double)

object Account {
  implicit val encoder: JsonEncoder[Account] =
    DeriveJsonEncoder.gen[Account]
  implicit val decoder: JsonDecoder[Account] =
    DeriveJsonDecoder.gen[Account]
}
