package faulttolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props, Terminated}
import faulttolerance.StartingStoppingActors.{Child, StartChild, system}

object DeathWatch extends App {

  val system = ActorSystem("DeathWatch")
  /**
    * Death watch
    */
  class Watcher extends Actor with ActorLogging {
    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started watching $name")
        context watch child
      case Terminated(actor) =>
        log.info(s"The reference that I'm watching is terminated: $actor")
    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("watched")
  Thread.sleep(1000)
  val watched = system.actorSelection("/user/watcher/watched")

  Thread.sleep(1000)
  watched ! "I'm watching you"
  watched ! PoisonPill
}
