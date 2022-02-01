package graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy}

object GraphCycles extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("GraphCycles")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x...")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
                   mergeShape <~ incrementerShape

    ClosedShape
  }

  //RunnableGraph.fromGraph(accelerator).run() // <- graph cycle deadlock!

  // Solution 1: MergePreferred
  val actualAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x...")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
                   mergeShape <~ incrementerShape

    ClosedShape
  }

  //RunnableGraph.fromGraph(actualAccelerator).run()

  val bufferedRepeater = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val repeaterShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map { x =>
      println(s"Buffering $x...")
      Thread.sleep(100)
      x
    })

    sourceShape ~> mergeShape ~> repeaterShape
                   mergeShape <~ repeaterShape

    ClosedShape
  }

  RunnableGraph.fromGraph(bufferedRepeater).run()
}
