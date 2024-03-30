package storages.categories

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class Category (id: Int, categoryName: String, readOnly: Boolean = false)

object Category {
  implicit val encoder: JsonEncoder[Category] =
    DeriveJsonEncoder.gen[Category]
  implicit val decoder: JsonDecoder[Category] =
    DeriveJsonDecoder.gen[Category]
}

