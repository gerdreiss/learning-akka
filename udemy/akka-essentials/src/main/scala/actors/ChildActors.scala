package actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActors extends App {

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }

  class Parent extends Actor {
    import Parent._
    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path}: Creating child '$name'")
        val childRef = context.actorOf(Props[Child], name)
        context.become(withChild(childRef))
    }
    def withChild(childRef: ActorRef): Receive = {
      case TellChild(message) => childRef forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message: String => println(s"${self.path}: I got $message")
    }
  }

  import Parent._
  val system = ActorSystem("ParentChildDemo")
  val parent  = system.actorOf(Props[Parent], "parent")
  val childName = "Billy_The_Kid"
  parent ! CreateChild(childName)
  parent ! TellChild(s"Hello, $childName")

  /*
    Guardian actors (top-level)
    - /       = the root guardian
    - /system = system guardian
    - /user   = user-level guardian
   */
  /**
    * Actor selection
    */
  val childSelection = system.actorSelection(s"/user/parent/$childName")
  childSelection ! "Found ya!"

 }
