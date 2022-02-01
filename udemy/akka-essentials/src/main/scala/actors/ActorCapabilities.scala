package actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi!" => sender() ! "Hello there!"
      case message: String => println(s"[$self] I have received $message")
      case number: Number => println(s"[$self] I have received the number $number")
      case SpecialMessage(contents) => println(s"[$self] I have received special message $contents")
      case SendMessageToSelf(contents) => self ! contents

      case SayHiTo(ref) => ref ! "Hi!"

      case ForwardMessage(contents, ref) => ref forward (contents + "!")
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")
  simpleActor ! "Hello Actor!"
  simpleActor ! 10


  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("special bloody message")

  case class SendMessageToSelf(contents: String)
  simpleActor ! SendMessageToSelf("selfmessage")


  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  alice !  "Hi!" // answer goes to "dead letters"

  case class ForwardMessage(contents: String, ref: ActorRef)
  alice ! ForwardMessage("Hi!", bob)
}
