package com.github.gerdreiss.akka.iot

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

import scala.concurrent.duration.DurationLong


object DeviceGroup {
  def props(groupId: String): Props = Props(new DeviceGroup(groupId))

  final case class RequestDeviceList(requestId: Long)
  final case class ReplyDeviceList(requestId: Long, ids: Set[String])

  final case class RequestAllTemperatures(requestId: Long)
  final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

  sealed trait TemperatureReading
  final case class Temperature(value: Double) extends TemperatureReading
  final case object TemperatureNotAvailable extends TemperatureReading
  final case object DeviceNotAvailable extends TemperatureReading
  final case object DeviceTimedOut extends TemperatureReading
}

class DeviceGroup(groupId: String) extends Actor with ActorLogging {
  var deviceIdToActor = Map.empty[String, ActorRef]
  var actorToDeviceId = Map.empty[ActorRef, String]

  var nextCollectionId = 0L

  override def preStart(): Unit = log.info("DeviceGroup {} started", groupId)
  override def postStop(): Unit = log.info("DeviceGroup {} stopped", groupId)

  override def receive: Receive = {
    case trackMsg @ DeviceManager.RequestTrackDevice(`groupId`, _) ⇒
      deviceIdToActor.get(trackMsg.deviceId) match {
        case Some(deviceActor) ⇒
          deviceActor forward trackMsg
        case None ⇒
          log.info("Creating device actor for {}", trackMsg.deviceId)
          val deviceActor = context.actorOf(Device.props(groupId, trackMsg.deviceId), s"device-${trackMsg.deviceId}")
          context.watch(deviceActor)
          deviceIdToActor += trackMsg.deviceId -> deviceActor
          actorToDeviceId += deviceActor -> trackMsg.deviceId
          deviceActor forward trackMsg
      }

    case DeviceManager.RequestTrackDevice(grpId, _) ⇒
      log.warning(
        "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
        grpId, groupId
      )

    case DeviceGroup.RequestDeviceList(requestId) ⇒
      sender() ! DeviceGroup.ReplyDeviceList(requestId, deviceIdToActor.keySet)

    case Terminated(deviceActor) ⇒
      val deviceId = actorToDeviceId(deviceActor)
      log.info("Device actor for {} has been terminated", deviceId)
      actorToDeviceId -= deviceActor
      deviceIdToActor -= deviceId

    case DeviceGroup.RequestAllTemperatures(requestId) ⇒
      context.actorOf(DeviceGroupQuery.props(
        actorToDeviceId = actorToDeviceId,
        requestId = requestId,
        requester = sender(),
        3.seconds
      ))
  }
}
