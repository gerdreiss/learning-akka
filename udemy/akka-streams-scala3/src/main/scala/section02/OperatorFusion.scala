package section02

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.*
import akka.actor.Actor
import akka.actor.Props

object OperatorFusion extends App:

  given system: ActorSystem             = ActorSystem("OperatorFusion")
  given materializer: ActorMaterializer = ActorMaterializer()

  val numSource   = Source(1 to 1000)
  val incFlow     = Flow[Int].map(_ + 1)
  val dblFlow     = Flow[Int].map(_ * 2)
  val printerSink = Sink.foreach[Int](println)

  // running on the same actor => operator/component Fusion
  // numSource.via(incFlow).via(dblFlow).to(printerSink).run()

  // more or less same as the graph above
  class SimpleActor extends Actor:
    def receive = { case x: Int =>
      // flow ops
      val x2 = x + 1
      val x3 = x2 * 2
      // sink op
      println(x3)
    }

  val simpleActor = system.actorOf(Props[SimpleActor]())
  // (1 to 1000).foreach(simpleActor ! _)

  // long running flows:
  val longIncFlow = Flow[Int].map { x =>
    println("incrementing input...")
    Thread.sleep(1000)
    x + 1
  }
  val longDblFlow = Flow[Int].map { x =>
    println("doubling input...")
    Thread.sleep(1000)
    x * 2
  }

  numSource.via(longIncFlow).via(longDblFlow).to(printerSink).run()
