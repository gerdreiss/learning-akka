package actors

import java.util.concurrent.atomic.AtomicLong

import akka.actor.Actor

object BankAccountExercise extends App {

  case class Deposit(amount: BigDecimal)
  case class Withdraw(amount: BigDecimal)
  case object Statement
  case class Success(balance: BigDecimal)
  case object Failure

  class BankAccount extends Actor {
    val balance = new AtomicLong(0L)

    override def receive: Receive = {
      case Deposit(amount) if amount.longify > 0L =>
        balance.addAndGet(amount.longify)
        sender() ! Success(BigDecimal(balance.doubleValue()/100.0))
      case Withdraw(amount) if amount.longify <= balance.get() =>
        balance.addAndGet(amount.negate.longify)
        sender() ! Success(BigDecimal(balance.doubleValue()/100.0))
      case Statement =>
        sender() ! Success(BigDecimal(balance.doubleValue()/100.0))
      case _ => Failure
    }
  }

  implicit class CurrencyLongifier(amount: BigDecimal) {
    def longify: Long = (amount * BigDecimal(100)).longValue()
    def negate: BigDecimal = amount * BigDecimal(-1)
  }

}
