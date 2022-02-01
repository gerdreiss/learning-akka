package actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object InlineAkkaConfigDemo extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val configString =
    """
      | akka {
      |   loglevel = ERROR
      | }
    """.stripMargin

  val config = ConfigFactory.parseString(configString)
  val system = ActorSystem("InlineAkkaConfigDemo", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleLoggingActor], "SimpleLogger")

  actor ! "A message to remember"

}
