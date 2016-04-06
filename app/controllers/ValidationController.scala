package controllers

import java.nio.charset.Charset
import java.util.Date
import javax.inject.Inject

import _root_.common.{BaseObject, Shared, myTypes}
import actors._
import akka.actor.{ActorSystem, _}
import akka.pattern.ask
import akka.util.Timeout
import com.avaje.ebean._
import entities._
import play.api.Logger
import play.api.libs.json.{JsValue, _}
import play.api.mvc._
import utils.Sha256

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.reflect._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

class ValidationController @Inject()(system: ActorSystem) extends Controller with myTypes with Sha256 with SeqOps {

  val r = Shared.r

  val secActor = system.actorOf(SecurityActor.props, "sec-actor")

  val m = ru.runtimeMirror(getClass.getClassLoader)

  var colOrder = Seq.empty[String] //new ArrayBuffer[String]()
  var colMap = scala.collection.mutable.Map[String, String]()
  var pList = new java.util.HashMap[String, Object]()

  implicit val timeout: Timeout = 5.seconds
  val waitTime = 700.milli

  val validateUserSql = "select u.password, u.id, m.fname " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "u.username = :username"
  val validateEmailSql = "select u.password, u.id, m.fname " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "m.email = :email"

  def genSql(query: String, colMap: scala.collection.mutable.Map[String, String]): RawSql = {
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
  def getList[T : TypeTag](getter: String, query: String, colMap: scala.collection.mutable.Map[String, String],
                 pList: java.util.HashMap[String, AnyRef], t: T): List[T] = {
    val rawSql = genSql(query, colMap)
    val myType = getType(t)
    val obj = methodByReflectionO[T](getter, m, myType)
    obj(rawSql, Some(pList)).asInstanceOf[List[T]]
  }

  /**
   * generate Json return
    *
    * @param colOrder column seq
   * @param outputPage values
   * @tparam T  generic
   * @return
   */
  def genJson[T: TypeTag : ClassTag](colOrder: Array[String], outputPage: List[T]): JsValue = {
    var jRows: ArrayBuffer[JsValue] = new ArrayBuffer[JsValue](outputPage.length)
    val newRow =
      for {
        agg <- outputPage
        jRowBuf: List[JsValue] = jsonByReflection[T](colOrder, m, agg)
      } yield Json.obj("vals" -> JsArray(jRowBuf))
    jRows ++= newRow
    val jRowsList: JsValue = JsArray(jRows.toList)
    Json.obj("data" -> jRowsList)
  }

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

  def mirrorObjMatch[T : TypeTag : ClassTag](obj : Any) : JsValue =
    obj match {
      case _: String => Json.toJson(obj.toString)
      case _: Double => Json.toJson(obj.asInstanceOf[Double])
      case _: Long => Json.toJson(obj.asInstanceOf[Long])
      case _: Integer => Json.toJson(obj.asInstanceOf[Integer].toDouble)
      case _: Date => Json.toJson(obj.asInstanceOf[Date].formatted("MM/dd/yyyy"))
      case _: Any => Json.toJson("")
    }

  def jsonByReflection[T : TypeTag : ClassTag](colOrder: Array[String], m: ru.Mirror, agg: T): List[JsValue] = {
    val jRowBuf = new ListBuffer[JsValue]()
    val myType = getType(agg)
    val newRow =
      for {
        col <- colOrder
        fieldTermSymb = myType.decl(ru.TermName(col)).asTerm
        im = m.reflect[T](agg)(getClassTag(agg))
        fieldMirror = im.reflectField(fieldTermSymb)
        obj = fieldMirror.get
    } yield  { if (Option(obj).isEmpty) Json.toJson("") else mirrorObjMatch(obj) }
    jRowBuf ++=  newRow
    jRowBuf.toList
  }

  def ranSession = (1 to 50).map(_ => r.nextPrintableChar()).mkString("")

  def validateUser(name: String, passwd: String) = Action {
    val isEmail = name.contains("@")
    colMap.clear()
    colMap += "u.password" -> "password"
    colMap += "m.fname" -> "fname"

    pList.clear()
    pList += (if (isEmail) "email" -> name else "username" -> name)

    val pwdList = getList("allq", if (isEmail) validateEmailSql else validateUserSql, colMap, pList, MemberUser)
    if (pwdList.nonEmpty) {
      val membUser = pwdList.head.asInstanceOf[MemberUser]
      val pwdHash = membUser.password
      val valid = if (toHexString(passwd, Charset.forName("UTF-8")) == pwdHash) true else false
      if (valid)
        secActor ! InitSession(ranSession, membUser.id.toInt)
      Ok(if (valid)
        Json.obj("access" -> "auth", "uid" -> membUser.id, "welcome" -> membUser.fname, "sessAuth" -> ranSession)
      else
        Json.obj("access" -> "denied")
      )
    } else {
      Ok(Json.obj("access" -> "denied"))
    }
  }

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

  /**
  * speed up by mem caching the crud value
 *
  * @param id synth
  * @param key pattern
    * @param valOpt optional
    * @return
    */
  def processGet(id: Long, key: String, valOpt: Option[BaseObject]): JsValue = {
    val jSon =
      if (valOpt.isDefined) {
        setSeq(key, valOpt.get.toJSON.toString())
        valOpt.get.toJSON
      } else
        Json.obj("badkey" -> id)
    jSon
  }
}