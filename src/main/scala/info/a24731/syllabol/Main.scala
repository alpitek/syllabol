package info.a24731.syllabol

import info.a24731.syllabol.actors.MainGuardian
import org.apache.pekko.actor.typed.ActorSystem

object Main:

  def main(args: Array[String]): Unit = {
    val system: ActorSystem[MainGuardian.Command] = ActorSystem[MainGuardian.Command](MainGuardian(), "MainGuardian-actor-system")

    system.whenTerminated.foreach { _ =>
      system.log.info("actor system terminated")
    }(system.executionContext)

    system.log.info("system log")
  }
