package patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, FSM, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class FsmSpec extends TestKit(ActorSystem("FsmSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  import FsmSpec._

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A vending machine" should {
    "error when not initialized" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! RequestProduct("Snickers")
      expectMsg(VendingError("Machine not initialized"))
    }
    "report a product not available" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("Snickers" -> 10), Map("Snickers" -> 1))
      vendingMachine ! RequestProduct("Mars")
      expectMsg(VendingError("Product not available"))
    }
    "throw a timeout if not money inserted" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("Snickers" -> 1000), Map("Snickers" -> 100))
      vendingMachine ! RequestProduct("Snickers")
      expectMsg(Instruction("Please insert 100 yen"))
      within(1.5 seconds) {
        expectMsg(VendingError("Request timed out"))
      }
    }
    "handle reception of partial money" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("Snickers" -> 1000), Map("Snickers" -> 100))
      vendingMachine ! RequestProduct("Snickers")
      expectMsg(Instruction("Please insert 100 yen"))
      vendingMachine ! ReceiveMoney(10)
      expectMsg(Instruction("Please insert 90 yen"))
      within(1.5 seconds) {
        expectMsg(VendingError("Request timed out"))
        expectMsg(GiveBackChange(10))
      }
    }
    "deliver the product if the price is paid" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("Snickers" -> 1000), Map("Snickers" -> 100))
      vendingMachine ! RequestProduct("Snickers")
      expectMsg(Instruction("Please insert 100 yen"))
      vendingMachine ! ReceiveMoney(50)
      expectMsg(Instruction("Please insert 50 yen"))
      vendingMachine ! ReceiveMoney(50)
      expectMsg(Deliver("Snickers"))
    }
    "give back change and deliver the product if the price is overpaid" in {
      val vendingMachine = system.actorOf(Props[VendingMachine])
      vendingMachine ! Initialize(Map("Snickers" -> 1000), Map("Snickers" -> 100))
      vendingMachine ! RequestProduct("Snickers")
      expectMsg(Instruction("Please insert 100 yen"))
      vendingMachine ! ReceiveMoney(50)
      expectMsg(Instruction("Please insert 50 yen"))
      vendingMachine ! ReceiveMoney(70)
      expectMsg(Deliver("Snickers"))
      expectMsg(GiveBackChange(20))
    }
  }

  "A vending machine FSM" should {
    "error when not initialized" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! RequestProduct("Snickers")
      expectMsg(VendingError("Machine not initialized"))
    }
    "report a product not available" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Initialize(Map("Snickers" -> 10), Map("Snickers" -> 1))
      vendingMachine ! RequestProduct("Mars")
      expectMsg(VendingError("Product not available"))
    }
    "throw a timeout if not money inserted" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Initialize(Map("Snickers" -> 1000), Map("Snickers" -> 100))
      vendingMachine ! RequestProduct("Snickers")
      expectMsg(Instruction("Please insert 100 yen"))
      within(1.5 seconds) {
        expectMsg(VendingError("Request timed out"))
      }
    }
    "handle reception of partial money" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Initialize(Map("Snickers" -> 1000), Map("Snickers" -> 100))
      vendingMachine ! RequestProduct("Snickers")
      expectMsg(Instruction("Please insert 100 yen"))
      vendingMachine ! ReceiveMoney(10)
      expectMsg(Instruction("Please insert 90 yen"))
      within(1.5 seconds) {
        expectMsg(VendingError("Request timed out"))
        expectMsg(GiveBackChange(10))
      }
    }
    "deliver the product if the price is paid" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Initialize(Map("Snickers" -> 1000), Map("Snickers" -> 100))
      vendingMachine ! RequestProduct("Snickers")
      expectMsg(Instruction("Please insert 100 yen"))
      vendingMachine ! ReceiveMoney(50)
      expectMsg(Instruction("Please insert 50 yen"))
      vendingMachine ! ReceiveMoney(50)
      expectMsg(Deliver("Snickers"))
    }
    "give back change and deliver the product if the price is overpaid" in {
      val vendingMachine = system.actorOf(Props[VendingMachineFSM])
      vendingMachine ! Initialize(Map("Snickers" -> 1000), Map("Snickers" -> 100))
      vendingMachine ! RequestProduct("Snickers")
      expectMsg(Instruction("Please insert 100 yen"))
      vendingMachine ! ReceiveMoney(50)
      expectMsg(Instruction("Please insert 50 yen"))
      vendingMachine ! ReceiveMoney(70)
      expectMsg(Deliver("Snickers"))
      expectMsg(GiveBackChange(20))
    }
  }
}


object FsmSpec {

  case class Initialize(invertory: Map[String, Int], prices: Map[String, Int])
  case class RequestProduct(product: String)
  case class Instruction(text: String)
  case class ReceiveMoney(amount: Int)
  case class Deliver(product: String)
  case class GiveBackChange(amount: Int)
  case class VendingError(reason: String)
  case object ReceiveMoneyTimeout

  class VendingMachine extends Actor with ActorLogging {

    override def receive: Receive = idle

    def idle: Receive = {
      case Initialize(invertory, prices) => context.become(operational(invertory, prices))
      case _ => sender() ! VendingError("Machine not initialized")
    }

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = {
      case RequestProduct(product) => inventory.get(product) match {
        case None | Some(0) => sender() ! VendingError("Product not available")
        case Some(_) =>
          val price = prices(product)
          sender() ! Instruction(s"Please insert $price yen")
          context.become(waitForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule, sender()))
      }
    }

    def waitForMoney(
      inventory: Map[String, Int],
      prices: Map[String, Int],
      product: String,
      money: Int,
      moneyTimeoutSchedule: Cancellable,
      requester: ActorRef
    ): Receive = {

      case ReceiveMoneyTimeout =>
        requester ! VendingError("Request timed out")
        if (money > 0) requester ! GiveBackChange(money)
        context.become(operational(inventory, prices))

      case ReceiveMoney(amount) =>
        moneyTimeoutSchedule.cancel()
        val price = prices(product)
        val total = money + amount
        val change = total - price
        if (change >= 0) {
          requester ! Deliver(product)
          if (change > 0) {
            requester ! GiveBackChange(change)
          }
          context.become(operational(inventory.updated(product, inventory(product) - 1), prices))
        } else {
          requester ! Instruction(s"Please insert ${math.abs(change)} yen")
          context.become(waitForMoney(inventory, prices, product, total, startReceiveMoneyTimeoutSchedule, requester))
        }
    }

    def startReceiveMoneyTimeoutSchedule: Cancellable = context.system.scheduler.scheduleOnce(1 second) {
      self ! ReceiveMoneyTimeout
    }
  }

  /********************************************************************************************************************
    **
    **  ______   _____   __  __
    ** |  ____| / ____| |  \/  |
    ** | |__   | (___   | \  / |
    ** |  __|   \___ \  | |\/| |
    ** | |      ____) | | |  | |
    ** |_|     |_____/  |_|  |_|
    **
    **/
  trait VendingState
  case object Idle extends VendingState
  case object Operational extends VendingState
  case object WaitingForMoney extends VendingState

  trait VendingData
  case object Uninitialized extends VendingData
  case class Initialized(inventory: Map[String, Int], prices: Map[String, Int]) extends VendingData
  case class WaitForMoneyData(inventory: Map[String, Int], prices: Map[String, Int], product: String, money: Int, requester: ActorRef) extends VendingData

  class VendingMachineFSM extends FSM[VendingState, VendingData] {
    // initialize the state machine
    startWith(Idle, Uninitialized)

    when(Idle) {
      case Event(Initialize(inventory, prices), Uninitialized) =>
        goto(Operational) using Initialized(inventory, prices)
      case _ =>
        sender() ! VendingError("Machine not initialized")
        stay()
    }

    when(Operational) {
      case Event(RequestProduct(product), Initialized(inventory, prices)) => inventory.get(product) match {
        case None | Some(0) =>
          sender() ! VendingError("Product not available")
          stay()
        case Some(_) =>
          val price = prices(product)
          sender() ! Instruction(s"Please insert $price yen")
          goto(WaitingForMoney) using WaitForMoneyData(inventory, prices, product, 0, sender())
      }
    }

    when(WaitingForMoney, stateTimeout = 1 second) {
      case Event(StateTimeout, WaitForMoneyData(inventory, prices, product, money, requester)) =>
        requester ! VendingError("Request timed out")
        if (money > 0) requester ! GiveBackChange(money)
        goto(Operational) using Initialized(inventory, prices)

      case Event(
            ReceiveMoney(amount),
            WaitForMoneyData(
            inventory: Map[String, Int],
            prices: Map[String, Int],
            product: String,
            money: Int,
            requester: ActorRef)) =>
        val price = prices(product)
        val total = money + amount
        val change = total - price
        if (change >= 0) {
          requester ! Deliver(product)
          if (change > 0) {
            requester ! GiveBackChange(change)
          }
          goto(Operational) using Initialized(inventory.updated(product, inventory(product) - 1), prices)
        } else {
          requester ! Instruction(s"Please insert ${math.abs(change)} yen")
          stay() using WaitForMoneyData(inventory, prices, product, total, requester)
        }
    }

    whenUnhandled {
      case Event(_, _) =>
        sender() ! VendingError("Do not understand")
        stay()
    }

    onTransition {
      case stateA -> stateB =>
        log.info(s"Transitioning from $stateA to $stateB")
    }

    initialize()
  }

}