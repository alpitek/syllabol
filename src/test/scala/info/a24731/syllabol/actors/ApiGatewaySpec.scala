package info.a24731.syllabol.actors

import info.a24731.syllabol.actors.ApiGateway.ApiResponses.*
import info.a24731.syllabol.domain.{Grammar, JobId}
import info.a24731.syllabol.generation.WordGenerator
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.Success

class ApiGatewaySpec extends ScalaTestWithActorTestKit with AnyWordSpecLike:

  private val stubGenerator: WordGenerator = (expr: String, count: Int) =>
      Success(List.fill(count)("stubWord"))

  "ApiGateway" should {

    "start a generation job and return GenerationStarted" in {
      val gateway = spawn(ApiGateway(stubGenerator))
      val probe = createTestProbe[StartJobResponse]()
      val grammar = Grammar("syllable", "word")

      gateway ! ApiGateway.StartGeneration("dwarven", grammar, amount = 50, probe.ref)
      val response = probe.expectMessageType[GenerationStarted]

      response.jobId.toString should not be empty
    }

    "route GetJobStatus request to the corresponding active master" in {
      val gateway = spawn(ApiGateway(stubGenerator))
      val startProbe = createTestProbe[StartJobResponse]()
      val statusProbe = createTestProbe[GetJobStatusResponse]()
      val grammar = Grammar("syllable", "word")

      gateway ! ApiGateway.StartGeneration("elvish", grammar, amount = 100, startProbe.ref)
      val started = startProbe.expectMessageType[GenerationStarted]

      gateway ! ApiGateway.GetJobStatus(started.jobId, statusProbe.ref)
      val status = statusProbe.expectMessageType[JobStatus]

      status.jobId shouldBe started.jobId
      status.total shouldBe 100
    }

    "return JobNotFound for non-existing jobId" in {
      val gateway = spawn(ApiGateway(stubGenerator))
      val probe = createTestProbe[GetJobStatusResponse]()
      val unknownJobId = JobId.generate()

      gateway ! ApiGateway.GetJobStatus(unknownJobId, probe.ref)
      probe.expectMessage(JobNotFound(unknownJobId))
    }
  }
