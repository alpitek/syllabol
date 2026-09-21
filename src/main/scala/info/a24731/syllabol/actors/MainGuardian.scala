package info.a24731.syllabol.actors

import info.a24731.syllabol.actors.ApiGateway
import info.a24731.syllabol.actors.ApiGateway.ApiResponses.*
import info.a24731.syllabol.domain.{Grammar, JobId}
import info.a24731.syllabol.generation.GenerexWordGenerator
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.*
import scala.language.postfixOps

object MainGuardian:
  sealed trait Command
  private final case class WrappedStartResponse(resp: StartJobResponse)      extends Command
  private final case class WrappedStatusResponse(resp: GetJobStatusResponse) extends Command
  private case object CheckStatus                                            extends Command

  private val period       = 500
  private val grammarWidth = 35000000

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timers =>
      context.log.info("Initializing Syllabol system...")

      val generator = new GenerexWordGenerator()
      val gateway   = context.spawn(ApiGateway(generator), "api-gateway")

      val startAdapter  = context.messageAdapter[StartJobResponse](WrappedStartResponse.apply)
      val statusAdapter = context.messageAdapter[GetJobStatusResponse](WrappedStatusResponse.apply)

      val testGrammar = Grammar(
        syllablePattern = "ka|ko|ku|ra|ri|ru|th|gr",
        wordPattern = "(ka|ko|ku|ra|ri|ru|th|gr){2,4}"
      )

      gateway ! ApiGateway.StartGeneration(
        languageId = "dwarven",
        grammar = testGrammar,
        amount = grammarWidth,
        replyTo = startAdapter
      )

      def awaitingJobId(): Behavior[Command] = Behaviors.receiveMessage {
        case WrappedStartResponse(GenerationStarted(jobId)) =>
          context.log.info(s"Job started successfully with ID: $jobId. Polling status...")
          timers.startTimerAtFixedRate("status-poller", CheckStatus, period milliseconds)
          polling(jobId)

        case _ => Behaviors.same
      }

      def polling(jobId: JobId): Behavior[Command] = Behaviors.receiveMessage {
        case CheckStatus =>
          gateway ! ApiGateway.GetJobStatus(jobId, statusAdapter)
          Behaviors.same

        case WrappedStatusResponse(JobStatus(_, state, completed, total)) =>
          context.log.info(s"Status update: [$state] Progress: $completed/$total words")

          if (state == "Completed" || state.startsWith("Failed")) then
            context.log.info("Generation finished. Stopping ActorSystem...")
            Behaviors.stopped
          else Behaviors.same

        case WrappedStatusResponse(JobNotFound(_)) =>
          context.log.warn("Job not found!")
          Behaviors.stopped

        case _ => Behaviors.same
      }

      awaitingJobId()
    }
  }
