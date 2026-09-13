package info.a24731.syllabol

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors

import org.apache.pekko.actor.typed.scaladsl.ActorContext

object Main:

  def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
    context.log.info("actor system started")

    Behaviors.empty
  }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Main(), "default-actor-system")
    system.whenTerminated.foreach { _ =>
      system.log.info("actor system terminated")
    }(system.executionContext)

    system.log.info("system log")
    system.terminate()
  }
