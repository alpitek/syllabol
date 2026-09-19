package info.a24731.syllabol.actors

import info.a24731.syllabol.actors.ApiGateway.ApiResponses.{GenerationStarted, GetJobStatusResponse, JobNotFound, StartJobResponse}
import info.a24731.syllabol.actors.GenerationMaster.GetStatus
import info.a24731.syllabol.domain.{Grammar, JobId}
import info.a24731.syllabol.generation.WordGenerator
import org.apache.pekko.actor.typed.{ActorRef, Behavior}
import org.apache.pekko.actor.typed.scaladsl.Behaviors

object ApiGateway:
  sealed trait ApiCommand
  final case class StartGeneration(
    languageId: String,
    grammar: Grammar,
    amount: Int,
    replyTo: ActorRef[StartJobResponse]
  ) extends ApiCommand
  final case class GetJobStatus(
    jobId: JobId,
    replyTo: ActorRef[GetJobStatusResponse]
  ) extends ApiCommand
  final case class MasterTerminated(jobId: JobId) extends ApiCommand

  object ApiResponses:
    sealed trait ApiResponse
    sealed trait StartJobResponse extends ApiResponse
    case class GenerationStarted(jobId: JobId) extends StartJobResponse
    sealed trait GetJobStatusResponse extends ApiResponse
    case class JobStatus(jobId: JobId, state: String, completed: Int, total: Int) extends GetJobStatusResponse
    case class JobNotFound(jobId: JobId) extends GetJobStatusResponse

  def apply(generator: WordGenerator): Behavior[ApiCommand] = Behaviors.setup { context =>
    context.log.info("ApiGateway started")

    def running(activeMasters: Map[JobId, ActorRef[GenerationMaster.Command]]): Behavior[ApiCommand] =
      Behaviors.receiveMessage {
        case StartGeneration(langId, grammar, amount, replyTo) =>
          
          val jobId = JobId.generate()
          context.log.info(s"Creating new generation job $jobId for language $langId")

          val masterRef = context.spawn(
            GenerationMaster(jobId, langId, grammar, amount, generator),
            name = s"master-$jobId"
          )

          context.watchWith(masterRef, MasterTerminated(jobId))

          replyTo ! GenerationStarted(jobId)

          running(activeMasters + (jobId -> masterRef))

        case GetJobStatus(jobId, replyTo) =>
          activeMasters.get(jobId) match {
            case Some(masterRef) =>
              masterRef ! GetStatus(replyTo)
            case None => 
              replyTo ! JobNotFound(jobId)
          }

          Behaviors.same

        case MasterTerminated(jobId) =>
          context.log.info(s"Job $jobId terminated, cleaning up")
          running(activeMasters - jobId)
      }

    running(Map.empty)
  }

