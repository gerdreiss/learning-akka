import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

enum Greeter:
  case Greet(whom: String)
  case Stop

val greeter: Behavior[Greeter] =
  Behaviors.receiveMessage[Greeter] {
    case Greeter.Stop        =>
      println("shutting down...")
      Behaviors.stopped
    case Greeter.Greet(whom) =>
      println(s"Hello from Typed Actor 'greeter', $whom!")
      Behaviors.same
  }
