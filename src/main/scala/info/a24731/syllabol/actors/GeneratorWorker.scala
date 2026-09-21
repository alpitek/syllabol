package info.a24731.syllabol.actors

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import info.a24731.syllabol.domain.JobId
import info.a24731.syllabol.generation.WordGenerator
import info.a24731.syllabol.persistence.PersistenceSerialization
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.util.{Failure, Success}

object GeneratorWorker:

  sealed trait Command
  final case class GenerateBatch(
      jobId: JobId,
      expression: String,
      count: Int,
      replyTo: ActorRef[Response]
  ) extends Command

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes(
    Array(
      new JsonSubTypes.Type(value = classOf[BatchGenerated], name = "BatchGenerated"),
      new JsonSubTypes.Type(value = classOf[BatchFailed], name = "BatchFailed")
    )
  )
  sealed trait Response                                              extends PersistenceSerialization
  final case class BatchGenerated(jobId: JobId, words: List[String]) extends Response
  final case class BatchFailed(jobId: JobId, reason: String)         extends Response

  def apply(generator: WordGenerator): Behavior[Command] =
    Behaviors.receiveMessage { case GenerateBatch(jobId, expression, count, replyTo) =>
      generator.generate(expression, count) match
        case Success(words) =>
          replyTo ! BatchGenerated(jobId, words)
        case Failure(exception) =>
          replyTo ! BatchFailed(jobId, exception.getMessage)

      Behaviors.same
    }
