import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

sealed trait Greeter
final case class Greet(whom: String) extends Greeter
final case object Stop               extends Greeter

val greeter: Behavior[Greeter] =
  Behaviors.receiveMessage[Greeter] {
    case Greet(whom) =>
      println(s"Hello from Typed Actor 'greeter', $whom!")
      Behaviors.same
    case Stop        =>
      println("shutting down...")
      Behaviors.stopped
  }
