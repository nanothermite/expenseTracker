package actors

import java.io.File
import java.util.concurrent.ConcurrentHashMap

import akka.actor.{ActorLogging, Actor, Props}
import xls.ProcessXLS

/**
 * Created by hkatz on 3/15/16.
 */
case class XLSName(uploadType: String, name: String, userid: Int, file: File)

object XLSActor {
  private val statusMap = new ConcurrentHashMap[File, Boolean]()

  private[XLSActor] def putStatusState(fileKey: File, state: Boolean) =
    statusMap.put(fileKey, state)

  def props = Props[XLSActor]
}

class XLSActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case XLSName(uploadType: String, name: String, userid: Int, file: File) =>
      XLSActor.putStatusState(file, false)
      ProcessXLS.readFile(file, userid, uploadType)
      sender() ! s"got $name now"
  }
}
