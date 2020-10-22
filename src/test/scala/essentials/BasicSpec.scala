package essentials

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender // sender() will be testActor
  with AnyWordSpecLike // need for BDD test suite styles
  with BeforeAndAfterAll {

  // setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import BasicSpec._

  // BDD test style naming
  "A simple actor" should {
    "send back the same message" in {
      val echoActor = system.actorOf(Props[SimpleActor])
      val message = "hello, test"
      echoActor ! message

      expectMsg(message) // akka.test.single-expect-default
    }
  }

  "A blackhole actor" should {
    "send back some message" in {
      val blackhole = system.actorOf(Props[Blackhole])
      val message = "hello, test"
      blackhole ! message

      expectNoMessage(1.second) // no message in duration
    }
  }

  // message assertions
  "A lab test actor" should {
    val labTestActor = system.actorOf(Props[LabTestActor])

    "turn a string into uppercase" in {
      labTestActor ! "I love Akka"
      val reply = expectMsgType[String] // extract reply message with String type

      assert(reply == "I LOVE AKKA")
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi", "hello") // hi or hello expected
    }

    "reply with favorite tech" in {
      labTestActor ! "favoriteTech"
      expectMsgAllOf("Scala", "Akka") // Scala and Akka expected
    }

    "reply with cool tech in a different way" in {
      labTestActor ! "favoriteTech"
      val messages = receiveN(2) // Seq[Any]

      // free to do more complicated assertions
    }

    "reply with cool tech in a fancy way" in {
      labTestActor ! "favoriteTech"

      // if has messages which not defined in PF = MatctError
      expectMsgPF() {
        case "Scala" => // only care that the PF is defined
        case "Akka" =>
      }
    }

  }


}

/** It's recommended to use companion object for actor definitions, constants, test values and so on... */
object BasicSpec {

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class Blackhole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()

    override def receive: Receive = {
      case "greeting" =>
        if (random.nextBoolean()) sender() ! "hi" else sender() ! "hello"
      case "favoriteTech" =>
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender() ! message.toUpperCase()
    }
  }

}
