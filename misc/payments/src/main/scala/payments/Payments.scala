package payments

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Payments {

  object PaymentProcessor {

    def payment(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      context.log.info("Typed Payment Processor started")
      context.spawn(Configuration(), "config")
      Behaviors.empty
    }

  }

}
