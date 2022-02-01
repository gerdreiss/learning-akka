package primer

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}

import scala.concurrent.duration._
import scala.language.postfixOps

object BackpresureBasics extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("FirstPrinciples")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming: $x")
    x + 1
  }

  val slowSink = Sink.foreach[Int] { x =>
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

  // not backpressure:
  //fastSource to slowSink run()

  // yes backpressure:
  //fastSource.async.to(slowSink).run()

  val bufferedFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.backpressure)
  val throttledSource = fastSource.throttle(12, 1 second)

  throttledSource.async.via(bufferedFlow).async.to(slowSink).run()

}
