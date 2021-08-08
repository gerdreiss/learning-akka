import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive

object BankAccount:

  case class Deposit(amount: BigInt):
    require(amount > 0)

  case class Withdraw(amount: BigInt):
    require(amount > 0)

  case object Done
  case object Failed

end BankAccount

class BankAccount extends Actor:
  import BankAccount.*

  var balance = BigInt(0)

  def receive = {

    case Deposit(amount) =>
      balance += amount
      sender ! Done

    case Withdraw(amount) =>
      balance -= amount
      sender ! Done

    case _ =>
      sender ! Failed
  }

end BankAccount

object WireTransfer:

  case class Transfer(from: ActorRef, to: ActorRef, amount: BigInt)
  case object Done
  case object Failed

end WireTransfer

class WireTransfer extends Actor:
  import WireTransfer.*

  def receive: Receive =
    LoggingReceive {
      // keep on this line
      case Transfer(from, to, amount) =>
        from ! BankAccount.Withdraw(amount)
        context.become(awaitWithdraw(to, amount, sender))
    }

  def awaitWithdraw(to: ActorRef, amount: BigInt, client: ActorRef): Receive =
    LoggingReceive {
      case BankAccount.Done   =>
        to ! BankAccount.Deposit(amount)
        context.become(awaitDeposit(client))
      case BankAccount.Failed =>
        client ! Failed
        context.stop(self)
    }

  def awaitDeposit(client: ActorRef): Receive =
    LoggingReceive {
      // keep on this line
      case BankAccount.Done =>
        client ! Done
        context.stop(self)
    }

end WireTransfer

class TransferMain extends Actor:
  val accountA = context.actorOf(Props[BankAccount], "accountA")
  val accountB = context.actorOf(Props[BankAccount], "accountB")

  accountA ! BankAccount.Deposit(100)

  def receive = LoggingReceive { case BankAccount.Done =>
    transfer(150)
  }

  def transfer(amount: BigInt): Unit = {
    val transaction = context.actorOf(Props[WireTransfer], "transfer")

    transaction ! WireTransfer.Transfer(accountA, accountB, amount)

    context.become(LoggingReceive {
      case WireTransfer.Done   =>
        println("success")
        context.stop(self)
      case WireTransfer.Failed =>
        println("failed")
        context.stop(self)
    })
  }

end TransferMain
