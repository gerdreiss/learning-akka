package technics

import akka.actor._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

object IntegratingWithActors extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("IntegratingWithActors")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received a string: $s")
        sender() ! s"$s$s"
      case n: Int =>
        log.info(s"Just received a number: $n")
        sender() ! (n * 2)
      case _ =>
    }
  }

  val simpleActor = actorSystem.actorOf(Props[SimpleActor], "SimpleActor")

  val numbers = Source(1 to 10)

  // actor as a flow
  implicit val timeout: Timeout = Timeout(2 seconds)
  val actorBasedFlow = Flow[Int].ask[Int](4)(simpleActor)

  //numbers via actorBasedFlow to Sink.foreach(println) run()
  //numbers.ask[Int](parallelism = 4)(simpleActor).to(Sink.ignore).run() // equivalent

  // Actor as a source
  val actorPoweredSource: Source[Int, ActorRef] = Source.actorRef[Int](bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead)
  val materializedActorRef: ActorRef = actorPoweredSource.to(Sink.foreach[Int](n => println(s"Actor powered flow got number: $n"))).run()
  materializedActorRef ! 10
  materializedActorRef ! akka.actor.Status.Success("complete")

  // Actor as a destination/sink
  // - an init message
  // - an ack message to confirm the reception
  // - a complete message
  // - a function to generate a message in case the stream throws an exception
  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(ex: Throwable)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream initialized")
        sender() ! StreamAck

      case StreamComplete =>
        log.info("Stream complete")
        context.stop(self)

      case StreamFail(ex) =>
        log.error(s"Stream failed: $ex")

      case message =>
        log.info(s"Message received: $message")
        sender() ! StreamAck
    }
  }

  val destinationActor = actorSystem.actorOf(Props[DestinationActor], "DestinationActor")
  val actorPoweredSink = Sink.actorRefWithAck[Int](
    ref               = destinationActor,
    onInitMessage     = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage        = StreamAck,
    onFailureMessage  = throwable => StreamFail(throwable) // optional
  )

  Source(1 to 10).to(actorPoweredSink).run()

  // Sink.actorRef() <- not recommended!
}

