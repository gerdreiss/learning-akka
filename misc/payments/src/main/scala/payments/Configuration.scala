package payments

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}

object Configuration {
  final case class MerchantId(id: String)
  final case class MerchantConfiguration()

  sealed trait ConfigurationMessage
  final case class RetrieveConfiguration(merchantId: MerchantId, replyTo: ActorRef[ConfigurationResponse]) extends ConfigurationMessage

  sealed trait ConfigurationResponse
  final case class ConfigurationFound(merchantId: MerchantId, configuration: MerchantConfiguration) extends ConfigurationResponse
  final case class ConfigurationNotFound(merchanId: MerchantId)                                     extends ConfigurationResponse

  def apply(): Behavior[ConfigurationMessage] = Behaviors.setup(context => new Configuration(context))
}

import payments.Configuration._

// the AbstractBehavior trait is the entry point for using the object-oriented style API
class Configuration(context: ActorContext[ConfigurationMessage]) extends AbstractBehavior[ConfigurationMessage] {

  // the mutable state here holds the configuration values of each merchant we know about
  private var configurations: Map[MerchantId, MerchantConfiguration] = Map.empty

  // the onMessage method defines the initial behavior applied to a message upon reception
  override def onMessage(msg: ConfigurationMessage): Behavior[ConfigurationMessage] = msg match {
    case RetrieveConfiguration(merchantId, replyTo) =>
      configurations.get(merchantId) match {
        case Some(configuration) =>
          // reply to the sender using the fire-and-forget paradigm
          replyTo ! ConfigurationFound(merchantId, configuration)
        case None =>
          // reply to the sender using the fire-and-forget paradigm
          replyTo ! ConfigurationNotFound(merchantId)
      }
      // lastly, return the Behavior to be applied to the next received message
      // in this case, that's just the same Behavior as we already have
      this
  }
}
