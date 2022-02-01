package actors


import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import actors.BasicSpec._

  "An echo actor" should {
    "send back the same message" in {
      val actor = system.actorOf(Props[SimpleActor])
      val message = "hello, test!"
      actor ! message

      expectMsg(message)
    }
  }
  "A blackhole actor" should {
    "send back some message" in {
      val actor = system.actorOf(Props[BlackHole])
      val message = "hello, test!"
      actor ! message

      expectNoMessage(1 second)
    }
  }
  "A lab test actor" should {
    val actor = system.actorOf(Props[LabTestActor])
    "return the string message in upper case" in {
      actor ! "Akka is nice"
      val reply = expectMsgType[String]
      assert(reply == "AKKA IS NICE")
    }
    "reply to a greeting" in {
      actor ! "greeting"
      expectMsgAnyOf("hi", "hello")
    }
    "reply with favorite tech" in {
      actor ! "favoriteTech"
      expectMsgAllOf("Scala", "Akka")
    }
    "reply with favorite tech in a different way" in {
      actor ! "favoriteTech"
      val messages = receiveN(2)
      // do more complex assertions
      assert(messages == Seq("Scala", "Akka"))
    }
    "reply with favorite tech in a fancy way" in {
      actor ! "favoriteTech"
      expectMsgPF() {
        case "Scala" =>
        case "Akka" =>
      }
    }
  }
}

object BasicSpec {
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }
  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }
  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" =>
        val reply = if (random.nextBoolean()) "hi" else "hello"
        sender() ! reply
      case "favoriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String =>
        sender() ! message.toUpperCase
    }
  }
}