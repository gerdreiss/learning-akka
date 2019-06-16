package primer

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

object OperatorFusion extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("FirstPrinciples")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach[Int](println)

  //simpleSource via simpleFlow via simpleFlow2 to simpleSink run()

  // equivalent to above
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case x: Int =>
        // flow operations
        val y = x + 1
        val z = y * 10
        // sink operations
        println(z)
    }
  }

  val simpleActor = actorSystem.actorOf(Props[SimpleActor])
  //(1 to 1000).foreach(simpleActor ! _)

  // complex flows>
  val complexFlow1 = Flow[Int].map { x =>
    Thread.sleep(1000)
    x + 1
  }
  val complexFlow2 = Flow[Int].map { x =>
    Thread.sleep(1000)
    x * 10
  }

  //simpleSource via complexFlow via complexFlow2 to simpleSink run()

  // async boundary
  //simpleSource
  //  .via(complexFlow1).async
  //  .via(complexFlow2).async
  //  .to(simpleSink)
  //  .run()

  // ordering guarantees
  Source(1 to 3)
    .map(elm => { println(s"Flow A: $elm"); elm }).async
    .map(elm => { println(s"Flow B: $elm"); elm }).async
    .map(elm => { println(s"Flow C: $elm"); elm }).async
    .runWith(Sink.ignore)

}
