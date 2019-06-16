package graphs

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.math.{pow, sqrt}

object OpenGraphs extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("OpenGraphs")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val source1 = Source(1 to 10)
  val source2 = Source(42 to 1000)

  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val concat = builder.add(Concat[Int](2))

      source1 ~> concat
      source2 ~> concat

      SourceShape(concat.out)
      // shape
    } // static graph
  ) // source graph

  //sourceGraph to Sink.foreach(println) run()

  /**
    * Complex sink
    */
  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2: $x"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink1
      broadcast ~> sink2

      SinkShape(broadcast.in)
    }
  )

  //source1 to sinkGraph run()
  val incrementer = Flow[Int].map(_ + 1)
  val multiplier  = Flow[Int].map(_ * 10)
  val squarer     = Flow[Int].map(pow(_, 2.0).intValue())
  val rooter      = Flow[Int].map(sqrt(_).intValue())

  val complexFlow = squarer via multiplier via incrementer via rooter

  val complexFlowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val squarerShape     = builder.add(squarer)
      val multiplierShape  = builder.add(multiplier)
      val incrementerShape = builder.add(incrementer)
      val rooterShape      = builder.add(rooter)

      squarerShape ~> multiplierShape ~> incrementerShape ~> rooterShape

      FlowShape(squarerShape.in, rooterShape.out)
    }
  )

  complexFlowGraph.runWith(sourceGraph, sinkGraph)
}
