package recap

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorSystem, OneForOneStrategy, PoisonPill, Props, Stash, SupervisorStrategy}

object AkkaRecap extends App {

  // guardians: /system, /user, / = root guardian

  class SimpleActor extends Actor with Stash {
    override def receive: Receive = {
      case "createChild" =>
        val childActor = context.actorOf(Props[SimpleActor], "theChild")
        childActor ! "hello"
      case "stash" => stash()
      case "unstash" =>
        unstashAll()
        context.become(anotherHandler)
      case "change" => context.become(anotherHandler)
      case message => println(s"I received: $message")
    }

    def anotherHandler: Receive = {
      case message => println(s"In another receive handler: $message")
    }

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: RuntimeException => Restart
      case _ => Stop
    }
  }

  val system = ActorSystem("AkkaRecap")
  val actor = system.actorOf(Props[SimpleActor], "simpleActor")
  actor ! "hello!"

  // poison the actor!
  actor ! PoisonPill

  // logging
  // supervision

}
