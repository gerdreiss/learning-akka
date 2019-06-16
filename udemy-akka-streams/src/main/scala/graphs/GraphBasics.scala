package graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.math._

object GraphBasics extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("GraphBasics")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val input = Source(1 to 1000)

  val incrementer = Flow[Int].map(_ + 1)
  val multiplier  = Flow[Int].map(_ * 10)
  val squarer     = Flow[Int].map(pow(_, 2.0).intValue())
  val rooter      = Flow[Int].map(sqrt(_).intValue())

  val output = Sink.foreach[(Int, Int)](println)

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip       = builder.add(Zip[Int, Int]) // fan-in operator

      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> squarer ~> zip.in1

      zip.out ~> output

      ClosedShape
      // shape
    } // static graph
  )   // runnable graph

  //graph.run()

  val sink1 = Sink.foreach[Int](x => println(s"Sink 1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Sink 2: $x"))

  val exercise1 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator

      input ~> broadcast ~> rooter to sink1
               broadcast ~> squarer to sink2

      //broadcast.out(0) ~> sink1
      //broadcast.out(1) ~> sink2

      ClosedShape
      // shape
    } // static graph
  )   // runnable graph

  //exercise1.run()

  val fastSource = Source(Stream.from(0, 2)).take(500).throttle(10, 1 seconds)
  val slowSource = Source(Stream.from(1, 2)).take(500).throttle(1, 1 seconds)

  val exercise2 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merger = builder.add(Merge[Int](2))
      val balancer = builder.add(Balance[Int](2))

      //fastSource      ~> merger.in(0)
      //slowSource      ~> merger.in(1)
      //merger          ~> balancer
      //balancer.out(0) ~> sink1
      //balancer.out(1) ~> sink2

      fastSource ~> merger ~> balancer ~> sink1
      slowSource ~> merger;   balancer ~> sink2

      ClosedShape
      // shape
    } // static graph
  )   // runnable graph

  exercise2.run()
}
