package infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps

object TimersSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("TimersSchedulersDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  system.log.info("Scheduling reminder for simpleActor")

  import system.dispatcher

  system.scheduler.scheduleOnce(1 second) {
    simpleActor ! "reminder"
  }

  val routing = system.scheduler.schedule(1 second, 3 seconds) {
    simpleActor ! "heartbeat"
  }

  system.scheduler.scheduleOnce(10 seconds) {
    routing.cancel()
  }

  class SelfClosingActor extends Actor with ActorLogging {
    var schedule = newTimeoutWindow

    def newTimeoutWindow: Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }
    }

    override def receive: Receive = {
      case "timeout" => log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(s"received $message, staying alive")
        schedule.cancel()
        schedule = newTimeoutWindow
    }
  }

  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")

  system.scheduler.scheduleOnce(250 millis) {
    selfClosingActor ! "ping"
  }

  system.scheduler.scheduleOnce(2 seconds) {
    system.log.info("sending pong to the self-closing actor")
    selfClosingActor ! "pong"
  }

  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startPeriodicTimer(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I am alive")
      case Stop =>
        log.warning("Stopping...")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val heartbeatActor = system.actorOf(Props[TimerBasedHeartbeatActor], "heartbeatActor")
  system.scheduler.scheduleOnce(5 seconds) {
    heartbeatActor ! Stop
  }

}

