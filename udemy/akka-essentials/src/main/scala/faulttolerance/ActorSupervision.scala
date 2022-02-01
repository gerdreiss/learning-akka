package faulttolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy}

object ActorSupervision {

  case class EmptySentenceException(message: String) extends RuntimeException(message)
  case class LongSentenceException(message: String) extends RuntimeException(message)
  case class LowerCaseException(message: String) extends RuntimeException(message)

  case object Report

  class FussyWordCounter extends Actor {
    var words = 0

    override def receive: Receive = {
      case Report => sender() ! words
      case sentence: String =>
        validate(sentence)
        countWords(sentence)
      case _ => throw new IllegalArgumentException("can only receive strings")
    }

    private def validate(sentence: String): Unit = {
      if (sentence.isEmpty)                   throw EmptySentenceException("empty sentence")
      if (sentence.length > 20)               throw LongSentenceException("too long sentence")
      if (Character.isLowerCase(sentence(0))) throw LowerCaseException("lower case sentence")
    }

    private def countWords(sentence: String): Unit = {
      words += sentence.split(" ").length
    }
  }

  class Supervisor extends Actor {
    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: EmptySentenceException => Stop
      case _: LongSentenceException  => Resume
      case _: LowerCaseException     => Restart
      case _: Exception              => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        sender() ! context.actorOf(props)
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty
    }
  }

  class AllForOneSupervisor extends Supervisor {
    override val supervisorStrategy = AllForOneStrategy() {
      case _: EmptySentenceException => Stop
      case _: LongSentenceException  => Resume
      case _: LowerCaseException     => Restart
      case _: Exception              => Escalate
    }
  }
}
