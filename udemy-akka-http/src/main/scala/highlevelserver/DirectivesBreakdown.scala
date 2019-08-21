package highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

object DirectivesBreakdown extends App {

  implicit val system: ActorSystem = ActorSystem("DirectivesBreakdown")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Type 1: filtering
  val simpleHttpMethodRoute =
    post {
      complete(StatusCodes.Forbidden)
    }
  val simplePathRoute =
    path("about") {
      complete {
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    <h1>This is a simple path route</h1>
            |  </body>
            |</html>
            |""".stripMargin
        )
      }
    }
  val complexPathRoute =
    path("api" / "v1") {
      complete(StatusCodes.OK)
    }
  val dontConfuse =
    path("api/v1") { // <- this is URL-encoded and doesn't work as one would expect
      complete(StatusCodes.OK)
    }
  val pathEndRoute =
    pathEndOrSingleSlash {
      complete(StatusCodes.OK)
    }

  // Type 2: extraction
  val pathExtractionRoute =
    path("api" / "v1" / "items" / IntNumber) { id =>
      complete(
        HttpEntity(
          contentType = ContentTypes.`application/json`,
          s"""{ "received": $id }"""
        )
      )
    }
  val pathMultiExtractionRoute =
    path("api" / "v1" / "orders" / IntNumber / "items" / IntNumber) { (orderId, itemId) =>
      complete(
        HttpEntity(
          contentType = ContentTypes.`application/json`,
          s"""{ "order": $orderId, "item": $itemId }"""
        )
      )
    }
  val queryParamExtractionRoute =
    path("api" / "v1" / "items") {
      parameter('id.as[Int]) { itemId =>
        complete(
          HttpEntity(
            contentType = ContentTypes.`application/json`,
            s"""{ "item": $itemId, "type": ${itemId.getClass} }"""
          )
        )
      }
    }
  val requestExtractionRoute =
    path("api" / "v1") {
      extractRequest { req =>
        complete(
          HttpEntity(
            contentType = ContentTypes.`application/json`,
            s"""{ "req": ${req.headers} }"""
          )
        )
      }
    }
  val logExtractionRoute =
    path("api" / "v1") {
      extractLog { log =>
        complete(
          HttpEntity(
            contentType = ContentTypes.`application/json`,
            s"""{ "req": ${log.mdc} }"""
          )
        )
      }
    }

  Http().bindAndHandle(logExtractionRoute, "localhost", 8080)

}
