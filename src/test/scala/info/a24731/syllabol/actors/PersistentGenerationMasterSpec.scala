package info.a24731.syllabol.actors

import info.a24731.syllabol.domain.{Grammar, JobId}
import info.a24731.syllabol.generation.WordGenerator
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import org.apache.pekko.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit.SerializationSettings
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Promise
import scala.concurrent.duration.*
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

import com.typesafe.config.ConfigFactory

private val testConfig = EventSourcedBehaviorTestKit.config
  .withFallback(ConfigFactory.load())

class PersistentGenerationMasterSpec
//  extends ScalaTestWithActorTestKit(EventSourcedBehaviorTestKit.config)
    extends ScalaTestWithActorTestKit(testConfig)
    with AnyWordSpecLike
    with Matchers:

  private val stubGenerator: WordGenerator = (expr: String, count: Int) => Success(List.fill(count)("testWord"))

  private val hangingGenerator: WordGenerator = (_, _) => Try(scala.concurrent.Await.result(Promise[List[String]]().future, 1 minute))

  private val noOpGenerator: WordGenerator = (_, _) => Failure(new RuntimeException("Test pause generator"))

  private val eventSourcedTestKit = EventSourcedBehaviorTestKit[
    PersistentGenerationMaster.Command,
    PersistentGenerationMaster.Event,
    PersistentGenerationMaster.State
  ](
    system,
    PersistentGenerationMaster(
      jobId = JobId.generate(),
      languageId = "elvish",
      grammar = Grammar("syllable", "wordPattern"),
      amount = 200,
      generator = noOpGenerator
    ),
    SerializationSettings.enabled
//    SerializationSettings.disabled
//    SerializationSettings.enabled.withVerifyCommands(false)
  )

  "PersistentGenerationMaster" should {

    "persist BatchProcessed events and maintain correct state" in {
      eventSourcedTestKit.clear()

      val result = eventSourcedTestKit.runCommand(
        PersistentGenerationMaster.WorkerResponse(
          GeneratorWorker.BatchGenerated(JobId.generate(), List("w1", "w2", "w3"))
        )
      )

      result.event shouldBe a[PersistentGenerationMaster.BatchProcessed]
      result.state.completedAmount shouldBe 3
      result.state.words should contain theSameElementsAs List("w1", "w2", "w3")
    }

    "recover state properly after actor restart" in {
      eventSourcedTestKit.clear()

      eventSourcedTestKit.runCommand(
        PersistentGenerationMaster.WorkerResponse(
          GeneratorWorker.BatchGenerated(JobId.generate(), List("word1", "word2"))
        )
      )

      eventSourcedTestKit.restart()

      eventSourcedTestKit.getState().completedAmount shouldBe 2
      eventSourcedTestKit.getState().words should contain theSameElementsAs List("word1", "word2")
    }
  }
