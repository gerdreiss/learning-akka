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

case class Guitar(make: String, model: String, quantity: Int = 0)

case class CreateGuitar(guitar: Guitar)
case class GuitarCreated(id: Int)
case class FindGuitar(id: Int)
case class FindGuitarsInStock(inStock: Boolean)

case class AddQuantity(id: Int, quantity: Int)

case object FindAllGuitars
case object UnknownRequest

class GuitarDB extends Actor with ActorLogging {

  val guitars = mutable.Map.empty[Int, Guitar]
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

    case AddQuantity(id, quantity) =>
      log.info(s"Adding quantity $quantity to guitar with id $id")
      val newGuitar = guitars.get(id).map(g => g.copy(quantity = g.quantity + quantity))
      newGuitar.foreach(g => guitars.update(id, g))
      sender() ! newGuitar

    case FindGuitarsInStock(inStock) =>
      log.info("Searching for guitars in stock...")
      sender() ! guitars.values.filter(_.quantity > 0 == inStock).toList

    case request =>
      log.error(s"Unknown request: $request")
      sender() ! UnknownRequest
  }
}

trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
  implicit val guitarFormat: RootJsonFormat[Guitar] = jsonFormat3(Guitar)
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
  //    |  "model": "Stratocaster",
  //    |  "quantity": 100
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

  import StatusCodes._

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.POST, uri@Uri.Path("/api/guitars/inventory"), _, _, _) =>
      val query: Uri.Query = uri.query()
      val result: Option[Future[HttpResponse]] = for {
        id <- query.get("id").map(_.toInt)
        q <- query.get("quantity").map(_.toInt)
      } yield {
        (guitarDb ? AddQuantity(id, q))
          .mapTo[Option[Guitar]]
          .map {
            _.fold(HttpResponse(status = BadRequest))(_ => HttpResponse(status = OK))
          }
      }
      result.getOrElse(Future(HttpResponse(status = NotFound)))

    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitars"), _, _, _) =>
      val query: Uri.Query = uri.query()

      if (query.isEmpty)
        (guitarDb ? FindAllGuitars).mapTo[List[Guitar]].map { guitars =>
          HttpResponse(
            entity = HttpEntity(
              ContentTypes.`application/json`,
              guitars.toJson.prettyPrint
            )
          )
        }
      else
        query.get("id").map(_.toInt) match {
          case None => Future(HttpResponse(status = NotFound))
          case Some(id) => (guitarDb ? FindGuitar(id)).mapTo[Option[Guitar]].map {
            case None => HttpResponse(status = NotFound)
            case Some(guitar) => HttpResponse(
              entity = HttpEntity(
                ContentTypes.`application/json`,
                guitar.toJson.prettyPrint
              )
            )
          }
        }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitars"), _, entity, _) =>
      val strictEntityFuture: Future[HttpEntity.Strict] = entity.toStrict(3 seconds)
      strictEntityFuture.flatMap { e =>
        val guitar = e.data.utf8String.parseJson.convertTo[Guitar]
        val guitarCreatedFuture = (guitarDb ? CreateGuitar(guitar)).mapTo[GuitarCreated]
        guitarCreatedFuture.map(_ => HttpResponse(Created))
      }

    case request: HttpRequest =>
      request.discardEntityBytes()
      Future {
        HttpResponse(status = NotFound)
      }
  }

  Http().bindAndHandleAsync(requestHandler, "localhost", 8080)

}
