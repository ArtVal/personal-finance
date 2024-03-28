package services

import io.getquill.mirrorContextWithQueryProbing.transaction
import services.AccountServiceImpl.{newAddBalance, newDeleteBalance}
import storages.accounts.{Account, AccountRepo}
import storages.categories.{Category, CategoryRepo}
import storages.operations.{Operation, OperationRepo}
import zio.{Task, ZIO, ZLayer}

import javax.sql.DataSource

trait AccountService {
  def addOperation(operation: Operation): Task[Account]
  def deleteOperation(operationId: Int): Task[Unit]
  def addCategory(category: Category): Task[Unit]
  def deleteCategory(categoryId: Int): Task[Unit]
}

case class AccountServiceImpl(ds: DataSource, accountRepo: AccountRepo, operationRepo: OperationRepo, categoryRepo: CategoryRepo) extends AccountService {
  override def addOperation(operation: Operation): Task[Account] = transaction{
    for {
      oldBalance <- accountRepo.lookup(operation.id)
        .flatMap(b => ZIO.getOrFail(b).mapError(e => new Exception("account not found", e)))
      newBalance <- newAddBalance(oldBalance.balance, operation)
      _ <- operationRepo.add(operation)
      account <- accountRepo.updateAccount(newBalance)
    } yield account
  }

  override def deleteOperation(operationId: Int): Task[Unit] = transaction {
    for {
      operation <- operationRepo.lookup(operationId).flatMap(ZIO.getOrFail[Operation](_)).mapError(e => new Exception("operation not found", e))
      oldBalance <- accountRepo.lookup(operation.id).flatMap(ZIO.getOrFail[Account](_)).mapError(e => new Exception("account not found", e))
      newBalance <- newDeleteBalance(oldBalance.balance, operation)
      _ <- operationRepo.delete(operationId)
      account <- accountRepo.updateAccount(newBalance)
    } yield account
  }

  override def addCategory(category: Category): Task[Unit] = categoryRepo.add(category).map(_ => ())

  override def deleteCategory(categoryId: Int): Task[Unit] = transaction{
    categoryRepo.delete(categoryId).mapError(e => new Exception("category used", e))
  }
}

object AccountServiceImpl {
  def layer: ZLayer[DataSource with AccountRepo with OperationRepo with CategoryRepo, Nothing, AccountServiceImpl] =
    ZLayer.fromFunction(AccountServiceImpl(_,_,_,_))

  private def newAddBalance(oldBalance: Double, operation: Operation): Task[Account] = {
    val balance = if (operation.id == 1) oldBalance + operation.amount else oldBalance - operation.amount
    if (balance < 0) ZIO.fail(new Exception("insufficient funds"))
    else ZIO.succeed(Account(operation.accountId, balance))
  }

  private def newDeleteBalance(oldBalance: Double, operation: Operation): Task[Account] = {
    val balance = if (operation.id == 1) oldBalance - operation.amount else oldBalance + operation.amount
    if (balance < 0) ZIO.fail(new Exception("insufficient funds"))
    else ZIO.succeed(Account(operation.accountId, balance))
  }
}



object AccountService {
  def addOperation(operation: Operation): ZIO[AccountService, Throwable, Account] =
    ZIO.serviceWithZIO[AccountService](_.addOperation(operation))
  def deleteOperation(operationId: Int): ZIO[AccountService, Throwable, Unit] =
    ZIO.serviceWithZIO[AccountService](_.deleteOperation(operationId))
  def addCategory(category: Category): ZIO[AccountService, Throwable, Unit] =
    ZIO.serviceWithZIO[AccountService](_.addCategory(category))
  def deleteCategory(categoryId: Int): ZIO[AccountService, Throwable, Unit] =
    ZIO.serviceWithZIO[AccountService](_.deleteCategory(categoryId: Int))
}