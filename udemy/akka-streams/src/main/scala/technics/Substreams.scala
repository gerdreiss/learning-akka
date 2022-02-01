package technics

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.util.{Failure, Success}

object Substreams extends App {

  implicit val system: ActorSystem = ActorSystem("Substreams")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  // 1 - grouping a stream
  val wordsSource = Source(List("Akka", "Shmakka", "Xyqkka", "Substreams"))
  val groups = wordsSource.groupBy(30, _.toLowerCase.headOption.getOrElse('\0'))

  def teeLog[A](message: String)(a: => A) = {
    println(message + a)
    a
  }

  groups
    .to(Sink.fold(0)((count, word) => teeLog(s"Received $word, new count = ")(count + 1)))
    .run()

  // 2 - merge substreams
  val textSource = Source(List("bla", "bloop", "bladibla"))
  val ff = textSource.groupBy(2, _.length % 2)
    .map(_.length)
    .mergeSubstreamsWithParallelism(2)
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  ff.onComplete {
    case Success(value) => println(s"Total = $value")
    case Failure(exception) => println(s"Boom! $exception")
  }
}
