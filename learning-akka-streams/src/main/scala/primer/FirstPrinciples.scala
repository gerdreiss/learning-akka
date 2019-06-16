package primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

object FirstPrinciples extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("FirstPrinciples")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  // Sources
  val source = Source(1 to 10)
  // Sinks
  val sink = Sink.foreach[Int](println)

  val graph = source to sink

  //graph.run()

  // flows transform elements
  val flow = Flow[Int].map(_ * 2)

  val graphWithFlow = source via flow to sink

  //graphWithFlow.run()

  /**
    * Exercise
    */
  val names = List("Alice", "Bob", "Charlie", "David", "Martin", "Xavier")
  val nameSource = Source(names)
  val longNameFlow = Flow[String].filter(_.length > 5)
  val limitFlow = Flow[String].take(2)
  val printSink = Sink.foreach[String](println)

  //(nameSource via longNameFlow via limitFlow to printSink).run()

  nameSource filter (_.length > 5) take 2 runForeach println

  source.runWith(Sink.reduce[Int](_ + _))
  source.runReduce[Int](_ + _)

  source.viaMat(Flow[Int].map(_.toString))(Keep.right).toMat(printSink)(Keep.right)
}
