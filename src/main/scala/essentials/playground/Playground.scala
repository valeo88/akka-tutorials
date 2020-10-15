package essentials.playground

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object Playground extends App {

  val system = ActorSystem("Playground")

  val playgroundActor = system.actorOf(Props[MyPlaygroundActor], "playgroundActor")
  playgroundActor ! "I love Akka!"

  // your code here
}

class MyPlaygroundActor extends Actor with ActorLogging {
  override def receive = {
    case m => log.info(m.toString)
  }
}
