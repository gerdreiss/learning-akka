package infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Terminated}
import akka.routing._
import com.typesafe.config.ConfigFactory

object Routers extends App {

  /**
    * Manual router
    */
  class Master extends Actor with ActorLogging {
    private val slaves = (1 to 5) map { i =>
      val slave = context.actorOf(Props[Slave], s"slave$i")
      context.watch(slave)
      ActorRefRoutee(slave)
    }
    private val router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {

      case Terminated(ref) =>
        router.removeRoutee(ref)
        val newSlave = context.actorOf(Props[Slave])
        context.watch(newSlave)
        router.addRoutee(newSlave)

      case message =>
        router.route(message, sender())
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("RoutersDemo", ConfigFactory.load().getConfig("routersDemo"))
  val master = system.actorOf(Props[Master])

  (1 to 9) foreach { i =>
    master ! s"$i Hello from the world"
  }

  /**
    * Pool router
    */
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "simplePoolMaster")
  (1 to 9) foreach { i =>
    poolMaster ! s"$i Hello from the world"
  }

  /**
    * from configuration
    */
  val poolMaster2 = system.actorOf(FromConfig.props(Props[Slave]), "poolMaster2")
  (1 to 9) foreach { i =>
    poolMaster2 ! s"$i Hello from the world"
  }

  /**
    * router with actors created else where
    * GROUP router
    */
  val slaves = (1 to 5) map { i =>
    system.actorOf(Props[Slave], s"slave$i")
  }
  val slavePaths = slaves.map(_.path.toString)
  val groupMaster = system.actorOf(RoundRobinGroup(slavePaths).props())
  (1 to 9) foreach { i =>
    groupMaster ! s"$i Hello from the world"
  }


  // sending message to every one
  groupMaster ! Broadcast("hello, everyone!")

  // PoisonPill and Kill are NOT routed
  // AddRoutee, Remove, Get handled only by the routing actor
}
