package lowlevel.https

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model.ContentTypes.`text/html(UTF-8)`
import akka.http.scaladsl.model._
import akka.stream._

object LowLevelHTTPS extends App {

  implicit val system: ActorSystem = ActorSystem("LowLevelHTTPS")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        status = StatusCodes.OK, // HTTP 200
        entity = HttpEntity(
          `text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Hello from Akka HTTPS!
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        status = StatusCodes.NotFound,
        entity = HttpEntity(
          `text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Oops! The resource not found...
            |  </body>
            |</html>
            |""".stripMargin

        )
      )
  }

  val httpsBinding = Http()
    .bindAndHandleSync(requestHandler, "localhost", 8443, ssl.httpsConnectionContext)

}
