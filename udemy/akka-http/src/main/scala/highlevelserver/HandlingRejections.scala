package highlevelserver

import akka.actor.ActorSystem
import akka.http.javadsl.server.MissingQueryParamRejection
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MethodRejection, RejectionHandler, SchemeRejection}
import akka.stream.ActorMaterializer

object HandlingRejections extends App {

  implicit val system: ActorSystem = ActorSystem("HandlingRejections")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val simpleRoute =
    path("api" / "r") {
      get {
        complete(OK)
      } ~
        parameter('q) { q =>
          complete(OK)
        }
    }

  // Rejection Handlers
  val badReqHandler: RejectionHandler = { rejections =>
    println(s"boom, rejected: $rejections")
    Some(complete(BadRequest))
  }
  val vorbiddenHandler: RejectionHandler = { rejections =>
    println(s"boom, rejected: $rejections")
    Some(complete(Forbidden))
  }

  RejectionHandler.default // <- the default rejection handler

  val simpleRouteWithHandlers =
    handleRejections(badReqHandler) {
      path("api" / "r") {
        get {
          complete(OK)
        } ~
          post {
            handleRejections(vorbiddenHandler) {
              parameter('q) { _ =>
                complete(Created)
              }
            }
          }
      }
    }

  //Http().bindAndHandle(simpleRouteWithHandlers, "localhost", 8080)

  implicit val customRejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case r: MethodRejection =>
        complete(s"Rejected method $r")
      case r: MissingQueryParamRejection =>
        complete(s"Missing parameter $r")
    }
    // multiple handles can be added to determine the handler priority
    .handle {
      case r: SchemeRejection =>
        complete(s"Wrong scheme $r")
    }
    .result()


  // sealing a route
  Http().bindAndHandle(simpleRoute, "localhost", 8080) // the implicit rejection handler is used here

}
