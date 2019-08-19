package highlevelserver

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.stream._

object HighLevelIntro extends App {

  implicit val system: ActorSystem = ActorSystem("HighLevelIntro")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import system.dispatcher

  // directives
  import Directives._

  val simpleRoute: Route =
    path("home") {
      complete(StatusCodes.OK)
    }

  val pathGetRoute: Route =
    path("home") {
      get {
        complete(StatusCodes.OK)
      }
    }

  val chainedRoute: Route =
    path("ok") {
      get {
        complete(StatusCodes.OK)
      } ~
      post {
        complete(StatusCodes.Created)
      } ~
      put {
        complete(StatusCodes.Forbidden)
      }
    } ~
    path("hello") {
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    <center>
            |      <h1>
            |        Hello from Akka HTTP
            |      </h1>
            |    </center>
            |  </body>
            |</html>
            |""".stripMargin
        )
      )
    }

  Http().bindAndHandle(chainedRoute, "localhost", 8080, ssl.httpsConnectionContext)

}
