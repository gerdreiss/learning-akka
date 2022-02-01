import akka.actor.typed.*
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.FSM.Shutdown

enum Greeter:
  case Greet(whom: String)
  case Stop

enum Guardian:
  case NewGreeter(replyTo: ActorRef[ActorRef[Greeter]])
  case Shutdown

val greeter: Behavior[Greeter] =
  Behaviors.receiveMessage[Greeter] {
    case Greeter.Greet(whom) =>
      println(s"Hello from Typed Actor 'greeter', $whom!")
      Behaviors.same

    case Greeter.Stop =>
      println("shutting down...")
      Behaviors.stopped
  }

val guardian = Behaviors.receive[Guardian] {
  case (ctx, Guardian.NewGreeter(replyTo)) =>
    val ref: ActorRef[Greeter] = ctx.spawnAnonymous(greeter)
    replyTo ! ref
    Behaviors.same

  case (_, Guardian.Shutdown) =>
    Behaviors.stopped

}
