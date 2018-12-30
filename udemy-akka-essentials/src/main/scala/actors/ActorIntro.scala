package actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorIntro extends App {

  val system = ActorSystem("actors-intro")

  class WordCountActor extends Actor {
    var totalWords = 0
    override def receive: Receive = {
      case message: String =>
        println(s"[${self.path.name}] received: $message")
        totalWords += message.split(" ").length
      case what => println(s"${self.path.name} cannot understand $what")
    }
  }

  val wordCounter = system.actorOf(Props[WordCountActor], "wordCounter")
  val anotherWordCounter = system.actorOf(Props[WordCountActor], "anotherWordCounter")

  wordCounter ! "I am learning Akka"
  anotherWordCounter ! "Me too"

  wordCounter ! 100
}
