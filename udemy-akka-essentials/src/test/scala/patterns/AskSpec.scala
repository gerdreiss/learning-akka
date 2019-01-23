package patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._
  "An authenticator" should {
    "fail to authenticate a non-registered user" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! AuthenticateUser("daniel", "password")
      expectMsg(AuthenticationFailure("user not found"))
    }
    "fail to authenticate if invalid password" in {
      val authManager = system.actorOf(Props[AuthManager])
      authManager ! RegisterUser("daniel", "password")
      authManager ! AuthenticateUser("daniel", "wrong")
      expectMsg(AuthenticationFailure("password incorrect"))
    }
  }
}

object AskSpec {

  case class Read(key: String)
  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map.empty)

    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying to read the value at the key $key")
        sender() ! kv.get(key) // Option[String]
      case Write(key, value) =>
        log.info(s"Writing the value $value for the key $key")
        context.become(online(kv + (key -> value)))
    }
  }

  case class RegisterUser(name: String, pass: String)
  case class AuthenticateUser(name: String, pass: String)
  case class AuthenticationFailure(message: String)
  case object AuthenticationSuccess

  class AuthManager extends Actor with ActorLogging {
    implicit val timeout: Timeout = Timeout(3 second)
    implicit val execCtx: ExecutionContext = context.dispatcher

    private val authDB = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(name, pass) =>
        authDB ! Write(name, pass)
      case AuthenticateUser(name, pass) =>
        // important to get the sender before entering onComplete
        val orgSender = sender()
        authDB ? Read(name) onComplete {
          case Success(None) => orgSender ! AuthenticationFailure("user not found")
          case Success(Some(pwd)) =>
            if (pwd == pass) sender() ! AuthenticationSuccess
            else sender() ! AuthenticationFailure("password incorrect")
          case Failure(_) =>
            orgSender ! AuthenticationFailure("system error")
        }
    }
  }
}