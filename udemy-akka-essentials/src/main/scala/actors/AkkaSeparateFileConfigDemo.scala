package actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object AkkaSeparateFileConfigDemo extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  private val separateFileConfig = ConfigFactory.load("secretFolder/secretConfiguration")
  val system = ActorSystem("AkkaSeparateFileConfigDemo", separateFileConfig)
  val actor = system.actorOf(Props[SimpleLoggingActor], "SimpleLogger")
  actor ! "Loggable message"

}
