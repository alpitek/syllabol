package info.a24731.syllabol.actors

import info.a24731.syllabol.actors.ApiGateway.ApiResponses.{GetJobStatusResponse, JobStatus}
import info.a24731.syllabol.domain.{Grammar, JobId}
import info.a24731.syllabol.generation.WordGenerator
import org.apache.pekko.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import org.apache.pekko.actor.typed.scaladsl.{Behaviors, Routers}

object GenerationMaster:
  sealed trait Command
  final case class GetStatus(replyTo: ActorRef[GetJobStatusResponse]) extends Command

  // internal commands for state switching (FSM)
  private final case class BatchCompleted(generatedCount: Int) extends Command
  private final case class JobFailed(reason: String) extends Command
  private final case class WorkerResponse(response: GeneratorWorker.Response) extends Command

  def apply(jobId: JobId, languageId: String, grammar: Grammar, amount: Int, generator: WordGenerator): Behavior[Command] =
    Behaviors.setup { context =>
      context.log.info(s"Starting GenerationMaster for Job $jobId (Language: $languageId)")

      val workerReplyTo: ActorRef[GeneratorWorker.Response] =
        context.messageAdapter(resp => WorkerResponse(resp))

      val pool = Routers.pool(poolSize = 4) {
        Behaviors.supervise(GeneratorWorker(generator))
          .onFailure[Exception](SupervisorStrategy.restart)
      }
      val router = context.spawn(pool, s"worker-pool-$jobId")

      val batchSize = math.min(100, amount)
      router ! GeneratorWorker.GenerateBatch(jobId, grammar.wordPattern, batchSize, workerReplyTo)

      generating(jobId, amount, completedAmount = 0, batchSize, router, workerReplyTo, grammar)
    }

  private def generating(jobId: JobId,
    totalAmount: Int,
    completedAmount: Int,
    batchSize: Int,
    router: ActorRef[GeneratorWorker.Command],
    workerReplyTo: ActorRef[GeneratorWorker.Response],
    grammar: Grammar,
  ): Behavior[Command] =
    Behaviors.receive { (context, message) => message match
      case GetStatus(replyTo) =>
        replyTo ! JobStatus(jobId, state = "Running", completed = completedAmount, total = totalAmount)
        Behaviors.same

      case WorkerResponse(GeneratorWorker.BatchGenerated(_, words)) =>
        context.log.info(s"Generated ${words.size} words: ${words.take(100).mkString(", ")}${if (words.size > 100) "..." else ""}")
        val count = words.size
        val newTotal = completedAmount + count

        if (newTotal >= totalAmount) then
          completed(jobId, totalAmount)
        else
          val nextBatchSize = math.min(batchSize, totalAmount - newTotal)
          router ! GeneratorWorker.GenerateBatch(jobId, grammar.wordPattern, nextBatchSize, workerReplyTo)
          generating(jobId, totalAmount, newTotal, batchSize, router, workerReplyTo, grammar)

      case WorkerResponse(GeneratorWorker.BatchFailed(_, reason)) =>
        failed(jobId, totalAmount, completedAmount, reason)

      case BatchCompleted(count) =>
        val newTotal = completedAmount + count
        if (newTotal >= totalAmount) then completed(jobId, totalAmount)
        else generating(jobId, totalAmount, newTotal, batchSize, router, workerReplyTo, grammar)

      case JobFailed(reason) =>
        failed(jobId, totalAmount, completedAmount, reason)
    }

  private def completed(jobId: JobId, totalAmount: Int): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetStatus(replyTo) =>
        replyTo ! JobStatus(jobId, state = "Completed", completed = totalAmount, total = totalAmount)
        Behaviors.same

      case _ =>
        Behaviors.same
    }

  private def failed(jobId: JobId, totalAmount: Int, completedAmount: Int, reason: String): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetStatus(replyTo) =>
        replyTo ! JobStatus(jobId, state = s"Failed: $reason", completed = completedAmount, total = totalAmount)
        Behaviors.same

      case _ =>
        Behaviors.same
    }

  def simulateBatchCompletion(count: Int): Command = BatchCompleted(count)
  def simulateFailure(reason: String): Command = JobFailed(reason)
