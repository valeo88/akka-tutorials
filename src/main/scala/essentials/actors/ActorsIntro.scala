package essentials.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsIntro extends App {

  // Part 1 - Actor System
  // one system per app, heavy object
  // name must contain only alpha-numberic symbols
  val actorSystem = ActorSystem("firstActorSystem")
  println(actorSystem.name)

  // Part 2 - create actors
  // actors are uniquely identified
  // messages are asynchronous
  // each actor may respond to message differently
  // actors are encapsulated

  class WordCountActor extends Actor {
    // internal data
    var totalWords = 0

    // behavior
    // Receive - alias for PartialFunction[Any, Unit]
    override def receive: Receive = {
      case message: String =>
        println(s"[word counter] received message: ${message}")
        totalWords += message.split(" ").length
      case msg => println(s"[word counter] I can't understand ${msg.toString}")
    }
  }

  // Part 3 - instantiation actor
  // To do this use Actor System, not NEW
  // name restriction for actor as for actor system
  val wordCountActor: ActorRef = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  val wordCountActor2: ActorRef = actorSystem.actorOf(Props[WordCountActor], "wordCounter2")

  // Part 4 - communicate!
  // ! - Sends a one-way asynchronous message. E.g. fire-and-forget semantics. = tell method
  // asynchronous! - actor 2 can receive message before first
  wordCountActor ! "I'm learning Akka and it's very cool!"
  wordCountActor2 ! "Another message"

  // How to define actor with constructor arguments?
  class Bot(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"[bot] Hi, my name is ${name}")
      case "bye" => println("[bot] Good night!")
      case _ => println("[bot] I dont' understand you, sorry :(")
    }
  }

  // we can call new in Props.apply() - not good!
  val bot = actorSystem.actorOf(Props(new Bot("Butty")), "bot")
  bot ! "hi"
  bot ! "weeee"

  // Best practice - declare companion object
  object Bot {
    def props(name: String) = Props(new Bot(name))
  }
  val bot2 = actorSystem.actorOf(Bot.props("Bot2"), "bot2")
  bot2 ! "bye"

}
