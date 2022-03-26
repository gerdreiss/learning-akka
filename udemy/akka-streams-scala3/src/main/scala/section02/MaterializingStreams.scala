package section02

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.*
import akka.NotUsed
import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure
import akka.Done

object MaterializingStreams extends App:

  given system: ActorSystem             = ActorSystem("MaterializingStreams")
  given materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val numSource: Source[Int, NotUsed] = Source(1 to 10)

  val printingSink: Sink[Any, Future[Done]] = Sink.foreach(println)
  val reducingSink: Sink[Int, Future[Int]]  = Sink.reduce[Int](_ + _)

  val incrementingFlow: Flow[Int, Int, NotUsed]#Repr[Int] = Flow[Int].map(_ + 1)

  val printingGraph: RunnableGraph[NotUsed] = numSource to printingSink
  // val simpleGraphMatVal: NotUsed            = printingGraph.run() // NotUsed == void

  // numSource
  //   .runWith(reducingSink)
  //   .onComplete {
  //     case Success(result) => println(s"The sum of all elements is: $result")
  //     case Failure(error)  => println(s"The sum of the elements could not be computed: $error")
  //   }

  // choosing materialized value
  val incrementingGraph: RunnableGraph[Future[Done]] =
    numSource
      .viaMat(incrementingFlow)(Keep.right)
      .toMat(printingSink)(Keep.right)
  // incrementingGraph
  //   .run()
  //   .onComplete {
  //     case Success(result) => println(s"Stream processing finished: $result")
  //     case Failure(error)  => println(s"Stream processing failed: $error")
  //   }

  val sum: Future[Int] =
    // numSource.runWith(Sink.reduce[Int](_ + _)) == numSource.to(Sink.reduce(_ + _))(Keep.right)
    numSource.runReduce(_ + _)

  // backwards, keeping the left mat value
  printingSink runWith numSource

  // both ways
  incrementingFlow.runWith(numSource, printingSink)
