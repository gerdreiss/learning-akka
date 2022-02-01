package learning.akkastreams

import akka.stream.ClosedShape
import akka.stream.scaladsl._
import akka.{Done, NotUsed}

import scala.concurrent.Future

object Graphs extends App {

  val writeAuthors: Sink[Author, Future[Done]] = Sink.foreach(println)
  val writeHashtags: Sink[Hashtag, Future[Done]] = Sink.foreach(println)

  val retrieveAuthors = Flow[Tweet].map(_.author)
  val retrieveHashtags = Flow[Tweet].mapConcat(_.hashtags.toList)

  val graph1: RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val bcast = builder.add(Broadcast[Tweet](2))
      tweets ~> bcast.in

      bcast.out(0) ~> retrieveAuthors ~> writeAuthors
      bcast.out(1) ~> retrieveHashtags ~> writeHashtags

      ClosedShape
    })

  val graph2: RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val in = Source(1 to 10)
    val out = Sink.foreach(println)

    val bcast = builder.add(Broadcast[Int](2))
    val merge = builder.add(Merge[Int](2))

    val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

    in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
    bcast ~> f4 ~> merge
    ClosedShape
  })

  graph1.run()
  graph2.run()

}
