package essentials.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}

object IntroAkkaConfig extends App {

  class SimpleLoggingActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
   * 1 - inline configuration
   */
  val configString =
    """
      | akka {
      |   loglevel = "ERROR"
      | }
    """.stripMargin

  val config: Config = ConfigFactory.parseString(configString)
  val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))
  val actor = system.actorOf(Props[SimpleLoggingActor])

  actor ! "A message to remember"

  /**
   * 2 - config file (by default = src/main/resources/application.conf)
   */

  val defaultConfigFileSystem = ActorSystem("DefaultConfigFileDemo")
  val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])
  defaultConfigActor ! "Remember me"

  /**
   * 3 - separate config in the same file (block mySpecialConfig in src/main/resources/application.conf)
   */
  val specialConfig: Config = ConfigFactory.load().getConfig("mySpecialConfig")
  val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
  val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])
  specialConfigActor ! "Remember me, I am special"

  /**
   * 4 - separate config in another file (src/main/resources/secretFolder/secretConfiguration.conf)
   */

  val separateConfig: Config = ConfigFactory.load("secretFolder/secretConfiguration.conf")
  println(s"separate config log level: ${separateConfig.getString("akka.loglevel")}")

  /**
   * 5 - different file formats
   * JSON, Properties
   */
  val jsonConfig: Config = ConfigFactory.load("json/jsonConfig.json")
  println(s"json config: ${jsonConfig.getString("aJsonProperty")}")
  println(s"json config: ${jsonConfig.getString("akka.loglevel")}")

  val propsConfig: Config = ConfigFactory.load("props/propsConfiguration.properties")
  println(s"properties config: ${propsConfig.getString("my.simpleProperty")}")
  println(s"properties config: ${propsConfig.getString("akka.loglevel")}")

}
