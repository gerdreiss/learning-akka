package technics

import akka.actor.ActorSystem
import akka.stream.Supervision.{Resume, Stop}
import akka.stream.scaladsl._
import akka.stream.{ActorAttributes, ActorMaterializer}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

object FaultTolerance extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("FaultTolerance")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val faultySource = Source(1 to 10).map(e => if (e == 6) throw new RuntimeException else e)
  //faultySource.log("trackingSource").to(Sink.ignore).run()

  faultySource
    .recover {
      case _: RuntimeException => Int.MinValue
    }
    .log("gracefulSource")
    .to(Sink.ignore)
  //.run()

  faultySource
    .recoverWithRetries(3, {
      case _: RuntimeException => Source(90 to 99)
    })
    .log("retrial")
    .to(Sink.ignore)
  //.run()


  RestartSource
    .onFailuresWithBackoff(
      minBackoff = 1 second,
      maxBackoff = 30 second,
      randomFactor = 0.2
    )(() => {
      val randomNumber = new Random().nextInt(20)
      Source(1 to 10).map(elm => if (elm == randomNumber) throw new RuntimeException else elm)
    })
    .log("restartBackoff")
    .to(Sink.ignore)
  //.run()


  Source(1 to 20)
    .map(n => if (n == 13) throw new RuntimeException("bad luck") else n)
    .log("supervision")
    .withAttributes(ActorAttributes.supervisionStrategy {
      case _: RuntimeException => Resume
      case _ => Stop
    })
    .to(Sink.ignore)
    .run()

}
