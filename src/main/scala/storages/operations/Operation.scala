package storages.operations

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.time.Instant

case class Operation(id: Int, accountId: Int, categoryId: Int, amount: Double, created: Instant)

object Operation{
  implicit val encoder: JsonEncoder[Operation] =
    DeriveJsonEncoder.gen[Operation]
  implicit val decoder: JsonDecoder[Operation] =
    DeriveJsonDecoder.gen[Operation]
}
