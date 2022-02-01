package highlevelserver

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import lowlevel.rest._
import spray.json._

import scala.concurrent.duration._
import scala.language.postfixOps

object HighLevelExample extends App with GuitarStoreJsonProtocol {

  implicit val system: ActorSystem = ActorSystem("HighLevelExample")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import system.dispatcher


  val guitarDb = system.actorOf(Props[GuitarDB], "LowLevelGuitarDB")

  List(
    Guitar("Fender", "Stratocaster"),
    Guitar("Gibson", "Les Paul"),
    Guitar("Martin", "LX1")
  ) foreach { guitar =>
    guitarDb ! CreateGuitar(guitar)
  }

  implicit val defaultTimeout: Timeout = Timeout(2 seconds)

  val guitarServerRoutes =
    path("api" / "guitars") {
      parameter('id.as[Int]) { guitarId =>
        get {
          complete {
            (guitarDb ? FindGuitar(guitarId))
              .mapTo[Option[Guitar]]
              .map {
                case Some(guitar) =>
                  HttpEntity(
                    ContentTypes.`application/json`,
                    guitar.toJson.prettyPrint
                  )
                case None =>
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    "Not Found"
                  )
              }
          }
        }
      } ~
        get {
          complete {
            (guitarDb ? FindAllGuitars)
              .mapTo[List[Guitar]]
              .map { guitars =>
                HttpEntity(
                  ContentTypes.`application/json`,
                  guitars.toJson.prettyPrint
                )
              }
          }
        }

    } ~
      path("api" / "guitars" / IntNumber) { guitarId =>
        get {
          complete {
            (guitarDb ? FindGuitar(guitarId))
              .mapTo[Option[Guitar]]
              .map {
                case Some(guitar) =>
                  HttpEntity(
                    ContentTypes.`application/json`,
                    guitar.toJson.prettyPrint
                  )
                case None =>
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    "Not Found"
                  )
              }
          }
        }
      } ~
      path("api" / "guitars" / "inventory") {
        get {
          parameter('inStock.as[Boolean]) { inStock =>
            complete {
              (guitarDb ? FindGuitarsInStock(inStock))
                .mapTo[List[Guitar]]
                .map { guitars =>
                  HttpEntity(
                    ContentTypes.`application/json`,
                    guitars.toJson.prettyPrint
                  )
                }
            }
          }
        }
      }

  val simplifiedGuitarServerRoutes =
    pathPrefix("api" / "guitars") {
      (path("inventory") & get) {
        parameter('inStock.as[Boolean]) { inStock =>
          complete {
            (guitarDb ? FindGuitarsInStock(inStock))
              .mapTo[List[Guitar]]
              .map { guitars =>
                HttpEntity(
                  ContentTypes.`application/json`,
                  guitars.toJson.prettyPrint
                )
              }
          }
        }
      } ~
        (path(IntNumber) | parameter('id.as[Int])) { guitarId =>
          complete {
            (guitarDb ? FindGuitar(guitarId))
              .mapTo[Option[Guitar]]
              .map {
                case Some(guitar) =>
                  HttpEntity(
                    ContentTypes.`application/json`,
                    guitar.toJson.prettyPrint
                  )
                case None =>
                  HttpEntity(
                    ContentTypes.`text/plain(UTF-8)`,
                    "Not Found"
                  )
              }
          }
        }
    } ~
      pathEndOrSingleSlash {
        complete {
          (guitarDb ? FindAllGuitars)
            .mapTo[List[Guitar]]
            .map { guitars =>
              HttpEntity(
                ContentTypes.`application/json`,
                guitars.toJson.prettyPrint
              )
            }
        }
      }

  Http().bindAndHandle(simplifiedGuitarServerRoutes, "localhost", 8080)

}
