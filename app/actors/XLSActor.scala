package actors

import akka.actor.Actor.Receive
import akka.actor.{Actor, Props}

/**
 * Created by hkatz on 3/15/16.
 */
object XLSActor {
  def props = Props[XLSActor]

  case class XLSName(name: String)
}

class XLSActor extends Actor {
  import XLSActor._

  override def receive: Receive = {
    case XLSName(name: String) =>
      sender() ! s"got $name"
  }
}
