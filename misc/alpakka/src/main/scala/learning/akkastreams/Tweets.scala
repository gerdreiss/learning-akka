package learning.akkastreams

import akka.NotUsed
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._

import scala.concurrent.Future

object Tweets extends App {

  val hashtags: Source[Hashtag, NotUsed] = tweets.mapConcat(_.hashtags.toList)

  val names: Source[String, NotUsed] =
    tweets
      .map(_.hashtags) // Get all sets of hashtags ...
      .reduce(_ ++ _) // ... and reduce them to a single set, removing duplicates across all tweets
      .mapConcat(identity) // Flatten the stream of tweets to a stream of hashtags
      .map(_.name.toUpperCase) // Convert all hashtags to upper case

  val authors: Source[String, NotUsed] =
    tweets
      .filter(_.hashtags.contains(akkaTag))
      .map(_.author)
      .map(_.handle.toUpperCase) // Convert all handle to upper case

  // .runWith(Sink.foreach(println)) = .runForeach(println)
  names.runWith(Sink.foreach(println))
  authors.runForeach(println)

  // handling backpressure
  tweets
    .buffer(10, OverflowStrategy.dropHead)
    .map(slowComputation)
    .runWith(Sink.ignore)

  private def slowComputation = {
    t: Tweet => {
      Thread.sleep(1000)
      t.author
    }
  }


  val count: Flow[Tweet, Int, NotUsed] = Flow[Tweet].map(_ ⇒ 1)
  val sumSink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _)

  val counterGraph: RunnableGraph[Future[Int]] =
    tweets
      .via(count)
      .toMat(sumSink)(Keep.right)

  val sum: Future[Int] = counterGraph.run()

  sum.foreach(c ⇒ println(s"Total tweets processed: $c"))
}
