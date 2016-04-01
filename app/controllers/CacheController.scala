package controllers

import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global

class CacheController extends Controller with SeqOps {
  /**
    * set cache key to value asynchronously
    *
    * @param key string
    * @param value string
    * @return
    */
  def memadd(key: String, value: String) = Action.async {
    addSeq(key, value).map {
      case i: Boolean   => Ok(if (i) "ok" else "exists")
      case t: AnyRef    => Ok("unset")
    }
  }

  /**
    * set cache key to value asynchronously
    *
    * @param key string
    * @param value string
    * @return
    */
  def memset(key: String, value: String) = Action.async {
    setSeq(key, value).map {
      case i: Unit => Ok("ok")
      case t: AnyRef => Ok("unset")
    }
  }

  /**
    * blocking version
    *
    * @param key string
    * @return
    */
  def memgetBlock(key: String) = Action {
    val res = myCache.awaitGet[String](key) match {
      case Some(value) => value
      case None        => s"key $key not found"
    }
    Ok(res)
  }

  /**
    * asynch get key for minDuration
    *
    * @param key string
    * @return
    */
  def memget(key: String) = Action.async {
    getSeq(key).map {
      case Some(i: String) => Ok(if (i != null) i else "nf")
      case t: AnyRef => Ok("broke")
    }
  }

  /**
    * async drop key for minDuration
    *
    * @param key string
    * @return
    */
  def memdrop(key: String) = Action.async {
    delSeq(key).map {
      case i: Boolean => Ok(if (i) "hit" else "miss")
      case t: AnyRef => Ok("broke")
    }
  }
}
