package infrastructure


import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.util.Random

object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    private var counter = new AtomicInteger()
    override def receive: Receive = {
      case message =>
        log.info(s"${counter.incrementAndGet()} $message")
    }
  }

  val system = ActorSystem("DispatchersDemo"/*, ConfigFactory.load().getConfig("dispatcherDemo")*/)

  val simpleCounters = (1 to 10) map { i =>
    system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")
  }

  val r = new Random()
  (1 to 1000) foreach {
    simpleCounters(r.nextInt(10)) ! _
  }

  val rtjvmActor = system.actorOf(Props[Counter], "rtjvm")
  (1 to 1000) foreach {
    rtjvmActor ! _
  }

}
