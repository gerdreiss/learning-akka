package bank.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.persistence.typed._
import akka.persistence.typed.scaladsl._

import java.util.UUID

object Bank {

  // commands = messages
  import Command._
  import Response._

  // events
  sealed trait Event
  object Event {
    case class BankAccountCreated(id: String) extends Event
  }

  def commandHandler(context: ActorContext[Command]): (State, Command) => Effect[Event, State] = {
    (state, command) =>
      command match {
        case createCommand: CreateBankAccount =>
          val id = UUID.randomUUID().toString
          val newBankAccount = context.spawn(PersistentBankAccount(id), id)
          Effect
            .persist(Event.BankAccountCreated(id))
            .thenReply(newBankAccount)(_ => createCommand)

        case updateCommand @ UpdateBalance(id, _, _, replyTo) =>
          state.accounts.get(id) match {
            case Some(bankAccount) => Effect.reply(bankAccount)(updateCommand)
            case None              => Effect.reply(replyTo)(BankBalanceUpdatedResponse(None))
          }

        case getCommand @ GetBankAccount(id, replyTo) =>
          state.accounts.get(id) match {
            case Some(bankAccount) => Effect.reply(bankAccount)(getCommand)
            case None              => Effect.reply(replyTo)(GetBankAccountResponse(None))
          }
      }
  }

  def eventHandler(context: ActorContext[Command]): (State, Event) => State = {
    case (state, Event.BankAccountCreated(id)) =>
      // exists after the command handler, does not exist in the recovery mode
      val account = context
        .child(id)
        .getOrElse(context.spawn(PersistentBankAccount(id), id))
        .asInstanceOf[ActorRef[Command]]
      state.copy(accounts = state.accounts + (id -> account))
  }

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("bank"),
      emptyState = State(),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context)
    )
  }
}
