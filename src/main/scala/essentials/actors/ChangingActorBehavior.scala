package essentials.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehavior extends App {

  object FussyKid {
    case object KidAccept
    case object KidReject

    val HAPPY = "happy"
    val SAD = "sad"
  }
  class FussyKid extends Actor {
    import FussyKid._
    import Mom._
    // internal state of kid
    var state = HAPPY
    override def receive: Receive = {
      case Food(VEGETABLES) => state = SAD
      case Food(CHOCHOLATE) => state = HAPPY
      case Ask(_) =>
        // PROBLEM: in real app this can have a lot of variants!
        if (state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  // can create actor without state!
  class StatelessFussyKid extends Actor {
    import FussyKid._
    import Mom._

    override def receive: Receive = happyReceive // by default

    /**
     * Changes the Actor's behavior to become the new 'Receive' (PartialFunction[Any, Unit]) handler.
     * This method acts upon the behavior stack as follows:
     *
     *  - if `discardOld = true` it will replace the top element (i.e. the current behavior)
     *  - if `discardOld = false` it will keep the current behavior and push the given one atop
     *  The default of replacing the current behavior on the stack has been chosen to avoid memory
     *  leaks in case client code is written without consulting this documentation first (i.e.
     *  always pushing new behaviors and never issuing an `unbecome()`)
    */
    def happyReceive: Receive = {
      case Food(VEGETABLES) => context.become(sadReceive, false) // push sadReceive to stack
      case Food(CHOCHOLATE) => // stay happy
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLES) => context.become(sadReceive, false) // push sadReceive to stack
      case Food(CHOCHOLATE) => context.unbecome() // pop sadReceive from stack
      case Ask(_) => sender() ! KidReject
    }
  }

  import Mom._
  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(question: String) // like Do you want to play?

    val VEGETABLES = "veggies"
    val CHOCHOLATE = "choco"
  }
  class Mom extends Actor {
    import Mom._
    import FussyKid._

    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test interaction with kid
        kidRef ! Food(VEGETABLES)
        kidRef ! Food(VEGETABLES)
        kidRef ! Food(CHOCHOLATE)
        kidRef ! Food(CHOCHOLATE)
        kidRef ! Ask("do you want to play?")
      case KidAccept => println("My kid is happy!")
      case KidReject => println("My kid is sad, but healthy!")
    }
  }

  val system = ActorSystem("changingActorBehavior")

  val fussyKid = system.actorOf(Props[FussyKid], "kid")
  val statelessFussyKid = system.actorOf(Props[StatelessFussyKid], "kid2")
  val mom = system.actorOf(Props[Mom], "mom")

  mom ! MomStart(statelessFussyKid)

  // Exercise #1: Counter with context.become and no MUTABLE STATE
  object Counter {
    case class Increment(amount: Int = 1)
    case class Decrement(amount: Int = 1)
    case object Print
  }
  import Counter._

  class Counter extends Actor {
    // save state as function param in call stack
    override def receive: Receive = countReceive(0)

    def countReceive(currentCount: Int): Receive = {
      case Increment(amount) =>
        println(s"[countReceive($currentCount)] incrementing")
        context.become(countReceive(currentCount + amount))
      case Decrement(amount) =>
        println(s"[countReceive($currentCount)] decrementing")
        context.become(countReceive(currentCount - amount))
      case Print => println(s"[countReceive($currentCount)] my current count is $currentCount")
    }
  }

  val counter = system.actorOf(Props[Counter], "counter")
  counter ! Increment(10)
  counter ! Decrement(4)
  counter ! Increment()
  counter ! Print
}
