package highlevelserver

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spray.json._

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

case class Player(nickname: String, characterClass: String, level: Int)

case object GetAllPlayers
case class GetPlayer(nickname: String)
case class GetPlayersByClass(characterClass: String)
case class AddPlayer(player: Player)
case class RemovePlayer(player: Player)
case object Ack

class GameAreaMap extends Actor with ActorLogging {

  type Nickname = String

  val players = mutable.Map.empty[Nickname, Player]

  override def receive: Receive = {
    case GetAllPlayers =>
      log.info("Getting all players")
      sender() ! players.values.toList

    case GetPlayer(nickname) =>
      log.info(s"Getting player $nickname")
      sender() ! players.get(nickname)

    case GetPlayersByClass(characterClass) =>
      log.info(s"Getting all players for class $characterClass")
      sender() ! players.values.toList.filter(_.characterClass == characterClass)

    case AddPlayer(player) =>
      log.info(s"Adding player $player")
      players.update(player.nickname, player)
      sender() ! Ack

    case RemovePlayer(player) =>
      log.info(s"Removing player $player")
      players.remove(player.nickname)
      sender() ! Ack
  }
}

trait PlayerJsonProtocol extends DefaultJsonProtocol {
  implicit val format: RootJsonFormat[Player] = jsonFormat3(Player)
}

object MarshallingJson extends App with PlayerJsonProtocol with SprayJsonSupport {

  implicit val system: ActorSystem = ActorSystem("MarshallingJson")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val gameMap = system.actorOf(Props[GameAreaMap], "GameAreaMap")

  val players = List(
    Player("p1", "c1", 70),
    Player("p2", "c2", 71),
    Player("p3", "c4", 72),
    Player("p4", "c4", 88),
    Player("p5", "c3", 73)
  )

  players.foreach(gameMap ! AddPlayer(_))

  implicit val timeout: Timeout = Timeout(3 seconds)

  val gameRoutes =
    pathPrefix("api" / "player") {
      get {
        path("classes" / Segment) { characterClass =>
          complete((gameMap ? GetPlayersByClass(characterClass)).mapTo[List[Player]])
        } ~
          (path(Segment) | parameter('nickname)) { nickname =>
            complete((gameMap ? GetPlayer(nickname)).mapTo[Option[Player]])
          } ~
          pathEndOrSingleSlash {
            complete((gameMap ? GetAllPlayers).mapTo[List[Player]])
          }
      } ~
        post {
          entity(as[Player]) { player =>
            complete((gameMap ? AddPlayer(player)).map {
              case Success(_) => StatusCodes.Created
              case _ => StatusCodes.BadRequest
            })
          }
        } ~
        delete {
          entity(as[Player]) { player =>
            complete((gameMap ? RemovePlayer(player)).map {
              case Success(_) => StatusCodes.OK
              case _ => StatusCodes.BadRequest
            })
          }
        }
    }

}

