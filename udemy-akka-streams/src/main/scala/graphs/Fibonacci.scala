package graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape, UniformFanInShape}

object Fibonacci extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("Fibonacci")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val fiboGen = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val zipper = builder.add(Zip[BigInt, BigInt])
    val merger = builder.add(MergePreferred[(BigInt, BigInt)](1))
    val fibona = builder.add(Flow[(BigInt, BigInt)].map { case (last, prev) =>
      Thread.sleep(180)
      (last + prev, last)
    })
    val broadcast = builder.add(Broadcast[(BigInt, BigInt)](2))
    val extracter = builder.add(Flow[(BigInt, BigInt)].map(_._1))

    zipper.out ~> merger ~>           fibona ~> broadcast ~> extracter
                  merger.preferred      <~      broadcast

    UniformFanInShape(extracter.out, zipper.in0, zipper.in1)
  }

  val fiboGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val source1 = builder.add(Source.single(BigInt(1)))
      val source2 = builder.add(Source.single(BigInt(2)))

      val sink = builder.add(Sink.foreach(println))
      val fibo = builder.add(fiboGen)

      source1 ~> fibo.in(0)
      source2 ~> fibo.in(1)
      fibo.out ~> sink

      ClosedShape
    }
  )

  fiboGraph.run()
}
