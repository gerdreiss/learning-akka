package lowlevel.rest

import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream._
import akka.util.Timeout
import spray.json._

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

case class Guitar(make: String, model: String)

case class CreateGuitar(guitar: Guitar)
case class GuitarCreated(id: Int)
case class FindGuitar(id: Int)
case object FindAllGuitars
case object UnknownRequest

class GuitarDB extends Actor with ActorLogging {

  val guitars: mutable.Map[Int, Guitar] = mutable.Map.empty
  val currentGuitarId = new AtomicInteger(0)

  override def receive: Receive = {
    case FindAllGuitars =>
      log.info("Searching for all guitars...")
      sender() ! guitars.values.toList

    case FindGuitar(id) =>
      log.info(s"Searching guitar by id: $id")
      sender() ! guitars.get(id)

    case CreateGuitar(guitar) =>
      log.info(s"Adding guitar $guitar with id $currentGuitarId")
      val newGuitarId = currentGuitarId.getAndIncrement()
      guitars += newGuitarId -> guitar
      sender() ! GuitarCreated(newGuitarId)

    case request =>
      log.error(s"Unknown request: $request")
      sender() ! UnknownRequest
  }
}

trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
  implicit val guitarFormat: RootJsonFormat[Guitar] = jsonFormat2(Guitar)
}

object LowLevelRest extends App with GuitarStoreJsonProtocol {

  implicit val system: ActorSystem = ActorSystem("LowLevelRest")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // don't do this in prod!

  import system.dispatcher

  //val simpleGuitar = Guitar("Fender", "Stratocaster")
  //println(simpleGuitar.toJson.prettyPrint)
  //val simpleGuitarJson =
  //  """
  //    |{
  //    |  "make": "Fender",
  //    |  "model": "Stratocaster"
  //    |}
  //    |""".stripMargin
  //println(simpleGuitarJson.parseJson.convertTo[Guitar])

  val guitarDb = system.actorOf(Props[GuitarDB], "LowLevelGuitarDB")

  List(
    Guitar("Fender", "Stratocaster"),
    Guitar("Gibson", "Les Paul"),
    Guitar("Martin", "LX1")
  ) foreach { guitar =>
    guitarDb ! CreateGuitar(guitar)
  }

  implicit val defaultTimeout: Timeout = Timeout(2 seconds)

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/guitars"), _, _, _) =>
      val evtlGuitars = (guitarDb ? FindAllGuitars).mapTo[List[Guitar]]
      evtlGuitars.map { guitars =>
        HttpResponse(
          entity = HttpEntity(
            ContentTypes.`application/json`,
            guitars.toJson.prettyPrint
          )
        )
      }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitars"), _, entity, _) =>
      val strictEntityFuture: Future[HttpEntity.Strict] = entity.toStrict(3 seconds)
      strictEntityFuture.flatMap { e =>
        val guitar = e.data.utf8String.parseJson.convertTo[Guitar]
        val guitarCreatedFuture = (guitarDb ? CreateGuitar(guitar)).mapTo[GuitarCreated]
        guitarCreatedFuture.map(_ => HttpResponse(StatusCodes.Created))
      }

    case request: HttpRequest =>
      request.discardEntityBytes()
      Future {
        HttpResponse(status = StatusCodes.NotFound)
      }
  }

  Http().bindAndHandleAsync(requestHandler, "localhost", 8080)

}
