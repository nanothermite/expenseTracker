package controllers

import _root_.common.Shared
import shade.memcached.Memcached
import play.api.libs.concurrent.Promise._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by hkatz on 3/31/16.
  */
trait SeqOps {
  val myCache: Memcached = Shared.memd.get
  val minDuration:Duration = 1.milli
  val maxDuration:Duration = 2.milli

  /**
    *
    * actual work horse adding means only add key if not present
    *
    * @param key string
    * @param value string
    * @return
    */
  def addSeq(key: String, value: String): Future[Any] =
    Future.firstCompletedOf(Seq(myCache.add(key, value, minDuration), timeout("Oops", maxDuration)))

  /**
    *
    * actual work horse setting
    *
    * @param key string
    * @param value string
    * @return
    */
  def setSeq(key:String, value: String): Future[Any] =
    Future.firstCompletedOf(Seq(myCache.set(key, value, minDuration), timeout("Oops", maxDuration)))

  /**
    * actual work horse getting
    *
    * @param key string
    * @return
    */
  def getSeq(key:String): Future[Any] =
    Future.firstCompletedOf(Seq(myCache.get[String](key), timeout("Oops", minDuration)))

  /**
    * actual work horse deleting
    *
    * @param key string
    * @return
    */
  def delSeq(key:String) : Future[Any] =
    Future.firstCompletedOf(Seq(myCache.delete(key), timeout("Oops", maxDuration)))

  /**
    * actual work horse deleting
    *
    * @param key string
    * @return
    */
  def compareSetSeq(key:String, newVal:String) : Future[Any] =
    Future.firstCompletedOf(Seq(myCache.compareAndSet(key, Some(key), newVal, minDuration), timeout("Oops", maxDuration)))

}
