package actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {

  class ActorWithExplicitLogger extends Actor {
    val log = Logging(context.system, this)
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  class ActorWithActorLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Got two things: {} and {}", a, b)
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("LoggingDemo")

  val actorWithExplicitLogger = system.actorOf(Props[ActorWithExplicitLogger], "ActorWithExplicitLogger")
  val actorWithActorLogging = system.actorOf(Props[ActorWithActorLogging], "ActorWithActorLogging")

  actorWithExplicitLogger ! "Logging a simple message"

  actorWithActorLogging ! "Logging a simpler message"
  actorWithActorLogging ! ("Thing one", "Thing two")
}
