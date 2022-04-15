package bank.app

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import bank.actors.Bank
import bank.actors.Command

import scala.concurrent.Future

import concurrent.duration._
import scala.concurrent.ExecutionContext
import akka.http.scaladsl.Http
import bank.http.BankRouter
import scala.util.Success
import scala.util.Failure

object BankApp extends App {

  trait RootCommand
  case class RetrieveBankActor(replyTo: ActorRef[ActorRef[Command]]) extends RootCommand

  val rootBehavior: Behavior[RootCommand] = Behaviors.setup { context =>
    val bankActor = context.spawn(Bank(), "bank")

    Behaviors.receiveMessage { //
      case RetrieveBankActor(replyTo) =>
        replyTo ! bankActor
        Behaviors.same
    }
  }

  implicit val system: ActorSystem[RootCommand] = ActorSystem(rootBehavior, "BankSystem")
  implicit val ec: ExecutionContext             = system.executionContext
  implicit val timeout: Timeout                 = Timeout(5.seconds)

  def startHttpServer(bank: ActorRef[Command]): Unit = {
    val router = new BankRouter(bank)
    val routes = router.routes

    Http()
      .newServerAt("localhost", 8080)
      .bind(routes)
      .onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
        case Failure(e) =>
          system.log.error("Failed to bind HTTP endpoint, terminating system", e)
          system.terminate()
      }
  }

  val bankActorFuture: Future[ActorRef[Command]] = system.ask(replyTo => RetrieveBankActor(replyTo))
  bankActorFuture.foreach(startHttpServer)

}
