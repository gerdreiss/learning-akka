package actors

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorSystem, Props}

object CounterExercise extends App {
  case object Increment
  case object Decrement
  case object Print
  class CounterActor extends Actor {
    val counter: AtomicInteger = new AtomicInteger()
    override def receive: Receive = {
      case Increment => counter.incrementAndGet()
      case Decrement => counter.decrementAndGet()
      case Print => println(counter.get())
    }
  }

  val system = ActorSystem("Counters")
  val counter = system.actorOf(Props[CounterActor])
  counter ! Increment
  counter ! Increment
  counter ! Increment
  counter ! Decrement
  counter ! Decrement
  counter ! Increment
  counter ! Increment
  counter ! Print
}
