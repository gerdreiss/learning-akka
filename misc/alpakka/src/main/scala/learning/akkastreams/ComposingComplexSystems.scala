package learning.akkastreams

import akka.stream._
import akka.stream.scaladsl._

object ComposingComplexSystems extends App {

  import akka.stream.scaladsl.GraphDSL.Implicits._

  val g1 =
  //@formatter:off
  RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

    val A: Outlet[Int]                  = builder.add(Source.single(0)).out
    val B: UniformFanOutShape[Int, Int] = builder.add(Broadcast[Int](2))
    val C: UniformFanInShape[Int, Int]  = builder.add(Merge[Int](2))
    val D: FlowShape[Int, Int]          = builder.add(Flow[Int].map(_ + 1))
    val E: UniformFanOutShape[Int, Int] = builder.add(Balance[Int](2))
    val F: UniformFanInShape[Int, Int]  = builder.add(Merge[Int](2))
    val G: Inlet[Int]                   = builder.add(Sink.foreach((i: Int) => println(i))).in

    // Wow!!
                  C     <~      F
    A  ~>  B  ~>  C     ~>      F
           B  ~>  D  ~>  E  ~>  F
                         E  ~>  G

    ClosedShape
  })
  //@formatter:on

  val g2 =
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>
      val B = builder.add(Broadcast[Int](2))
      val C = builder.add(Merge[Int](2))
      val E = builder.add(Balance[Int](2))
      val F = builder.add(Merge[Int](2))

      Source.single(0) ~> B.in; B.out(0) ~> C.in(1); C.out   ~> F.in(0)
                                            C.in(0)          <~ F.out

      B.out(1).map(_ + 1) ~> E.in; E.out(0) ~> F.in(1)
      E.out(1) ~> Sink.foreach((i: Int) => println(i))

      ClosedShape
    })


  g1.run()
  g2.run()
}
