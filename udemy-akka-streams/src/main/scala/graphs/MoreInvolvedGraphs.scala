package graphs

import java.time.LocalDate

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

object MoreInvolvedGraphs extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("MoreOpenGraphs")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: LocalDate)

  val transactionSource = Source(List(
    Transaction("1203940123", "Paul", "Jim", 100, LocalDate.now()),
    Transaction("1203940124", "Daniel", "Jim", 100000, LocalDate.now()),
    Transaction("1203940125", "Jim", "Alice", 7000, LocalDate.now())
  ))

  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](tx => println(s"Suspicious transaction ID: $tx"))

  val suspiciousTxnStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousTxnFilter = builder.add(Flow[Transaction].filter(_.amount > 10000))
    val trxIdExtractor = builder.add(Flow[Transaction].map[String](_.id))

    broadcast.out(0) ~> suspiciousTxnFilter ~> trxIdExtractor
    //broadcast.out(1) goes nowhere yet

    new FanOutShape2(
      // incoming
      broadcast.in,
      // outgoing, not yet sinked
      broadcast.out(1),
      // txn id outgoing
      trxIdExtractor.out
    )
  }

  val suspiciousTxnRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val suspiciousTxnShape = builder.add(suspiciousTxnStaticGraph)

      transactionSource ~> suspiciousTxnShape.in

      suspiciousTxnShape.out0 ~> bankProcessor
      suspiciousTxnShape.out1 ~> suspiciousAnalysisService

      ClosedShape
    }
  )

  suspiciousTxnRunnableGraph.run()
}
