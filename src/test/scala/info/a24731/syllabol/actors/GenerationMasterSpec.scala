package info.a24731.syllabol.actors

import info.a24731.syllabol.actors.ApiGateway.ApiResponses.{GetJobStatusResponse, JobStatus}
import info.a24731.syllabol.domain.{Grammar, JobId}
import info.a24731.syllabol.generation.WordGenerator
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.concurrent.Eventually
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Promise
import scala.concurrent.duration.*
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class GenerationMasterSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Eventually:

  private val successfulGenerator: WordGenerator = (expr: String, count: Int) =>
    Success(List.fill(count)("generatedWord"))

  private val failingGenerator: WordGenerator = (expr: String, count: Int) =>
    Failure(new RuntimeException("Regex parsing failed"))

  private class ControlledGenerator extends WordGenerator:
    private val promise = Promise[List[String]]()
    def complete(): Unit = promise.success(List("word"))
    override def generate(expression: String, count: Int): Try[List[String]] =
      Try(scala.concurrent.Await.result(promise.future, 5 seconds))

  "GenerationMaster FSM" should {
    "start in Running state with 0 completed words" in {
      val jobId = JobId.generate()
      val grammar = Grammar("syllable", "word")
      val controlledGen = new ControlledGenerator()

      val master = spawn(GenerationMaster(jobId, "dwarven", grammar, amount = 100, successfulGenerator))
      val probe = createTestProbe[GetJobStatusResponse]()

      master ! GenerationMaster.GetStatus(probe.ref)
      probe.expectMessage(JobStatus(jobId, state = "Running", completed = 0, total = 100))
      controlledGen.complete()
    }

    "process batches via worker pool until reach Completed state" in {
      val jobId = JobId.generate()
      val grammar = Grammar("ka|ko", "(ka|ko){2}")

      val master = spawn(
        GenerationMaster(jobId, "dwarven", grammar, amount = 250, successfulGenerator)
      )
      val probe = createTestProbe[GetJobStatusResponse]()

      eventually {
        master ! GenerationMaster.GetStatus(probe.ref)
        val status = probe.expectMessageType[JobStatus]
        status.state shouldBe "Completed"
        status.completed shouldBe 250
        status.total shouldBe 250
      }
    }

    "transition to Failed state if generator fails" in {
      val jobId = JobId.generate()
      val grammar = Grammar("invalid", "[")
      val master = spawn(
        GenerationMaster(jobId, "orcish", grammar, amount = 100, failingGenerator)
      )
      val probe = createTestProbe[GetJobStatusResponse]()

      eventually {
        master ! GenerationMaster.GetStatus(probe.ref)
        val status = probe.expectMessageType[JobStatus]
        status.state should startWith("Failed:")
      }
    }

  }
