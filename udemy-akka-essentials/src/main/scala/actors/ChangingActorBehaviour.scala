package actors

import actors.ChangingActorBehaviour.Mom.Nurture
import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehaviour extends App {

  class FussyKid extends Actor {
    import FussyKid._
    import Mom._

    var state: String = HAPPY

    override def receive: Receive = {
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(question) => sender() ! answer(question)
    }

    private def answer(question: String): Answer = if (state == HAPPY) Accept else Reject
  }

  object FussyKid {
    trait Answer
    case object Accept extends Answer
    case object Reject extends Answer
    val HAPPY = "happy"
    val SAD = "sad"
  }

  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, discardOld = false)
      case Food(CHOCOLATE) =>
      case Ask(_) => sender() ! Accept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, discardOld = false)
      case Food(CHOCOLATE) => context.unbecome
      case Ask(_) => sender() ! Reject
    }
  }

  class Mom extends Actor {
    import Mom._
    import FussyKid._
    override def receive: Receive = {
      case Nurture(kid) =>
        kid ! Food(VEGETABLE)
        kid ! Ask("Wanna play?") // Reject
        kid ! Food(VEGETABLE)
        kid ! Ask("Wanna play?") // Reject
        kid ! Food(CHOCOLATE)
        kid ! Ask("Wanna play now?") // Still Reject because Stack not empty
        kid ! Food(CHOCOLATE)
        kid ! Ask("Wanna play now?") // Now the kid is happy, so the happyHandler is used again
      case Accept => println("Yay, the  is happy!")
      case Reject => println("Aww, the kid is sad, but healthy...")
    }
  }

  object Mom {
    case class Nurture(kid: ActorRef)
    case class Food(name: String)
    case class Ask(question: String)
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  val system = ActorSystem("ChangingActorBehaviourDemo")
  val fussyKid = system.actorOf(Props[FussyKid])
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid])
  val mom = system.actorOf(Props[Mom])

  //mom ! Nurture(fussyKid)
  mom ! Nurture(statelessFussyKid)

}
