package storages.operations

import java.time.Instant

case class Operation(id: Int, accountId: Int, categoryId: Int, amount: Double, created: Instant)
