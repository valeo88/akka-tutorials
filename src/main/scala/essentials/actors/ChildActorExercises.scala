package essentials.actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorExercises extends App {

  // Distributed word counter

  /*
    create WordCounterMaster
    send Initialize(10) to wordCounterMaster
    send "Akka is awesome" to wordCounterMaster
      wcm will send a WordCountTask("...") to one of its children
        child replies with a WordCountReply(3) to the master
      master replies with 3 to the sender.
    requester -> wcm -> wcw
           r  <- wcm <-
   */
  // round robin logic
  // 1,2,3,4,5 and 7 tasks
  // 1,2,3,4,5,1,2

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    // id - identifier for task to map request-reply
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }
  class WordCounterMaster extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChildren) =>
        println("[master] initializing...")
        // sequence of actorRef size=nChildren
        val childrenRefs = for (i <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker], s"wcw_$i")
        context.become(withChildren(childrenRefs, 0, 0, Map()))
    }

    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        println(s"[master] I have received: $text - I will send it to child $currentChildIndex")
        val originalSender = sender()
        val task = WordCountTask(currentTaskId, text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        // define next values
        // round robin child index
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
        val newTaskId = currentTaskId + 1
        // add pair (id, sender) to requestMap to link task and sender
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childrenRefs, nextChildIndex, newTaskId, newRequestMap))
      case WordCountReply(id, count) =>
        println(s"[master] I have received a reply for task id $id with $count")
        // send reply to original sender
        val originalSender = requestMap(id)
        originalSender ! count
        // remove task from map
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - id))
    }
  }

  class WordCounterWorker extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(id, text) =>
        println(s"${self.path} I have received task $id with $text")
        // id of request MUST BE EQUAL to id of reply
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }

  // actor for testing system
  class TestActor extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        val texts = List("I love Akka", "Scala is super dope", "yes", "me too")
        texts.foreach(text => master ! text)
      case count: Int =>
        println(s"[test actor] I received a reply: $count")
    }
  }

  val system = ActorSystem("roundRobinWordCountExercise")
  val testActor = system.actorOf(Props[TestActor], "testActor")
  testActor ! "go"

}
