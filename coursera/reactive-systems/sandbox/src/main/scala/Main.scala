import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import javax.management.MBeanServerInvocationHandler

//val system = ActorSystem.create("Sandbox")
//val actor = system.actorOf(Props[TransferMain], "wiretransfer")
val system = ActorSystem[Nothing](
  Behaviors.setup[Nothing] { ctx =>
    val greeterRef = ctx.spawn(greeter, "greeter")
    ctx.watch(greeterRef) // sign death pact

    greeterRef ! Greet("world")
    greeterRef ! Stop

    Behaviors.empty
  },
  "helloworld"
)

@main def hello: Unit =
  println("Hello world!")
  println(msg)

def msg = "I was compiled by Scala 3. :)"
