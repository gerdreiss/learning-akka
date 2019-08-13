package lowlevel

import akka.actor._
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object LowLevelAPI extends App {

  implicit val system: ActorSystem = ActorSystem("LowLevelServerAPI")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  import system.dispatcher

  val serverSource = Http().bind("localhost", 8000)

  val connectionSink = Sink.foreach[IncomingConnection] { connection =>
    println(s"Incoming from ${connection.remoteAddress}")
  }

  val serverBindingFuture: Future[Http.ServerBinding] = serverSource.to(connectionSink).run()
  serverBindingFuture.onComplete {
    case Success(binding) =>
      println(s"Server binding successfull: $binding")
      binding.terminate(2 seconds)
      println("Terminated...")
    case Failure(exc) => println(s"Server binding failed: $exc")
  }

  // Method 1: sync
  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        status = StatusCodes.OK, // HTTP 200
        entity = HttpEntity(
          `text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Hello from Akka HTTP!
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

  val httpSyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithSyncHandler(requestHandler)
  }

  //Http().bind("localhost", 8080).runWith(httpSyncConnectionHandler)
  //Http().bindAndHandleSync(requestHandler, "localhost", 8080)

  // Method 2: async

  val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) =>
      Future {
        HttpResponse(
          status = StatusCodes.OK, // HTTP 200
          entity = HttpEntity(
            `text/html(UTF-8)`,
            """
              |<html>
              |  <body>
              |    Hello from Akka HTTP!
              |  </body>
              |</html>
              |""".stripMargin
          )
        )
      }
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future {
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
  }


  val httpAsyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithAsyncHandler(asyncRequestHandler)
  }

  //Http().bind("localhost", 8081).runWith(httpAsyncConnectionHandler)
  //Http().bindAndHandleAsync(asyncRequestHandler, "localhost", 8081)

  // Method 3: async via AKKA Streams
  val streamBasedRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
      HttpResponse(
        status = StatusCodes.OK,
        entity = HttpEntity(
          `text/plain(UTF-8)`,
          "Welcome!"
        )
      )
    case HttpRequest(HttpMethods.GET, Uri.Path("/search"), _, _, _) =>
      HttpResponse(
        status = StatusCodes.Found,
        headers = List(Location("http://duckduckgo.com"))
      )
    case HttpRequest(HttpMethods.GET, Uri.Path("/about"), _, _, _) =>
      HttpResponse(
        status = StatusCodes.OK, // HTTP 200
        entity = HttpEntity(
          `text/html(UTF-8)`,
          """
            |<html>
            |  <body>
            |    Hello from Akka HTTP!
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

  //Http().bind("localhost", 8082).runForeach { connection =>
  //  connection.handleWith(streamBasedRequestHandler)
  //}

  val bindingFuture = Http().bindAndHandle(streamBasedRequestHandler, "localhost", 8082)

  bindingFuture.flatMap(_.unbind()).onComplete { _ =>
    system.terminate()
  }
}
