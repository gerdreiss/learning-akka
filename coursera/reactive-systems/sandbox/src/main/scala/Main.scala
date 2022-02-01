import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import javax.management.MBeanServerInvocationHandler

val system = ActorSystem[Nothing](
  Behaviors.setup[Nothing] { ctx =>
    val greeterRef = ctx.spawn(greeter, "greeter")
    ctx.watch(greeterRef) // sign death pact

    greeterRef ! Greeter.Greet("world")
    greeterRef ! Greeter.Stop

    Behaviors.empty
  },
  "helloworld"
)

@main def main: Unit = ()
