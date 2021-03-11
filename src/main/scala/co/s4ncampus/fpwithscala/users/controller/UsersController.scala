package co.s4ncampus.fpwithscala.users.controller

import co.s4ncampus.fpwithscala.users.domain._
import cats.effect.Sync
import cats.syntax.all._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
import co.s4ncampus.fpwithscala.users.domain.User
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder

class UsersController[F[_]: Sync] extends Http4sDsl[F] {

    implicit val userDecoder: EntityDecoder[F, User] = jsonOf

    private def createUser(userService: UserService[F]): HttpRoutes[F] = 
        HttpRoutes.of[F] {
            case req @ POST -> Root =>
                val action = for {
                    user <- req.as[User]
                    result <- userService.create(user).value
                } yield result
                
                action.flatMap {
                    case Right(saved) => Ok(saved.asJson)
                    case Left(UserAlreadyExistsError(existing)) => Conflict(s"The user with legal id ${existing.legalId} already exists")
                }

        }
    private def updateUser(userService: UserService[F]): HttpRoutes[F] = {
        HttpRoutes.of[F] {
            case req@ PUT -> Root / id =>
                val action = for {
                    user <- req.as[User]
                    updated = user.copy(legalId = id)
                    result <- userService.updateUser(updated).value
                } yield result

                action.flatMap {
                    case Right(saved) => Ok(saved.asJson)
                    case Left(UserNotFoundError) => NotFound("User not found")
                }
        }
    }
    private def readUser(userService: UserService[F]): HttpRoutes[F] =
        HttpRoutes.of[F] {
            case GET -> Root / id =>
                val action = userService.read(id).value
                Ok(action)
        }

    private def deleteUser(userService: UserService[F]): HttpRoutes[F] =
        HttpRoutes.of[F] {
            case DELETE -> Root / legalId =>
                val action = for {
                    result <- userService.deleteUser(legalId).value
                } yield result
                action.flatMap {
                    case Some(user) => Ok(user)
                    case None => NotFound("User not found")
                }
        }

    def endpoints(userService: UserService[F]): HttpRoutes[F] = {
        //To convine routes use the function `<+>`
        createUser(userService) <+> updateUser(userService) <+> readUser(userService) <+> deleteUser(userService)
    }

}

object UsersController {
    def endpoints[F[_]: Sync](userService: UserService[F]): HttpRoutes[F] =
        new UsersController[F].endpoints(userService)
}