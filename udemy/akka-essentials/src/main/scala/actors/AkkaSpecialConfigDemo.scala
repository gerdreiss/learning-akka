package actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object AkkaSpecialConfigDemo extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  private val mySpecialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
  val system = ActorSystem("AkkaSpecialConfigDemo", mySpecialConfig)
  val actor = system.actorOf(Props[SimpleLoggingActor], "SimpleLogger")
  actor ! "Loggable message"

}
