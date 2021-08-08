/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import akka.actor.*
import scala.collection.immutable.Queue

object BinaryTreeSet:

  trait Operation:
    def requester: ActorRef
    def id: Int
    def elem: Int

  trait OperationReply:
    def id: Int

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection */
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply

  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply



class BinaryTreeSet extends Actor:
  import BinaryTreeSet.*
  import BinaryTreeNode.*

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = true))

  var root = createRoot

  // optional (used to stash incoming operations during garbage collection)
  var pendingQueue = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {

    case op: Operation =>
      root ! op

    case GC =>
      val newRoot = createRoot
      root ! CopyTo(newRoot)
      context become garbageCollecting(newRoot)
  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = {
    case op: Operation =>
      pendingQueue = pendingQueue enqueue op
    case CopyFinished =>
      root ! PoisonPill
      root = newRoot
      pendingQueue.foreach(root ! _)
      pendingQueue = Queue.empty[Operation]
      context become normal
  }



object BinaryTreeNode:
  trait Position

  case object Left extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)
  /**
   * Acknowledges that a copy has been completed. This message should be sent
   * from a node to its parent, when this node and all its children nodes have
   * finished being copied.
   */
  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode],  elem, initiallyRemoved)

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor:
  import BinaryTreeNode.*
  import BinaryTreeSet.*

  var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  // optional
  def receive = normal

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = {
    case op: Insert => doInsert(op)
    case op: Remove => doRemove(op)
    case op: Contains => checkContains(op)
    case op: CopyTo => doCopy(op)
  }

  private def doInsert(op: Insert) =
    if op.elem == this.elem then
      this.removed = false
      op.requester ! OperationFinished(op.id)
    else {
      val position = positionOf(op.elem)
      subtrees.get(position)
        .fold {
          subtrees += (position -> newNode(op.elem))
          op.requester ! OperationFinished(op.id)
        } {
          _ forward op
        }
    }

  private def doRemove(op: Remove) =
    if op.elem == this.elem then
      this.removed = true
      op.requester ! OperationFinished(op.id)
    else
      subtrees.get(positionOf(op.elem))
        .fold {
          op.requester ! OperationFinished(op.id)
        } {
          _ forward op
        }

  private def checkContains(op: Contains) =
    if op.elem == this.elem then
      op.requester ! ContainsResult(op.id, !removed)
    else
      subtrees.get(positionOf(op.elem))
        .fold {
          op.requester ! ContainsResult(op.id, false)
        } {
          _ forward op
        }

  private def doCopy(op: CopyTo) =
    context become copying(subtrees.values.toSet, removed)

    if removed then self ! OperationFinished(0)
    else op.treeNode ! Insert(self, elem, elem)

    subtrees.values.foreach(_ ! op)

  // optional
  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = {
    case OperationFinished(_) =>
      context become copying(expected, true)
      if expected.isEmpty then
        context.parent ! CopyFinished
    case CopyFinished =>
      val newExpected = expected - sender
      if newExpected.isEmpty && insertConfirmed then
        context.parent ! CopyFinished
      else
        context.become(copying(newExpected, insertConfirmed))
  }

  private def positionOf(element: Int): Position =
    if element < this.elem then Left else Right

  private def newNode(newElem: Int) =
    context.actorOf(BinaryTreeNode.props(newElem, false))
