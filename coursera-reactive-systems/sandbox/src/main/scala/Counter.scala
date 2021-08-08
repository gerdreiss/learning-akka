import akka.actor.{Actor, Props}

class Counter extends Actor:
  var count = 0

  override def receive = {
    case "incr" => count += 1
    case "get"  => sender ! count
  }

end Counter

class CounterMain extends Actor:
  val counter = context.actorOf(Props[Counter], "counter")

  counter ! "incr"
  counter ! "incr"
  counter ! "incr"
  counter ! "get1"

  override def receive = { case count: Int =>
    println(s"count was $count")
    context.stop(self)
  }

end CounterMain