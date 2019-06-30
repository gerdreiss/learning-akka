package technics

import java.time.LocalDate

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._

object AdvancedBackpressure extends App {

  implicit val actorSystem: ActorSystem = ActorSystem("AdvancedBackpressure")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val controlledFlow = Flow[Int].map(_ * 2).buffer(10, OverflowStrategy.dropHead)

  case class PagerEvent(desc: String, date: LocalDate, nInstances: Int = 1)

  case class Notification(email: String, pagerEvent: PagerEvent)

  val events = List(
    PagerEvent("Infrastructure broke", LocalDate.now()),
    PagerEvent("Illegal elements in the data pipeline", LocalDate.now()),
    PagerEvent("A service stopped responding", LocalDate.now()),
    PagerEvent("A button does not work", LocalDate.now())
  )

  val eventSource = Source(events)

  val onCallEngineer = "daniel@mail.com"

  def sendEmail(notification: Notification): Unit =
    println(s"Dear ${notification.email}, event: ${notification.pagerEvent}")

  val notificationSink = Flow[PagerEvent]
    .map(event => Notification(onCallEngineer, event))
    .to(Sink.foreach[Notification](sendEmail))

  //eventSource.to(notificationSink).run()

  def slowSendEmail(notification: Notification): Unit = {
    Thread.sleep(3000)
    sendEmail(notification)
  }

  val aggregateNotificationFlow = Flow[PagerEvent]
    // when source is not backpressurable
    .conflate((event1, event2) => {
    val nInstances = event1.nInstances + event2.nInstances
    PagerEvent(s"Instances: $nInstances", LocalDate.now(), nInstances)
  })
    .map(event => Notification(onCallEngineer, event))

  //eventSource.via(aggregateNotificationFlow).async.to(Sink.foreach[Notification](slowSendEmail)).run()

  val slowCounter = Source(Stream.from(1)).throttle(1, 1.second)
  val hungrySink = Sink.foreach[Int](println)

  val extrapolator = Flow[Int].extrapolate(Iterator.from)
  val repeater = Flow[Int].extrapolate(elm => Iterator.continually(elm))
  val expander = Flow[Int].expand(Iterator.from)

  //slowCounter.via(extrapolator).to(hungrySink).run()
  //slowCounter.via(repeater).to(hungrySink).run()
  slowCounter.via(expander).to(hungrySink).run()
}
