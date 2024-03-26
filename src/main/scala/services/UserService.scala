package services

import common.Common
import common.Common.{binary2hex, hex2binary}
import controllers.Authentication.LoginCredentials
import services.UserServiceImpl.{hashPassword, verifyPassword}
import storages.Ctx.transaction
import storages.accounts.AccountRepo
import storages.users
import storages.users.{User, UserRepo}
import zio.crypto.hash.{Hash, HashAlgorithm, MessageDigest}
import zio.{Chunk, RIO, Task, ZIO, ZLayer}

import javax.sql.DataSource

trait UserService {
  def register(user: LoginCredentials): Task[User]
  def verify(user: LoginCredentials): Task[Boolean]
}

case class UserServiceImpl(ds: DataSource, userRepo: UserRepo, accountRepo: AccountRepo, hash: Hash) extends UserService {
  override def register(user: LoginCredentials): Task[User] = transaction{
    for {
      hash <- hashPassword(user.password)
      id <- userRepo.register(users.User(0, user.login, hash))
      _ <- accountRepo.register(id)
    } yield {
      User(id, user.login, hash)
    }
  }.provide(ZLayer.succeed(hash), ZLayer.succeed(ds))

  override def verify(credentials: LoginCredentials): Task[Boolean] = {
    for {
      userOpt <- userRepo.lookupByLogin(credentials.login)
      verified <- userOpt
        .map{user => verifyPassword(credentials.password, user.password)}
        .getOrElse(ZIO.succeed(false))
    } yield verified
  }.provide(ZLayer.succeed(hash))
}

object UserServiceImpl {
  def layer: ZLayer[DataSource with UserRepo with AccountRepo with Hash, Nothing, UserServiceImpl] =
    ZLayer.fromFunction(UserServiceImpl(_, _, _, _))

  private def hashPassword(password: String): ZIO[Hash, Throwable, String] = Hash.hash[HashAlgorithm.SHA256](
    m = Chunk.fromArray(password.getBytes)
  ).map(digest => binary2hex(digest.value.toArray))

  def verifyPassword(password: String, hashPwd: String): RIO[Hash, Boolean] =
    Hash.verify[HashAlgorithm.SHA256](
      Chunk.fromArray(hex2binary(Common.string2hex(password))),
      MessageDigest(Chunk.fromArray(hex2binary(hashPwd))))
}
object UserService {
  def register(user: LoginCredentials): ZIO[UserService, Throwable, User] =
    ZIO.serviceWithZIO[UserService](_.register(user))

  def verify(user: LoginCredentials): ZIO[UserService, Throwable, Boolean] =
    ZIO.serviceWithZIO[UserService](_.verify(user))

}
