package bank.actors

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

  // events = to persist to Cassandra
  trait Event
  object Event {
    case class BankAccountCreated(bankAccount: BankAccount) extends Event
    case class BalanceUpdated(amount: Double) extends Event
  }

  import Command._
  import Event._
  import Response._

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
      if (newBalance < 0)
        Effect.reply(replyTo)(BankBalanceUpdatedResponse(None))
      else
        Effect
          .persist(BalanceUpdated(amount))
          .thenReply(replyTo)(newState => BankBalanceUpdatedResponse(Some(newState)))

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
