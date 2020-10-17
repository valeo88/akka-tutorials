package essentials.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import essentials.actors.ActorCapabilities.Counter.{Decrement, Increment, Print}

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

  /**
   * Exercises
   *
   * 1. a Counter actor
   *   - Increment
   *   - Decrement
   *   - Print
   *
   * 2. a Bank account as an actor
   *   receives
   *   - Deposit an amount
   *   - Withdraw an amount
   *   - Statement
   *   replies with
   *   - Success
   *   - Failure
   *
   *   interact with some other kind of actor
   */

  // Counter
  // Using DOMAINS
  object Counter {
    case class Increment(amount: Int = 1)
    case class Decrement(amount: Int = 1)
    case object Print
  }

  class Counter extends Actor {
    var counter = 0

    override def receive: Receive = {
      case Increment(amount) => counter += amount
      case Decrement(amount) => counter -= amount
      case Print => println(s"[${self.path}] has value: ${counter}")
    }
  }

  val counter = system.actorOf(Props[Counter], "counter")
  counter ! Increment(10)
  counter ! Decrement(4)
  counter ! Increment
  counter ! Print

  // Bank account
  // DOMAIN
  object BankAccount {
    trait BankAccountOperation
    case class Deposit(amount: Int) extends BankAccountOperation
    case class Withdraw(amount: Int) extends BankAccountOperation
    case object Statement extends BankAccountOperation

    case class TransactionSuccess(message: String)
    case class TransactionFailure(message: String)
  }
  import BankAccount._

  class BankAccount extends Actor {
    var balance = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0) sender() ! TransactionFailure("amount can't be negative")
        else {
          balance += amount
          sender() ! TransactionSuccess(s"Deposit ${amount} on balance, result=${balance}")
        }
      case Withdraw(amount) =>
        if (amount < 0) sender() ! TransactionFailure("amount can't be negative")
        else if (amount > balance) sender() ! TransactionFailure("Insufficient money on balance")
        else {
          balance -= amount
          sender() ! TransactionSuccess(s"Withdraw ${amount} from balance, result=${balance}")
        }
      case Statement => sender() ! TransactionSuccess(s"Current balance is ${balance}")
    }

  }

  // Person to interact with bank account
  object BankStuff {
    case class CommandToBankAccount(operation: BankAccountOperation, bankAccount: ActorRef)
  }
  import BankStuff._

  class BankStuff extends Actor {
    override def receive: Receive = {
      case TransactionSuccess(message) => println(s"[${sender()}] replied with success: ${message}")
      case TransactionFailure(message) => println(s"[${sender()}] replied with failure: ${message}")
      case CommandToBankAccount(operation, ref) => ref ! operation
    }
  }

  // test bank account
  val bankAccount = system.actorOf(Props[BankAccount], "bankAccount")
  val bankStuff = system.actorOf(Props[BankStuff], "bankStuff")

  // messages send in this order -> queue
  bankStuff ! CommandToBankAccount(Deposit(1000), bankAccount)
  bankStuff ! CommandToBankAccount(Withdraw(2000), bankAccount)
  bankStuff ! CommandToBankAccount(Statement, bankAccount)

}
