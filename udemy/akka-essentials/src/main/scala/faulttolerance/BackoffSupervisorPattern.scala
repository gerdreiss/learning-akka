package faulttolerance

import java.io.File

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{Backoff, BackoffSupervisor}

import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object BackoffSupervisorPattern extends App {

  case object ReadFile

  class FilebasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = _

    override def preStart(): Unit = log.info("Persistent actor starting...")
    override def postStop(): Unit = log.warning("Persistent actor stopped")
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.warning("Persistent actor restarting...")

    override def receive: Receive = {
      case ReadFile =>
        if (dataSource == null)
          dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
        log.info(s"I've just read some IMPORTANT data:\n\n${dataSource.getLines().mkString("\n")}")
    }
  }

  val system = ActorSystem("BackoffSupervisorDemo")
  val actor = system.actorOf(Props[FilebasedPersistentActor], "simpleActor")
  actor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    Backoff.onFailure(
      Props[FilebasedPersistentActor],
      "simpleBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    )
  )

  val simpleBackoffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
  simpleBackoffSupervisor ! ReadFile


  val stopSupervisorProps = BackoffSupervisor.props(Backoff
    .onStop(
      Props[FilebasedPersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    )
    .withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

  val stopSupervisor = system.actorOf(stopSupervisorProps, "stopSupervisor")
  stopSupervisor ! ReadFile

  class EagerFBPActor extends FilebasedPersistentActor {
    override def preStart(): Unit = {
      log.info("Actor starting...")
      dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
    }
  }

  //val eagerActor = system.actorOf(Props[EagerFBPActor], "eagerActor")
  val repeatedSupervisorPros = BackoffSupervisor.props(
    Backoff.onStop(
      Props[EagerFBPActor],
      "eagerActor",
      1 second,
      30 seconds,
      0.1
    )
  )

}
