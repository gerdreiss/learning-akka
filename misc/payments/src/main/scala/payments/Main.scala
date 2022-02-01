package payments

import akka.actor.typed.ActorSystem
import payments.Payments.PaymentProcessor

object Main extends App {

  ActorSystem[Nothing](PaymentProcessor.payment(), "typed-payment-processor")

}
