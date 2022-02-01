package example

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import org.scalatest._
import payments.Configuration
import payments.Configuration._

class ConfigurationSpec extends ScalaTestWithActorTestKit with WordSpecLike {

  "The Configuration actor" should {

    "not find a configuration for an unknown merchant" in {
      // define a probe which allows it to easily send messages
      val probe: TestProbe[ConfigurationResponse] = createTestProbe[ConfigurationResponse]()

      // spawn a new Configuration actor as child of the TestKit's guardian actor
      val configurationActor: ActorRef[ConfigurationMessage] = spawn(Configuration())

      // send a message to the actor under test with the probe's reference as sender
      configurationActor ! Configuration.RetrieveConfiguration(MerchantId("unknown"), probe.ref)

      // expect a certain type of message as response. there are many different ways to retrieve
      // or to expect messages
      val response: ConfigurationNotFound = probe.expectMessageType[ConfigurationNotFound]
      response.merchanId shouldBe MerchantId("unknown")
    }

  }

}
