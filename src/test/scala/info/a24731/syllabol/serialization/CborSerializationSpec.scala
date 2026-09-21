package info.a24731.syllabol.serialization

import com.typesafe.config.ConfigFactory
import info.a24731.syllabol.actors.{GeneratorWorker, PersistentGenerationMaster}
import info.a24731.syllabol.domain.JobId
import org.apache.pekko.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, SerializationTestKit}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class CborSerializationSpec extends ScalaTestWithActorTestKit(ConfigFactory.load()) with AnyWordSpecLike with Matchers:

  private val serializationTestKit = SerializationTestKit(system)

  "CBOR Serialization" should {
    "correctly serialize and deserialize commands with polymorphic responses" in {
      val command: PersistentGenerationMaster.Command =
        PersistentGenerationMaster.WorkerResponse(
          GeneratorWorker.BatchGenerated(
            jobId = JobId.generate(),
            words = List("w1", "w2", "w3")
          )
        )

      serializationTestKit.verifySerialization(command, assertEquality = true)
    }

    "correctly serialize and deserialize events" in {
      val event: PersistentGenerationMaster.Event =
        PersistentGenerationMaster.BatchProcessed(count = 3, words = List("w1", "w2", "w3"))

      serializationTestKit.verifySerialization(event, assertEquality = true)
    }

    "correctly serialize and deserialize state" in {
      val state = PersistentGenerationMaster.State(
        completedAmount = 3,
        totalAmount = 10,
        words = List("w1", "w2", "w3"),
        isCompleted = false,
        failureReason = None
      )

      serializationTestKit.verifySerialization(state, assertEquality = true)
    }
  }
