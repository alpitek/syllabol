package info.a24731.syllabol.actors

import info.a24731.syllabol.actors.ApiGateway.ApiResponses.{GetJobStatusResponse, JobStatus}
import info.a24731.syllabol.domain.{Grammar, JobId}
import info.a24731.syllabol.generation.WordGenerator
import info.a24731.syllabol.persistence.PersistenceSerialization
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.actor.typed.scaladsl.{Behaviors, Routers}
import org.apache.pekko.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import org.apache.pekko.persistence.typed.{PersistenceId, RecoveryCompleted}

object PersistentGenerationMaster:

  sealed trait Command                                                extends PersistenceSerialization
  final case class GetStatus(replyTo: ActorRef[GetJobStatusResponse]) extends Command
  final case class WorkerResponse(response: GeneratorWorker.Response) extends Command

  sealed trait Event                                               extends PersistenceSerialization
  final case class BatchProcessed(count: Int, words: List[String]) extends Event
  final case class JobFailedEvent(reason: String)                  extends Event

  final case class State(
      completedAmount: Int = 0,
      totalAmount: Int = 0,
      words: List[String] = Nil,
      isCompleted: Boolean = false,
      failureReason: Option[String] = None
  ) extends PersistenceSerialization:
    def statusString: String =
      if isCompleted then "Completed"
      else failureReason.map(r => s"Failed: $r").getOrElse("Running")

  private val batchSize = 100

  def apply(
      jobId: JobId,
      languageId: String,
      grammar: Grammar,
      amount: Int,
      generator: WordGenerator
  ): Behavior[Command] = Behaviors.setup { context =>
    val workerReplyTo: ActorRef[GeneratorWorker.Response] =
      context.messageAdapter(resp => WorkerResponse(resp))

    val pool = Routers.pool(poolSize = 4) {
      Behaviors
        .supervise(GeneratorWorker(generator))
        .onFailure[Exception](SupervisorStrategy.restart)
    }
    val router = context.spawn(pool, s"worker-pool-$jobId")

    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId("GenerationMaster", jobId.toString),
      emptyState = State(completedAmount = 0, totalAmount = amount),
      commandHandler = (state, command) => handleCommand(jobId, grammar, router, workerReplyTo, state, command),
      eventHandler = (state, event) => handleEvent(state, event)
    ).receiveSignal { case (state, RecoveryCompleted) =>
      context.log.info(s"Recovered job $jobId state: ${state.completedAmount}/${state.totalAmount}")

      if !state.isCompleted && state.failureReason.isEmpty then
        val remaining = state.totalAmount - state.completedAmount
        val nextBatch = math.min(batchSize, remaining)
        router ! GeneratorWorker.GenerateBatch(jobId, grammar.wordPattern, nextBatch, workerReplyTo)
    }
  }

  private def handleCommand(
      jobId: JobId,
      grammar: Grammar,
      router: ActorRef[GeneratorWorker.Command],
      workerReplyTo: ActorRef[GeneratorWorker.Response],
      state: State,
      command: Command
  ): Effect[Event, State] =
    command match
      case GetStatus(replyTo) =>
        replyTo ! JobStatus(jobId, state.statusString, state.completedAmount, state.totalAmount)
        Effect.none
      case WorkerResponse(GeneratorWorker.BatchGenerated(_, words)) =>
        Effect
          .persist(BatchProcessed(words.size, words))
          .thenRun { newState =>
            if !newState.isCompleted then
              val remaining = newState.totalAmount - newState.completedAmount
              val nextBatch = math.min(batchSize, remaining)
              router ! GeneratorWorker.GenerateBatch(jobId, grammar.wordPattern, nextBatch, workerReplyTo)
          }
      case WorkerResponse(GeneratorWorker.BatchFailed(_, reason)) =>
        Effect.persist(JobFailedEvent(reason))

  private def handleEvent(state: State, event: Event): State =
    event match {
      case BatchProcessed(count, words) =>
        val newCompleted = state.completedAmount + count
        state.copy(
          completedAmount = newCompleted,
          words = state.words ++ words,
          isCompleted = newCompleted >= state.totalAmount
        )
      case JobFailedEvent(reason) => state.copy(failureReason = Some(reason))
    }
