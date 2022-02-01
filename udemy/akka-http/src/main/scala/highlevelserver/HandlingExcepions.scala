package highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import akka.stream.ActorMaterializer


object HandlingExcepions extends App {

  implicit val system: ActorSystem = ActorSystem("HandlingExcepions")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val simpleRoute =
    path("api" / "people") {
      get {
        throw new RuntimeException("boom!")
      } ~
        post {
          parameter('id) { _ =>
            throw new NoSuchElementException("boom!")
          }
        }
    }

  // Any exception not handled here will be handled by the default exception handler
  //ExceptionHandler.default()
  implicit val customExceptionHandler: ExceptionHandler = ExceptionHandler {
    case e: RuntimeException =>
      complete(NotFound, e.getMessage)
    case e: IllegalArgumentException =>
      complete(BadRequest, e.getMessage)
  }

  Http().bindAndHandle(simpleRoute, "localhost", 8080)
}
