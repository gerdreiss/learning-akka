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
  val simpleNestedRoute =
    path("api" / "v1") {
      get {
        complete(StatusCodes.OK)
      }
    }
  val compactSimpleNestedRoute =
    (path("api" / "v1") & get) {
      complete(StatusCodes.OK)
    }
  val compactExtractRequestRoute =
    (path("api" / "v1") & extractRequest & extractLog) { (req, log) =>
      log.info(s"HTTP Request received: $req")
      complete(StatusCodes.OK)
    }
  val repeatedRoute =
    path("about") {
      complete(StatusCodes.OK)
    } ~
      path("about" / "us") {
        complete(StatusCodes.OK)
      }
  val dryRoute =
    (path("about") | path("about" / "us")) {
      complete(StatusCodes.OK)
    }

  val blogByIdRoute =
    path(IntNumber) { postId =>
      complete(StatusCodes.OK)
    }
  val blogByQueryParamRoute =
    parameter('postId.as[Int]) { postId =>
      complete(StatusCodes.OK)
    }
  val combindedBlogByIdRoute =
    (path(IntNumber) | parameter('postId.as[Int])) { postId =>
      complete(StatusCodes.OK)
    }

  val completeOkRoute = complete(StatusCodes.OK)
  val failedRoute =
    path("unsupported") {
      failWith(new RuntimeException("Unsupported")) // => HTTP 500
    }
  val rejectRoute =
    path("rejected") {
      reject
    } ~
      path("index") {
        completeOkRoute
      }

  val getOrPutPath =
    path("api" / "v1") {
      get {
        completeOkRoute
      } ~
        post {
          complete(StatusCodes.Created)
        }
    }

  Http().bindAndHandle(getOrPutPath, "localhost", 8080)

}
