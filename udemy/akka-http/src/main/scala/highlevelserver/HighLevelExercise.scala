package highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import highlevelserver.HighLevelExercise.Person
import spray.json._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

trait PersonJsonProtocol extends DefaultJsonProtocol {
  implicit val guitarFormat: RootJsonFormat[Person] = jsonFormat2(Person)
}

object HighLevelExercise extends App with PersonJsonProtocol {

  implicit val system: ActorSystem = ActorSystem("HighLevelExercise")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import system.dispatcher

  case class Person(pin: Int, name: String)

  val people = mutable.MutableList(
    Person(1, "Alice"),
    Person(2, "Bob"),
    Person(3, "Charlie")
  )

  val personServerRoute =
    pathPrefix("api" / "people") {
      get {
        (path(IntNumber) | parameter('pin.as[Int])) { pin =>
          people.find(_.pin == pin).fold(complete(StatusCodes.NotFound)) { person =>
            complete(
              HttpEntity(
                ContentTypes.`application/json`,
                person.toJson.prettyPrint
              )
            )
          }
        } ~
          pathEndOrSingleSlash {
            complete(
              HttpEntity(
                ContentTypes.`application/json`,
                people.toList.toJson.prettyPrint
              )
            )
          }
      } ~
        (post & pathEndOrSingleSlash & extractRequest & extractLog) { (req, log) =>
          complete {
            req.entity.toStrict(2 seconds) map {
              _.data.utf8String.parseJson.convertTo[Person]
            } transformWith {
              case Success(person) =>
                log.info(s"Got person: $person")
                people += person
                Future(StatusCodes.Created)
              case Failure(ex) =>
                log.error(ex, "An error occurred")
                Future(StatusCodes.InternalServerError)
            } recoverWith {
              case _ => Future(StatusCodes.InternalServerError)
            }
          }
        }
    }

  Http().bindAndHandle(personServerRoute, "localhost", 8080)

}
