package section02

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.*

import scala.concurrent.Future

object FirstPrinciples extends App:

  given system: ActorSystem             = ActorSystem("FirstPrinciples")
  given materializer: ActorMaterializer = ActorMaterializer()

  // sources
  val source = Source(1 to 10)
  // sinks
  val sink   = Sink.foreach[Int](println)
  // flows
  val flow   = Flow[Int].map(_ * 2)
  // graphs
  val graph  = source via flow to sink

  // graph.run()

  // various kinds of sources
  val singleSource   = Source.single(1)
  val listSource     = Source(List(1, 2, 3))
  val emptySource    = Source.empty[Int]
  val infiniteSource = Source(Stream.from(1))

  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.fromFuture(Future(42))

  // sinks
  val doingNothing = Sink.ignore
  val foreachSink  = Sink.foreach[String](println)
  val headSink     = Sink.head[Int] // retrieves the head and closes the stream
  val foldSink     = Sink.fold[Int, Int](0)((acc, next) => acc + next)

  // flows
  val mapFlow  = Flow[Int].map(_ * 2)
  val takeFlow = Flow[Int].take(5)
  // drop, filter, etc.

  // source -> flow -> flow -> ... -> flow -> sink
  val g = infiniteSource via mapFlow via takeFlow via Flow[Int].map(_.toString) to foreachSink
  g.run()
