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
