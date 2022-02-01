package faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props}

object  StartingStoppingActors extends App {

  case class StartChild(name: String)
  case class StopChild(name: String)
  case object Stop

  class Parent extends Actor with ActorLogging  {
    override def receive: Receive = withChildren(Map.empty)

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child $name...")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.info(s"Stopping child $name...")
        children.get(name).foreach(context.stop)
      case Stop =>
        log.info("Stopping...")
        context stop self
    }
  }
  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("StartingStoppingActors")
  val parent = system.actorOf(Props[Parent], "parent")

  parent ! StartChild("child1")
  val child1 = system.actorSelection("/user/parent/child1")
  child1 ! "Hi, kid!"
  parent ! StopChild("child1")

  parent ! StartChild("child2")
  val child2 = system.actorSelection("/user/parent/child2")
  child2 ! "Hi, kid!"
  parent ! Stop


  val looseActor = system.actorOf(Props[Child])
  looseActor ! "hello, loose actor!"
  looseActor ! PoisonPill
  looseActor ! "are you still there?"

  val terminatedActor = system.actorOf(Props[Child])
  terminatedActor ! "you are about to be terminated"
  terminatedActor ! Kill
  terminatedActor ! "are you still there?"



}
