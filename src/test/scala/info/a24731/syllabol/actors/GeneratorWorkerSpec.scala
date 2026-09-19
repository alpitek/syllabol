package info.a24731.syllabol.actors

import info.a24731.syllabol.domain.JobId
import info.a24731.syllabol.generation.WordGenerator
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.{Failure, Success, Try}

class GeneratorWorkerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike:
  import GeneratorWorker.*

  "GeneratorWorker" should {
    "return BatchGenerated with generated words on success" in {
      val successGenerator = new WordGenerator:
        override def generate(expr: String, count: Int): Try[List[String]] =
          Success(List.fill(count)("test"))

      val worker = spawn(GeneratorWorker(successGenerator))
      val probe = createTestProbe[Response]()
      val jobId = JobId.generate()

      worker ! GenerateBatch(jobId, "[a-z]+", 3, probe.ref)
      probe.expectMessage(BatchGenerated(jobId, List("test", "test", "test")))
    }

    "return BatchFailed on generator exception" in {
      val failingGenerator = new WordGenerator:
        override def generate(expr: String, count: Int): Try[List[String]] =
          Failure(new IllegalArgumentException("Unclosed character class"))

      val worker = spawn(GeneratorWorker(failingGenerator))
      val probe = createTestProbe[Response]()
      val jobId = JobId.generate()

      worker ! GenerateBatch(jobId, "[a-z+", 3, probe.ref)
      probe.expectMessage(BatchFailed(jobId, "Unclosed character class"))
    }
  }
