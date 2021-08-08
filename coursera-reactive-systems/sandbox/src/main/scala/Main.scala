import akka.actor.{ActorSystem, Props}

val system = ActorSystem.create("Sandbox")
val actor = system.actorOf(Props[TransferMain], "wiretransfer")

@main def hello: Unit =
  println("Hello world!")
  println(msg)

def msg = "I was compiled by Scala 3. :)"
