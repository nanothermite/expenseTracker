package controllers

import java.nio.charset.Charset
import javax.inject.Inject

import _root_.common.{Shared, myTypes}
import actors._
import akka.actor.{ActorSystem, _}
import akka.pattern.ask
import akka.util.Timeout
import com.avaje.ebean._
import entities._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import utils.Sha256

import scala.collection.{immutable => im, mutable => mu}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

class ValidationController @Inject()(system: ActorSystem) extends Controller with myTypes with Sha256 with SeqOps {
  val secActor = system.actorOf(SecurityActor.props, "sec-actor")

  implicit val timeout: Timeout = 5.seconds
  val waitTime = 700.milli

  def invalidateUser(sessKey: String, checkid: Int) = Action {
    val endedFuture =
      (secActor ? CheckSession(sessKey)).mapTo[Int].map { uid: Int =>
        Logger.debug(s"got $uid")
        if (checkid == uid) {
          secActor ! EndSession(sessKey)
          true
        } else
          false
    }
    val ended = Await.result(endedFuture, waitTime)
    Ok(Json.obj("status" ->
      (if (ended) "loggedout" else "mismatched")))
  }

  def dumpMap = Action {
    val dumpFuture =
      (secActor ? DumpSessions).mapTo[Map[String, Int]].map { sessMap: Map[String, Int] =>
        Logger.debug(s"got ${sessMap.size} recs")
        sessMap
      }
    val sessMap: Map[String, Int] = Await.result(dumpFuture, waitTime)
    val sessJsonObj = {
      for {
        sessKey <- sessMap.keys
      } yield sessKey -> JsNumber(sessMap.get(sessKey).get)
    }.toMap
    Ok(JsObject(sessJsonObj))
  }

  val r = Shared.r

  val m = ru.runtimeMirror(getClass.getClassLoader)

  val validateUserSql = "select u.password, u.id, m.fname " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "u.username = :username"
  val validateEmailSql = "select u.password, u.id, m.fname " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "m.email = :email"
  val validateSocialSql = "select m.fname " +
    "from Member m " +
    "where m.email = :email"

  /**
    * reflection on Object
    *
    * @param name method
    * @param m    runtime
    * @param tru  reflection
    * @tparam T   internals
    * @return
    */
  def methodByReflectionO[T : TypeTag](name: String, m: ru.Mirror, tru: Type): MethodMirror = {
    val modX = tru.termSymbol.asModule
    val methodX = tru.decl(ru.TermName(name)).asMethod
    val mm = m.reflectModule(modX)
    val im = m.reflect(mm.instance)
    im.reflectMethod(methodX)
  }

  def genSql(query: String, colMap: im.Map[String, String]): RawSql = {
    val rawSqlBld = RawSqlBuilder.parse(query)
    for ((k, v) <- colMap) {
      rawSqlBld.columnMapping(k, v)
    }
    rawSqlBld.create()
  }

  /**
    * generate collection of T objects using getter
    *
    * @param getter method to invoke
    * @param query  actual
    * @param colMap for ebean
    * @param pList  parameters to query
    * @param t      for reflection
    * @tparam T     reflection on return type
    * @return
    */
  def getList[T : TypeTag](getter: String, query: String, colMap: im.Map[String, String],
                           pList: im.Map[String, AnyRef], t: T): List[T] = {
    val rawSql = genSql(query, colMap)
    val myType = getType(t)
    val obj = methodByReflectionO[T](getter, m, myType)
    obj(rawSql, Some(pList)).asInstanceOf[List[T]]
  }

  def validation(email: String): (Boolean, Option[Member]) = {
    //colMap.clear()
    val colMap = Map("m.fname" -> "fname")

    //pList.clear()
    val pList = Map("email" -> email)

    val pwdList = getList("allq", validateSocialSql, colMap, pList, Member)
    val valid =
      if (pwdList.nonEmpty) {
        val member = pwdList.head.asInstanceOf[Member]
        (true, Some(member))
      } else
        (false, None)
    valid
  }

  def validation(name: String, passwd: String): (Boolean, Option[MemberUser]) = {
    val isEmail = name.contains("@")
    //colMap.clear()
    val colMap = Map("u.password" -> "password", "m.fname" -> "fname")

    //pList.clear()
    val pList = (if (isEmail) Map("email" -> name) else Map("username" -> name))

    val pwdList = getList("allq", if (isEmail) validateEmailSql else validateUserSql, colMap, pList, MemberUser)
    val valid =
      if (pwdList.nonEmpty) {
        val membUser = pwdList.head.asInstanceOf[MemberUser]
        val pwdHash = membUser.password
        (toHexString(passwd, Charset.forName("UTF-8")) == pwdHash, Some(membUser))
      } else
        (false, None)
    valid
  }

  def ranSession = (1 to 50).map(_ => r.nextPrintableChar()).mkString("")

  def validateUser(name: String, passwd: String) = Action {
    val (valid, memberUserOpt) = validation(name, passwd)

    Ok(if (valid) {
      secActor ! InitSession(ranSession, memberUserOpt.get.id.toInt)
      Json.obj("access" -> "auth", "uid" -> memberUserOpt.get.id, "welcome" -> memberUserOpt.get.fname, "sessAuth" -> ranSession)
    } else
      Json.obj("access" -> "denied")
    )
  }
}