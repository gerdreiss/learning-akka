package actors

import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior

// a single bank account
object PersistentBankAccount {
  // Reasons for using Event Sourcing
  // - fault tolerance
  // - auditing

  // commands = messages
  sealed trait Command
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

  // events = to persist to Cassandra
  trait Event
  case class BankAccountCreated(
      bankAccount: BankAccount
  ) extends Event
  case class BalanceUpdated(
      amount: Double
  ) extends Event

  // state
  case class BankAccount(
      id: String,
      user: String,
      currency: String,
      balance: Double
  )

  // responses
  sealed trait Response
  case class BankAccountCreatedResponse(
      id: String
  ) extends Response
  case class BankBalanceUpdatedResponse(
      maybeBankAccount: Option[BankAccount]
  ) extends Response
  case class GetBankAccountResponse(
      maybeBankAccount: Option[BankAccount]
  ) extends Response

  // command handler = message handler => persist an event
  // event handler => update state
  // state

  val commandHandler: (BankAccount, Command) => Effect[Event, BankAccount] = {
    case (state, CreateBankAccount(user, currency, initialBalance, replyTo)) =>
      /** Steps:
        *   - bank creates me
        *   - bank sends me CreateBankAccount
        *   - I persist BankAccountCreated
        *   - I update my state
        *   - reply back to bank with BankAccountCreatedResponse
        *   - the bank surfaces the response to the HTTP server
        */
      Effect
        .persist(BankAccountCreated(BankAccount(state.id, user, currency, initialBalance)))
        .thenReply(replyTo)(_ => BankAccountCreatedResponse(state.id))
    case (state, UpdateBalance(_, _, amount, replyTo)) =>
      val newBalance = state.balance + amount
      if (newBalance < 0) {
        Effect.reply(replyTo)(BankBalanceUpdatedResponse(None))
      } else {
        Effect
          .persist(BalanceUpdated(amount))
          .thenReply(replyTo)(newState => BankBalanceUpdatedResponse(Some(newState)))
      }
    case (state, GetBankAccount(_, replyTo)) =>
      Effect.reply(replyTo)(GetBankAccountResponse(Some(state)))
  }

  val eventHandler: (BankAccount, Event) => BankAccount = {
    case (state, BankAccountCreated(bankAccountCreated)) => bankAccountCreated
    case (state, BalanceUpdated(amount)) => state.copy(balance = state.balance + amount)
  }

  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, BankAccount](
      persistenceId = PersistenceId.ofUniqueId(id),
      emptyState = BankAccount(id, "", "", 0.0),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}
