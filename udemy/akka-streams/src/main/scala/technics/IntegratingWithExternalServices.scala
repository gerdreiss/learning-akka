package technics

import java.time.LocalDate

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.MessageDispatcher
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

object IntegratingWithExternalServices extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("IntegratingWithExternalServices")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def genericExternalService[A, B](data: A): Future[B] = ???

  case class PagerEvent(application: String, description: String, date: LocalDate)

  val eventSource = Source(List(
    PagerEvent("AkkaInfra", "Infrastructure broke", LocalDate.now()),
    PagerEvent("FastDataPipeline", "Illegal elements in the data pipeline", LocalDate.now()),
    PagerEvent("AkkaInfra", "A service stopped responding", LocalDate.now()),
    PagerEvent("SuperFrontend", "A button does not work", LocalDate.now())
  ))

  class PagerActor extends Actor with ActorLogging {
    private val engineers = List("Daniel", "John", "Lady Gaga")
    private val emails = Map(
      "Daniel" -> "daniel@email.com",
      "John" -> "John@email.com",
      "Lady Gaga" -> "LadyGaga@email.com"
    )

    //import actorSystem.dispatcher // <- not recommended for mapAsync
    implicit val dispatcher: MessageDispatcher = actorSystem.dispatchers.lookup("dedicated-dispatcher") // see application.conf

    private def processEvent(pagerEvent: PagerEvent) = {
      val engineerIndex = pagerEvent.date.toEpochDay % engineers.length
      val engineer = engineers(engineerIndex.toInt)
      val engineerEmail = emails(engineer)

      // page the engineer
      log.info(s"Sending engineer $engineerEmail a high priority notification: $pagerEvent")

      Thread.sleep(1000)

      engineerEmail
    }

    override def receive: Receive = {
      case pagerEvent: PagerEvent =>
        sender() ! processEvent(pagerEvent)
    }
  }

  val pagedEmailsSink = Sink.foreach[String](email => println(s"Successfully sent notification to $email"))

  val infraEvents = eventSource.filter(_.application == "AkkaInfra")

  val pagerActor = actorSystem.actorOf(Props[PagerActor], "PagerActor")

  implicit val timeout: Timeout = Timeout(2 seconds)

  val alternativePagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(event => (pagerActor ? event).mapTo[String])

  alternativePagedEngineerEmails.to(pagedEmailsSink).run()

  //val pagedEngineerEmails = infraEvents.mapAsync(parallelism = 4)(PagerService.processEvent)
  //pagedEngineerEmails.to(pagedEmailsSink).run()

}
