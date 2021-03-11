package co.s4ncampus.fpwithscala.users.infraestructure.repository

import co.s4ncampus.fpwithscala.users.domain._

import cats.data._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import cats.effect.Bracket

private object UserSQL {

  def insert(user: User): Update0 = sql"""
    INSERT INTO USERS (LEGAL_ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE)
    VALUES (${user.legalId}, ${user.firstName}, ${user.lastName}, ${user.email}, ${user.phone})
  """.update

  def selectByLegalId(legalId: String): Query0[User] = sql"""
    SELECT ID, LEGAL_ID, FIRST_NAME, LAST_NAME, EMAIL, PHONE
    FROM USERS
    WHERE LEGAL_ID = $legalId
  """.query[User]

  def update(legalId: String,user: User): Update0 = sql"""
    UPDATE USERS
    SET ID = ${user.id}
    SET FIRST_NAME = ${user.firstName}
    SET LAST_NAME = ${user.lastName}
    SET EMAIL = ${user.email}
    SET PHONE = ${user.phone}
    WHERE LEGAL_ID = $legalId
       """.update

  def delete(legalId: String): Update0 = sql"""
    DELETE
    FROM USERS
    WHERE LEGAL_ID = $legalId
  """.update

}

class DoobieUserRepositoryInterpreter[F[_]: Bracket[?[_], Throwable]](val xa: Transactor[F])
    extends UserRepositoryAlgebra[F] {
  import UserSQL._

  def create(user: User): F[User] = 
    insert(user).withUniqueGeneratedKeys[Long]("ID").map(id => user.copy(id = id.some)).transact(xa)

  def findByLegalId(legalId: String): OptionT[F, User] = OptionT(selectByLegalId(legalId).option.transact(xa))

  def updateUser(legalId: String,user:User): F[Int] = update(legalId,user).run.transact(xa)

  def deleteUser(legalId: String) : F[Int] = delete(legalId).run.transact(xa)
}

object DoobieUserRepositoryInterpreter {
  def apply[F[_]: Bracket[?[_], Throwable]](xa: Transactor[F]): DoobieUserRepositoryInterpreter[F] =
    new DoobieUserRepositoryInterpreter[F](xa)
}