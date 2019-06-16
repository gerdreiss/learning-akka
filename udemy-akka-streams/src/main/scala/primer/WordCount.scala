package primer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.Future

object WordCount extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("WordCount")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val sentences =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua." ::
      "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat." ::
      "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur." ::
      "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum." :: Nil

  val sentenceSource: Source[String, NotUsed] = Source(sentences)
  val wordFlow: Flow[String, Seq[String], NotUsed] = Flow[String].map(_.split(" ").toSeq)
  val wordCountFlow: Flow[Seq[String], Int, NotUsed] = Flow[Seq[String]].map(_.size)
  val wordCountSumFlow = Flow[Int].fold(0)(_ + _)
  val wordCountSumSink: Sink[Int, Future[Int]] = Sink.reduce(_ + _)
  val wordCountPrintSink = Sink.foreach(println)

  val wordCountGraph = sentenceSource via wordFlow via wordCountFlow via wordCountSumFlow to wordCountPrintSink
  //wordCountGraph.run()

  //sentenceSource.runReduce((s1, s2) => s1.split(" ").length + s2.split(" ").length)
  sentenceSource.fold(0)((acc, sentence) => acc + sentence.split(" ").length).runForeach(println)
}
