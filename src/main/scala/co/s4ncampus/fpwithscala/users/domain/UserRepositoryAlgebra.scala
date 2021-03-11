package co.s4ncampus.fpwithscala.users.domain

import cats.data.{OptionT}


trait UserRepositoryAlgebra[F[_]] {
  def create(user: User): F[User]
  def findByLegalId(legalId: String): OptionT[F, User]
  //def updateUser(legalId:String, user: User):F[Int]
  def updateUser(user: User): OptionT[F, User]
  def deleteUser(legalId: String) : OptionT[F, User]
}