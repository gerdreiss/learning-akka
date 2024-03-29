package kvstore

import akka.actor.{ OneForOneStrategy, PoisonPill, Props, SupervisorStrategy, Terminated, ActorRef, Actor, Cancellable, actorRef2Scala }
import kvstore.Arbiter.*
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration.*
import akka.util.Timeout

object Replica:
  sealed trait Operation:
    def key: String
    def id: Long
  case class Insert(key: String, value: String, id: Long) extends Operation
  case class Remove(key: String, id: Long) extends Operation
  case class Get(key: String, id: Long) extends Operation

  sealed trait OperationReply
  case class OperationAck(id: Long) extends OperationReply
  case class OperationFailed(id: Long) extends OperationReply
  case class GetResult(key: String, valueOption: Option[String], id: Long) extends OperationReply

  def props(arbiter: ActorRef, persistenceProps: Props): Props = Props(Replica(arbiter, persistenceProps))

class Replica(val arbiter: ActorRef, persistenceProps: Props) extends Actor:
  import Replica.*
  import Replicator.*
  import Persistence.*
  import context.dispatcher

  /*
   * The contents of this actor is just a suggestion, you can implement it in any way you like.
   */
  
  var kv = Map.empty[String, String]
  // a map from secondary replicas to replicators
  var secondaries = Map.empty[ActorRef, ActorRef]
  // the current set of replicators
  var replicators = Set.empty[ActorRef]
  // the scheduled cancellables that will be cancelled when the respective response is received.
  var timeouts = Map.empty[Operation, Cancellable]

  arbiter ! Join

  def receive =
    case JoinedPrimary   => context.become(leader)
    case JoinedSecondary => context.become(replica)

  /* TODO Behavior for  the leader role. */
  val leader: Receive =
    case op: Insert   => doInsert(op)
    case op: Remove   => doRemove(op)
    case op: Get      => doGet(op)
    case op: Replicas => processNewReplicas(op)

  /* TODO Behavior for the replica role. */
  val replica: Receive =
    case op: Get => doGet(op)

  private def doInsert(op: Insert) =
    kv += op.key -> op.value
    //schedule(op)
    ack(op)

  private def doRemove(op: Remove) =
    kv -= op.key
    //schedule(op)
    ack(op)

  private def doGet(op: Get) =
    sender ! GetResult(op.key, kv.get(op.key), op.id)

  private def schedule(op: Operation) =
    timeouts += op -> context.system.scheduler.scheduleOnce(1.second, sender, OperationFailed(op.id))
 
  private def ack(op: Operation) =
    sender ! OperationAck(op.id)

  private def processNewReplicas(op: Replicas) =
    val added = op.replicas -- secondaries.keySet - self
    val removed = secondaries.keySet -- op.replicas

    removed.foreach { r =>
      secondaries(r) ! PoisonPill
      secondaries -= r
      replicators -= r
    }

    added.foreach { r =>
      val repl = context.actorOf(Replicator.props(r))
      replicators += repl
      secondaries += r -> repl
    }
