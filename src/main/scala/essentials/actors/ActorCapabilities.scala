package essentials.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorCapabilities extends App {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case "Hi" => context.sender() ! "Hello there" // reply to message from sender
      case message: String => println(s"[${self.path}] I have received $message")
      case number: Number => println(s"[simple actor] I have received NUMBER: $number")
      case SampleMessage(content) => println(s"[simple actor] I have received some special: $content")
      case SendMessageToYourself(content) => self ! content // will send message to another case
      case SayHiTo(ref) => ref ! "Hi"
      case WirelessMessage(content, ref) => ref forward (content + "s") // keep the original sender
    }
  }

  val system = ActorSystem("actorCapabilitiesDemo")
  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  simpleActor ! "Hello actor"

  // 1 - messages can be of ANY type
  // like number, object ...
  // a) messages must be IMMUTABLE
  // b) messages must be SERIALIZABLE
  // In practice better to use case classes and case objects!
  simpleActor ! 233.3

  case class SampleMessage(content: String)
  simpleActor ! SampleMessage("some secret content")

  // 2 - actors have information about their context and themselves
  // context.self === this in OOP

  // can send messaged to yourself
  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("redirect to me")

  // 3 - actors can REPLY to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // 4 - dead letters
  // if message can't be delivered => putted into dead letters
  alice ! "Hi"

  // 5 - forwarding messagges
  // D -> A -> B
  // forwarding = sending a message with ORIGINAL sender
  case class WirelessMessage(content: String, ref: ActorRef)
  alice ! WirelessMessage("Hi", bob)
}
