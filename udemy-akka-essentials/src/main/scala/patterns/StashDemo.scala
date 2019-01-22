package patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashDemo extends App {

  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  class ResourceActor extends Actor with ActorLogging with Stash {
    private var data: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening resource...")
        unstashAll()
        context become open
      case message =>
        log.info(s"Stashing $message...")
        stash()
    }

    def open: Receive = {
      case Read =>
        log.info(s"I have read $data")
      case Write(d) =>
        log.info(s"I am writing $d...")
        data = d
      case Close =>
        log.info("Closing resource...")
        unstashAll()
        context become closed
      case message =>
        log.info(s"Stashing $message because I can't handle it in the open state")
        stash()
    }
  }

  val system = ActorSystem("StashDemo")
  val actor = system.actorOf(Props[ResourceActor])
  actor ! Read
  actor ! Open
  actor ! Open
  actor ! Read
  actor ! Write("some random resource")
  actor ! Close
  actor ! Read
}
