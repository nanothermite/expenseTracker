package controllers

import java.util.Date
import javax.inject.Inject

import _root_.common.{BaseObject, Shared, myTypes}
import com.avaje.ebean._
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import entities._
import models.User
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsValue, _}
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}
import scala.concurrent.Future

class QueryController @Inject() (val messagesApi: MessagesApi,
                                 val env: Environment[User, CookieAuthenticator],
                                 socialProviderRegistry: SocialProviderRegistry) extends Silhouette[User, CookieAuthenticator]
  with myTypes with SeqOps {

  val r = Shared.r

  val m = ru.runtimeMirror(getClass.getClassLoader)

  var colOrder = Seq.empty[String] //new ArrayBuffer[String]()
  var colMap = scala.collection.mutable.Map[String, String]()
  var pList = new java.util.HashMap[String, Object]()
  val datePattern = "yyyy-MM-dd"

  val byMonthsql = "select sum(s.credit) as credit, " +
    "sum(s.debit) as debit, " +
    "cast(extract(month from s.trandate) as bigint) as period, " +
    " 'N' as periodType " +
    "from Transactions s " +
    "where extract(year from s.trandate) = :year and " +
    "s.userid = :userid " +
    "group by cast(extract(month from s.trandate) as bigint) " +
    "order by cast(extract(month from s.trandate) as bigint)"
  val byQuartersql = "select sum(s.credit) as credit, " +
    "sum(s.debit) as debit, " +
    "cast(extract(quarter from s.trandate) as bigint) as period, " +
    " 'Q' as periodType " +
    "from Transactions s " +
    "where extract(year from s.trandate) = :year and " +
    "s.userid = :userid " +
    "group by cast(extract(quarter from s.trandate) as bigint) " +
    "order by cast(extract(quarter from s.trandate) as bigint)"
  val byCategorysql = "select sum(u.credit) as credit, " +
    "sum(u.debit) as debit, " +
    "u.trantype as period, " +
    "'S' as periodType " +
    "from Transactions u " +
    "where extract(year from u.trandate) = :year and " +
    "   u.userid = :userid " +
    "group by u.trantype " +
    "order by u.trantype"
  val getYearssql = "select distinct extract(year from t.trandate) as year " +
    "from Transactions t " +
    "where userid = :userid " +
    "order by 1"
  val byEmailsql = "select u.id, u.username, u.password, u.role, " +
    "u.nodata, u.joined_date, u.activation, u.active_timestamp, u.active " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "m.email = :email"
  val byUsernamesql = "select m.id, m.email, m.fname, m.lname, m.phone_number, m.type, m.street1, m.street2, " +
    "m.city, m.state, m.country, m.joined_date, m.ip,  m.zip, m.userid, u.id, u.password " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "u.username = :username"

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
   * reflection on Class
    *
    * @param name  method
   * @param m     runtime mirror
   * @param obj   values in obj
   * @return
   */
  def methodByReflectionC(name: String, m: ru.Mirror, obj: AnyRef): MethodMirror = {
    val methodX = ru.typeOf[obj.type].decl(ru.TermName(name)).asMethod
    val im = m.reflect(obj)
    im.reflectMethod(methodX)
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

  /**
   * aggregate by month for a selected year for a particular userid
    *
    * @param year key
   * @param uid user
   * @return
   */
  def byMonth(year: Integer, uid: Integer) = Action.async {
    val key = s"mon-$year-$uid"
    getSeq(key).map {
      case None =>
        colOrder = Seq("credit","debit","period","periodType")

        colMap.clear()
        colMap += "sum(s.credit)" -> "credit"
        colMap += "sum(s.debit)" -> "debit"
        colMap += "cast(extract(month from s.trandate) as bigint)" -> "period"
        colMap += "'N'" -> "periodType"

        pList.clear()
        pList += "year" -> year
        pList += "userid" -> uid

        val aggList: List[Aggregates] = getList("allq", byMonthsql, colMap, pList, Aggregates).asInstanceOf[List[Aggregates]]
        val json: JsValue =
          if (aggList.nonEmpty) {
            val result = JsArray(aggList.map(_.toJSON))
            setSeq(key, result.toString())
            result
          } else
            Json.obj("result" -> "none")
      Ok(json)
      case Some(i: String) => Ok(Json.parse(i))
      case t: Any => Ok("broke")
    }
  }

  /**
   * aggregate by category for a particular year for a userid
    *
    * @param year key
   * @param uid  user
   * @return
   */
  def byCategory(year: Integer, uid: Integer) = Action.async {
    val key = s"cat-$year-$uid"
    getSeq(key).map {
      case None =>
        colOrder = Seq("credit", "debit", "period", "periodType")

        colMap = collection.mutable.Map("sum(u.credit)" -> "credit",
          "sum(u.debit)" -> "debit",
          "u.trantype" -> "period",
          "'S'" -> "periodType")

        pList.clear()
        pList += "year" -> year
        pList += "userid" -> uid

        val aggList = getList("allq", byCategorysql, colMap, pList, Aggregates).asInstanceOf[List[Aggregates]]
        val json: JsValue =
          if (aggList.nonEmpty) {
            val result = JsArray(aggList.map(_.toJSON))
            setSeq(key, result.toString())
            result
          } else
            Json.obj("result" -> "none")
        Ok(json)
      case Some(i: String) =>
        val iJson = Json.parse(i)
        Ok(iJson)
      case t: Any => Ok("broke")
    }
  }

  /**
   * pivot by Quarter
    *
    * @param year YYYY
   * @param uid userid
   * @return
   */
  def byQuarter(year: Integer, uid: Integer) = Action.async {
    val key = s"qrt-$year-$uid"
    getSeq(key).map {
      case None =>
        colOrder = Seq("credit", "debit", "period", "periodType")

        colMap = collection.mutable.Map("sum(s.credit)" -> "credit",
          "sum(s.debit)" -> "debit",
          "cast(extract(quarter from s.trandate) as bigint)" -> "period",
          "'Q'" -> "periodType")

        pList.clear()
        pList += "year" -> year
        pList += "userid" -> uid

        val aggList = getList("allq", byQuartersql, colMap, pList, Aggregates).asInstanceOf[List[Aggregates]]
        val retJson: JsValue =
          if (aggList.nonEmpty) {
            val result = JsArray(aggList.map(_.toJSON))
            //val result = genJson[Aggregates](colOrder.toArray, aggList.asInstanceOf[List[Aggregates]])
            setSeq(key, result.toString())
            result
          } else
            Json.obj("result" -> "none")
        Ok(retJson)
      case Some(i: String) => Ok(Json.parse(i))
      case t: AnyRef => Ok("broke")
    }
  }

  /**
   * find by email
    *
    * @param email key
   * @return
   */
  def byEmail(email: String)=  Action.async {
    val key = s"email-$email"
    getSeq(key).map {
      case None =>
        colOrder = Seq("id","username","password","role","nodata","joined_date","activation","active_timestamp","active")

        colMap.clear()
        colMap += "u.id" -> "id"
        colMap += "u.username" -> "username"
        colMap += "u.password" -> "password"
        colMap += "u.role" -> "role"
        colMap += "u.nodata" -> "nodata"
        colMap += "u.joined_date" -> "joined_date"
        colMap += "u.activation" -> "activation"
        colMap += "u.active_timestamp" -> "active_timestamp"
        colMap += "u.active" -> "active"

        pList.clear()
        pList += "email" -> email

        val aggList = getList("allq", byEmailsql, colMap, pList, EmailUser).asInstanceOf[List[EmailUser]]
        val retJson: JsValue =
          if (aggList.nonEmpty) {
            val result = JsArray(aggList.map(_.toJSON))
            setSeq(key, result.toString())
            result
          } else
            Json.obj("result" -> "none")
        Ok(retJson)
      case Some(i: String) => Ok(Json.parse(i))
      case t: AnyRef => Ok("broke")
    }
  }

  /**
   * show username records
    *
    * @param username key
   * @return
   */
  def byUsername(username: String) = Action {
    colMap.clear()
    colMap += "m.id" -> "id"
    colMap += "m.email" -> "email"
    colMap += "m.fname" -> "fname"
    colMap += "m.phone_number" -> "phone_number"
    colMap += "m.type" -> "type"
    colMap += "m.street1" -> "street1"
    colMap += "m.street2" -> "street2"
    colMap += "m.city" -> "city"
    colMap += "m.state" -> "state"
    colMap += "m.country" -> "country"
    colMap += "m.joined_date" -> "joined_date"
    colMap += "m.ip" -> "ip"
    colMap += "m.lname" -> "lname"
    colMap += "m.zip" -> "zip"
    colMap += "m.userid" -> "userid"
    colMap += "u.id" -> "uid"
    colMap += "u.password" -> "password"

    pList.clear()
    pList += "username" -> username

    val aggList = getList("allq", byUsernamesql, colMap, pList, MemberUser)
    val result = genJson(MemberUser.getColOrder.toArray[String], aggList.asInstanceOf[List[MemberUser]]) //, m)
    Ok(result)
  }

  /**
   * show years for user
    *
    * @param uid key
   * @return
   */
  def byYears(uid: Integer) = Action {
    colOrder = Seq("year")

    colMap.clear()
    colMap += "extract(year from t.trandate)" -> "year"

    pList.clear()
    pList += "userid" -> uid

    val aggList = getList("allq", getYearssql, colMap, pList, Years)
    val result = genJson(colOrder.toArray[String], aggList.asInstanceOf[List[Years]]) //, m)
    Ok(result)
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