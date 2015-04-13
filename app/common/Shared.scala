package common

import java.util.concurrent.ExecutorService

import play.api.Play
import play.api.Play.current
import shade.memcached.{Configuration, Memcached}
import scala.concurrent.ExecutionContext.Implicits.{global => ec}

/**
 * Created by hkatz on 4/8/15.
 */
object Shared {
  val defaultHP = "127.0.0.1:11211"
  val hostport = Play.application.configuration.getString("memcached.host").getOrElse(defaultHP)
  var memd = None:Option[Memcached]

  def genMemHandle() = {
    memd = Option(Memcached(Configuration(hostport), ec))
  }
}
