package essentials.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}

object ActorLoggingDemo extends App {

  class SimpleActorWithExplicitLogger extends Actor {
    // #1 - explicit logging = define logger in class
    val logger: LoggingAdapter = Logging(context.system, this)

    override def receive: Receive = {
      /*
        1 - DEBUG
        2 - INFO
        3 - WARNING/WARN
        4 - ERROR
       */
      case message => logger.info(message.toString)// LOG it
    }
  }

  val system = ActorSystem("LoggingDemo")
  val actor = system.actorOf(Props[SimpleActorWithExplicitLogger])

  actor ! "Logging a simple message"

  // #2 - ActorLogging, log method from it
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Two things: {} and {}", a, b) // interpolated parameters
      case message => log.info(message.toString)
    }
  }

  val simplerActor = system.actorOf(Props[ActorWithLogging])
  simplerActor ! "Logging a simple message by extending a trait"

  simplerActor ! (42, 65)
}
