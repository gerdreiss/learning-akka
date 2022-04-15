package bank.actors

import akka.actor.typed.ActorRef

// state
case class State(accounts: Map[String, ActorRef[Command]] = Map.empty)

case class BankAccount(
    id: String,
    user: String,
    currency: String,
    balance: Double
)

// commands = messages
sealed trait Command

object Command {
  case class CreateBankAccount(
      user: String,
      currency: String,
      initialBalance: Double,
      replyTo: ActorRef[Response]
  ) extends Command

  case class UpdateBalance(
      id: String,
      currency: String,
      amount: Double, // can be both positive and negative
      replyTo: ActorRef[Response]
  ) extends Command

  case class GetBankAccount(
      id: String,
      replyTo: ActorRef[Response]
  ) extends Command
}

// responses
sealed trait Response
object Response {
  case class BankAccountCreatedResponse(id: String) extends Response
  case class BankAccountBalanceUpdatedResponse(maybeBankAccount: Option[BankAccount])
      extends Response
  case class GetBankAccountResponse(maybeBankAccount: Option[BankAccount]) extends Response
}
