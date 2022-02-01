import akka.actor.typed.*
import java.time.LocalDate

object Protocols:

  case class RequestQuote(title: String, buyer: ActorRef[Quote])

  case class Quote(price: BigDecimal, seller: ActorRef[BuyOrQuite])

  enum BuyOrQuite:
    case Buy(address: Address, buyer: ActorRef[Shipping])
    case Quit

  case class Shipping(date: LocalDate)

  case class Address(street: String, location: String)
