package actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object AkkaConfigDemo extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("AkkaConfigDemo")
  val actor = system.actorOf(Props[SimpleLoggingActor], "SimpleLogger")
  actor ! "Loggable message"

}
