package actors

import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions._
import akka.actor.{Actor, ActorLogging, Props}

/**
 * Created by hkatz on 3/15/16.
 */
case class InitSession(name: String, id: Int)
case class CheckSession(name: String)
case class EndSession(name: String)
case object DumpSessions

object SecurityActor {
  private val sessionMap = new ConcurrentHashMap[String, Int]()

  private[SecurityActor] def startSession(sessKey: String, id: Int) =
    sessionMap.put(sessKey, id)

  private[SecurityActor] def checkSession(sessKey: String): Int =
    if (sessionMap.containsKey(sessKey)) sessionMap.get(sessKey) else -1

  private[SecurityActor] def endSession(sessKey: String): Unit =
    if (sessionMap.containsKey(sessKey)) sessionMap.remove(sessKey)

  private[SecurityActor] def dumpMap: Map[String, Int] = {
    for {
      key <- sessionMap.keys
    } yield key -> sessionMap.get(key)
  }.toMap

  def props = Props[SecurityActor]
}

class SecurityActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case InitSession(name: String, id: Int) => SecurityActor.startSession(name, id)
    case CheckSession(name: String) => sender ! SecurityActor.checkSession(name)
    case EndSession(name: String) => SecurityActor.endSession(name)
    case DumpSessions => sender ! SecurityActor.dumpMap
  }
}
