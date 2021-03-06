package com.github.gerdreiss.akka.iot

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Terminated}

import scala.concurrent.duration.FiniteDuration


object DeviceGroupQuery {
  case object CollectionTimeout

  def props(
    actorToDeviceId: Map[ActorRef, String],
    requestId:       Long,
    requester:       ActorRef,
    timeout:         FiniteDuration
  ): Props =
    Props(new DeviceGroupQuery(actorToDeviceId, requestId, requester, timeout))
}

class DeviceGroupQuery(
  actorToDeviceId: Map[ActorRef, String],
  requestId:       Long,
  requester:       ActorRef,
  timeout:         FiniteDuration
) extends Actor with ActorLogging {
  import context.dispatcher
  val queryTimeoutTimer: Cancellable = context.system.scheduler.scheduleOnce(timeout, self, DeviceGroupQuery.CollectionTimeout)

  override def preStart(): Unit =
    actorToDeviceId.keysIterator.foreach { deviceActor ⇒
      context.watch(deviceActor)
      deviceActor ! Device.ReadTemperature(0)
    }

  override def postStop(): Unit =
    queryTimeoutTimer.cancel()

  override def receive: Receive =
    waitingForReplies(
      Map.empty,
      actorToDeviceId.keySet
    )

  def waitingForReplies(
    repliesSoFar: Map[String, DeviceGroup.TemperatureReading],
    stillWaiting: Set[ActorRef]
  ): Receive = {

    case Device.RespondTemperature(0, valueOption) ⇒
      val deviceActor = sender()
      val reading = valueOption match {
        case Some(value) ⇒ DeviceGroup.Temperature(value)
        case None        ⇒ DeviceGroup.TemperatureNotAvailable
      }
      receivedResponse(deviceActor, reading, repliesSoFar, stillWaiting)

    case Terminated(deviceActor) ⇒
      receivedResponse(deviceActor, DeviceGroup.DeviceNotAvailable, repliesSoFar, stillWaiting)

    case DeviceGroupQuery.CollectionTimeout ⇒
      val timedOutReplies =
        stillWaiting.map { deviceActor ⇒
          val deviceId = actorToDeviceId(deviceActor)
          deviceId -> DeviceGroup.DeviceTimedOut
        }
      requester ! DeviceGroup.RespondAllTemperatures(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }

  def receivedResponse(
    deviceActor: ActorRef,
    reading: DeviceGroup.TemperatureReading,
    repliesSoFar: Map[String, DeviceGroup.TemperatureReading],
    stillWaiting: Set[ActorRef]
  ): Unit = {
    context.unwatch(deviceActor)
    val deviceId = actorToDeviceId(deviceActor)
    val newStillWaiting = stillWaiting - deviceActor

    val newRepliesSoFar = repliesSoFar + (deviceId -> reading)
    if (newStillWaiting.isEmpty) {
      requester ! DeviceGroup.RespondAllTemperatures(requestId, newRepliesSoFar)
      context.stop(self)
    } else {
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }

}
