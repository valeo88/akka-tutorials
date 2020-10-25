package essentials.faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartingStoppingActors extends App {

  val system = ActorSystem("StoppingActorsDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Parent extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.info(s"Stopping child with the name $name")
        val childOption = children.get(name)
        /**
         * Stop the actor pointed to by the given [[akka.actor.ActorRef]]; this is
         * an asynchronous operation, i.e. involves a message send.
         * If this method is applied to the `self` reference from inside an Actor
         * then that Actor is guaranteed to not process any further messages after
         * this call; please note that the processing of the current message will
         * continue, this method does not immediately terminate this actor.
         */
        childOption.foreach(childRef => context.stop(childRef))
      case Stop =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(message.toString)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
   * method #1 - using context.stop
   */
  import Parent._

  def contextStopDemo: Unit = {
      val parent = system.actorOf(Props[Parent], "parent")
      parent ! StartChild("child1")
      val child = system.actorSelection("/user/parent/child1")
      child ! "hi kid!"

      parent ! StopChild("child1")
      //  for (_ <- 1 to 50) child ! "are you still there?"

      parent ! StartChild("child2")
      val child2 = system.actorSelection("user/parent/child2")
      child2 ! "hi, second child"

      parent ! Stop
      for (_ <- 1 to 10) parent ! "parent, are you still there?" // should not be received
      for (i <- 1 to 100) child2 ! s"[$i] second kid, are you still alive?"
  }
  //contextStopDemo


  /**
   * method #2 - using special messages
   */
  def stopBySpecialMessage: Unit = {
      val looseActor = system.actorOf(Props[Child])
      looseActor ! "hello, loose actor"
      looseActor ! PoisonPill
      looseActor ! "loose actor, are you still there?"

      val abruptlyTerminatedActor = system.actorOf(Props[Child])
      abruptlyTerminatedActor ! "you are about to be terminated"
      abruptlyTerminatedActor ! Kill
      abruptlyTerminatedActor ! "you have been terminated"
  }
  //stopBySpecialMessage


  /**
   *  Death watch
   */
  class Watcher extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
      /**
       * Registers this actor as a Monitor for the provided ActorRef.
       * This actor will receive a Terminated(subject) message when watched
       * actor is terminated. */
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"the reference that I'm watching $ref has been stopped")
    }
  }

  def deathWatchDemo: Unit = {
    val watcher = system.actorOf(Props[Watcher], "watcher")
    watcher ! StartChild("watchedChild")
    val watchedChild = system.actorSelection("/user/watcher/watchedChild")
    Thread.sleep(500)

    watchedChild ! PoisonPill
  }
  deathWatchDemo

}
