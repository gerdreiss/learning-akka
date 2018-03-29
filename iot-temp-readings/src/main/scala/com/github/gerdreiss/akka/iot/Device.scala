package com.github.gerdreiss.akka.iot

import akka.actor.{ Actor, ActorLogging, Props }

object Device {
  def props(groupId: String, deviceId: String): Props = Props(new Device(groupId, deviceId))

  final case class RecordTemperature(requestId: Long, value: Double)
  final case class TemperatureRecorded(requestId: Long)

  final case class ReadTemperature(requestId: Long)
  final case class RespondTemperature(requestId: Long, value: Option[Double])
}

class Device(groupId: String, deviceId: String) extends Actor with ActorLogging {

  var lastTemperatureReading: Option[Double] = None

  override def preStart(): Unit = log.info("Device actor {}-{} started", groupId, deviceId)
  override def postStop(): Unit = log.info("Device actor {}-{} stopped", groupId, deviceId)

  override def receive: Receive = {
    case DeviceManager.RequestTrackDevice(`groupId`, `deviceId`) ⇒
      sender() ! DeviceManager.DeviceRegistered

    case DeviceManager.RequestTrackDevice(grpId, dvcId) ⇒
      log.warning(
        "Ignoring TrackDevice request for {}-{}. This actor is responsible for {}-{}.",
        grpId, dvcId, groupId, deviceId
      )

    case Device.RecordTemperature(id, value) ⇒
      log.info("Recorded temperature reading {} with {}", value, id)
      lastTemperatureReading = Some(value)
      sender() ! Device.TemperatureRecorded(id)

    case Device.ReadTemperature(id) ⇒
      sender() ! Device.RespondTemperature(id, lastTemperatureReading)
  }

}
