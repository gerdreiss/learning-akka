package faulttolerance

import akka.actor.{ActorRef, ActorSystem, Props, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import ActorSupervision._

  "A supervisor" should {
    "resume its child in case of a minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor1")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Lorem ipsum dolor"
      child ! Report
      expectMsg(3)

      child ! "Lorem ipsum dolor sit amet, mea atqui alterum ea, tota etiam erroribus per an."
      child ! Report
      expectMsg(3)
    }
    "restart its child in case of a sentence starting with lower case" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor2")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Lorem ipsum dolor"
      child ! Report
      expectMsg(3)

      child ! "lorem ipsum dolor"
      child ! Report
      expectMsg(0)
    }
    "terminate its child in case of an empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor3")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)

      child ! ""
      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == child)
    }
    "escalate an error when it does not know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor4")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)

      child ! 0
      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == child)
    }
  }
  "A kinder supervisor" should {
    "not kill its children in case it is restarted or escalates failures" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "kinderSupervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Lorem ipsum dolor"
      child ! Report
      expectMsg(3)

      child ! 0
      child ! Report
      expectMsg(0)
    }
  }
  "An all-for-one supervisor" should {
    "apply the all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor], "allForOneSupervisor")
      supervisor ! Props[FussyWordCounter]
      val child1 = expectMsgType[ActorRef]
      supervisor ! Props[FussyWordCounter]
      val child2 = expectMsgType[ActorRef]

      child2 ! "Lorem ipsum dolor"
      child2 ! Report
      expectMsg(3)

      EventFilter[LowerCaseException]() intercept {
        child1 ! "lorem ipsum dolor"
      }

      Thread.sleep(1000)

      child2 ! Report
      expectMsg(0)
    }
  }
}
