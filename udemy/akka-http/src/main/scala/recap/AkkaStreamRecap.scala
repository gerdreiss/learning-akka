package recap

import akka._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object AkkaStreamRecap extends App {

  implicit val system = ActorSystem("AkkaStreamRecap")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val source = Source(1 to 100)
  val sink = Sink.foreach[Int](println)
  val flow = Flow[Int].map(_ + 1)

  val runnableGraph: RunnableGraph[NotUsed] = source via flow to sink

  //val simpleMaterializedValue: NotUsed = runnableGraph.run()

  // materialized value

  val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

  val sumFuture = source runWith sumSink
  sumFuture.onComplete {
    case Success(sum) => println(s"The sum = $sum")
    case Failure(exc) => println(s"Boom! $exc")
  }

  val anotherMatVal = source.viaMat(flow)(Keep.right).toMat(sumSink)(Keep.right).run()
  anotherMatVal.onComplete {
    case Success(sum) => println(s"The mat val = $sum")
    case Failure(exc) => println(s"Boom! $exc")
  }

  val bufferedFlow = Flow[Int].buffer(10, OverflowStrategy.dropHead)

  source.async
    .via(bufferedFlow).async
    .runForeach { elm =>
      Thread.sleep(140)
      println(elm)
    }
}
