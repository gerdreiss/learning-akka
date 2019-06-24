package graphs

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, BidiShape, ClosedShape}

object BidirectionalFlows extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("BidirectionalFlows")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def encrypt(n: Int)(string: String) = string.map(c => (c + n).toChar)

  def decrypt(n: Int)(string: String) = string.map(c => (c - n).toChar)

  println(encrypt(3)("Akka"))
  println(decrypt(3)("Dnnd"))

  val bidiCryptoStaticGraph = GraphDSL.create() { implicit builder =>

    val encryptionFlow = builder.add(Flow[String].map(encrypt(3)))
    val decryptionFlow = builder.add(Flow[String].map(decrypt(3)))

    // BidiShape(encryptionFlow.in, encryptionFlow.out, decryptionFlow.in, decryptionFlow.out)
    // or just:
    BidiShape.fromFlows(encryptionFlow, decryptionFlow)
  }

  val unencryptedStrings = List("akka", "is", "awesome", "testing", "bidirectional", "flows")
  val unencryptedSource = Source(unencryptedStrings)
  val encryptedSource = Source(unencryptedStrings.map(encrypt(3)))

  val cryptoBidiGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val unencryptedSourceShape = builder.add(unencryptedSource)
      val encryptedSourceShape = builder.add(encryptedSource)

      val bidi = builder.add(bidiCryptoStaticGraph)

      val encryptedSinkShape = builder.add(Sink.foreach[String](s => println(s"Encrypted: $s")))
      val decryptedSinkShape = builder.add(Sink.foreach[String](s => println(s"Decrypted: $s")))

      unencryptedSourceShape ~> bidi.in1   ;  bidi.out1 ~> encryptedSinkShape
      decryptedSinkShape     <~ bidi.out2  ;  bidi.in2  <~ encryptedSourceShape

      ClosedShape
    })

  cryptoBidiGraph.run()
}
