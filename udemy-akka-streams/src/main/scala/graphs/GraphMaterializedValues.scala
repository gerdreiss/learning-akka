package graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object GraphMaterializedValues extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("GraphMaterializedValues")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0) { (count, _) => count + 1 }

  val complexWordSink = Sink.fromGraph(
    GraphDSL.create(printer, counter)((_, counter) => counter) { implicit builder =>
      (printerShape, counterShape) =>
        import GraphDSL.Implicits._

        val broadcast = builder.add(Broadcast[String](2))
        val lowerCaseFilter = builder.add(Flow[String].filter(_.forall(_.isLower)))
        val shortStringFilter = builder.add(Flow[String].filter(_.length < 5))

        broadcast ~> lowerCaseFilter ~> printerShape
        broadcast ~> shortStringFilter ~> counterShape

        SinkShape(broadcast.in)
    }
  )

  val shortStringCountFuture = wordSource.toMat(complexWordSink)(Keep.right).run()

  import actorSystem.dispatcher

  shortStringCountFuture.onComplete {
    case Success(count) => println(s"Total number of short strings = $count")
    case Failure(exc) => println(s"The count of short strings failed: $exc")
  }

  /**
    * Exercise
    */
  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counter = Sink.fold[Int, B](0) { (count, _) => count + 1 }

    Flow.fromGraph(
      GraphDSL.create(counter) { implicit builder =>
        counterShape =>
          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[B](2))
          val originalFlowShape = builder.add(flow)

          originalFlowShape ~> broadcast ~> counterShape

          FlowShape(originalFlowShape.in, broadcast.out(1))
      }
    )
  }

  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(identity)
  val simpleSink = Sink.ignore

  val enhancedFlowCountFuture = simpleSource
    .viaMat(enhanceFlow(simpleFlow))(Keep.right)
    .toMat(simpleSink)(Keep.left)
    .run()

  enhancedFlowCountFuture.onComplete {
    case Success(count) => println(s"$count elements went through the enhanced flow")
    case _ => println("Something went wrong")
  }
}

