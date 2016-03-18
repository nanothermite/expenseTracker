package common

import com.google.inject.Singleton
import play.api
import play.api.Play.current
import shade.memcached.{Configuration, Memcached}

import scala.concurrent.ExecutionContext.Implicits.{global => ec}
import scala.util.Random

/**
 * Created by hkatz on 3/16/16.
 */
@Singleton
class Shared(environment: api.Environment, configuration: play.api.Configuration) {
}

object Shared {
  val r = Random
  val defaultHP = "127.0.0.1:11211"
  val hostport = api.Play.application.configuration.getString("memcached.host").getOrElse(defaultHP)
  lazy val memd = genMemHandle
  def genMemHandle = Some(Memcached(Configuration(hostport), ec))
}