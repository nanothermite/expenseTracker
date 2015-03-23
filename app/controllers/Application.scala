package controllers

import java.lang
import java.util.Date

import com.avaje.ebean._
import argonaut._, Argonaut._
import models._
import play.api.libs.json.{JsPath, Reads}
import play.api.mvc._
import play.api.libs.functional.syntax._

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe._

object Application extends Controller {

  var colOrder = new ArrayBuffer[String]()
  var colMap = new java.util.HashMap[String, String]()
  var pList = new java.util.HashMap[String, Object]()
  val datePattern = "yyyy-MM-dd"

  val byMonthsql = "select sum(s.credit) as credit, " +
    "sum(s.debit) as debit, " +
    "cast(extract(month from s.trandate) as text) as period, " +
    " 'N' as periodType " +
    "from Transactions s " +
    "where extract(year from s.trandate) = :year and " +
    "s.userid = :userid " +
    "group by cast(extract(month from s.trandate) as text) " +
    "order by cast(extract(month from s.trandate) as text)"
  val byQuartersql = "select sum(s.credit) as credit, " +
    "sum(s.debit) as debit, " +
    "cast(extract(quarter from s.trandate) as text) as period, " +
    " 'N' as periodType " +
    "from Transactions s " +
    "where extract(year from s.trandate) = :year and " +
    "s.userid = :userid " +
    "group by cast(extract(quarter from s.trandate) as text) " +
    "order by cast(extract(quarter from s.trandate) as text)"
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
    "order by 1";
  val byEmailsql = "select u.id, u.username, u.password, u.role, " +
    "u.nodata, u.joined_date, u.activation, u.active_timestamp, u.active " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "m.email = :email"
  val byUsernamesql = "select m.id, m.email, m.fname, m.phone_number, m.type, m.street1, m.street2, " +
    "m.city, m.state, m.country, m.joined_date, m.ip, m.lname, m.zip, m.userid, u.id " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "u.username = :username"

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def genSql(query: String, colMap: java.util.HashMap[String, String], pList: java.util.HashMap[String, AnyRef]) : RawSql = {
    val rawSqlBld = RawSqlBuilder.parse(query)
    for (keyCol <- colMap.keySet) {
      rawSqlBld.columnMapping(keyCol, colMap.get(keyCol))
    }
    rawSqlBld.create()
  }

  def getList[_](getter: String, query: String, colMap: java.util.HashMap[String, String],
                 pList: java.util.HashMap[String, AnyRef], cl : Class[_]) : List[_] = {
    val rawSql = genSql(query, colMap, pList)
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val obj = methodByReflectionO(getter, m, cl)
    obj(rawSql, Some(pList)).asInstanceOf[List[_]]
  }

  def genJson[_](colOrder : Array[String], outputPage : List[Aggregates], cl : Class[_]) : Json = {
    val jTag : Json = jString("data")
    var jRows : ArrayBuffer[Json] =  new ArrayBuffer[Json](outputPage.length)
    val m = ru.runtimeMirror(cl.getClassLoader)
    for (agg <- outputPage) {
      var jRowBuf: ArrayBuffer[Json] = jsonByReflection(colOrder, m, agg)
      jRows += Json.obj("vals" -> jArray(jRowBuf.toList))
    }
    val jRowsList : Json = jArray(jRows.toList)
    Json.obj("data" -> jRowsList)
  }

  def methodByReflectionC(name : String, m: ru.Mirror, obj : AnyRef) : MethodMirror = {
    val methodX = ru.typeOf[obj.type].decl(ru.TermName(name)).asMethod
    val im = m.reflect(obj)
    im.reflectMethod(methodX)
  }

  def methodByReflectionO(name : String, m: ru.Mirror, obj : AnyRef) : MethodMirror = {
    val modX = ru.typeOf[Aggregates.type].termSymbol.asModule
    val methodX = ru.typeOf[Aggregates.type].decl(ru.TermName(name)).asMethod
    val mm = m.reflectModule(modX)
    val im = m.reflect(mm.instance)
    im.reflectMethod(methodX)
  }

  def jsonByReflection(colOrder: Array[String], m: ru.Mirror, agg: Aggregates): ArrayBuffer[Json] = {
    var jRowBuf: ArrayBuffer[Json] = new ArrayBuffer[Json]()
    for (col <- colOrder) {
      val fieldTermSymb = ru.typeOf[agg.type].decl(ru.TermName(col)).asTerm
      val im = m.reflect(agg)
      val fieldMirror = im.reflectField(fieldTermSymb)

      jRowBuf += (if (fieldMirror.get == null) jString("")
      else {
        fieldMirror.get match {
          case _: String => jString(fieldMirror.get.toString)
          case _: lang.Double => jNumber(fieldMirror.get.asInstanceOf[lang.Double])
          //            case _:java.lang.Integer => (fieldMirror.get.asInstanceOf[java.lang.Integer])
          case _: Date => jString(fieldMirror.get.asInstanceOf[Date].formatted("MM/dd/yyyy"))
        }
      })
    }
    jRowBuf
  }

  def byMonth(year : Integer, uid : Integer) = Action {
    colOrder.clear()
    colOrder += "credit"
    colOrder += "debit"
    colOrder += "period"
    colOrder += "periodType"

    colMap.clear()
    colMap.put("sum(s.credit)", "credit")
    colMap.put("sum(s.debit)", "debit")
    colMap.put("cast(extract(month from s.trandate) as text)", "period")
    colMap.put("'N'", "periodType")

    pList.clear()
    pList.put("year", year)
    pList.put("userid", uid)

    val myCl = classOf[Aggregates]
    val aggList = getList("allq", byMonthsql, colMap, pList, myCl)
    val result =  genJson(colOrder.toArray, aggList.asInstanceOf[List[Aggregates]], myCl)
    Ok(result.nospaces)
  }

  def byCategory(year : Integer, uid : Integer)  = Action {
    colOrder.clear()
    colOrder += "credit"
    colOrder += "debit"
    colOrder += "period"
    colOrder += "periodType"

    colMap.clear()
    colMap += "sum(u.credit)" -> "credit"
    colMap += "sum(u.debit)" -> "debit"
    colMap += "u.trantype" -> "period"
    colMap += "'S'" -> "periodType"

    pList.clear()
    pList += "year" -> year
    pList += "userid" -> uid

    val myCl = classOf[Aggregates]
    val aggList = getList("allq", byCategorysql, colMap, pList, myCl)
    val result = genJson(colOrder.toArray, aggList.asInstanceOf[List[Aggregates]], myCl)
    Ok(result.nospaces)
  }

  def byQuarter(year : Integer, uid: Integer) = Action {
    colOrder.clear()
    colOrder += "credit"
    colOrder += "debit"
    colOrder += "period"
    colOrder += "periodType"

    colMap.clear()
    colMap += "sum(s.credit)" -> "credit"
    colMap += "sum(s.debit)" -> "debit"
    colMap += "cast(extract(quarter from s.trandate) as text)" -> "period"
    colMap += "'N'" -> "periodType"

    pList.clear()
    pList += "year" -> year
    pList += "userid" -> uid

    val myCl = classOf[Aggregates]
    val aggList = getList("allq", byQuartersql, colMap, pList, myCl)
    val result = genJson(colOrder.toArray, aggList.asInstanceOf[List[Aggregates]], myCl)
    Ok(result.nospaces)
  }

  def byEmail(email: String) : Result = {
    colMap.clear()
    colMap += "u.id" ->  "id"
    colMap += "u.username" ->  "username"
    colMap += "u.password" ->  "password"
    colMap += "u.role" ->  "role"
    colMap += "u.nodata" ->  "nodata"
    colMap += "u.joined_date" ->  "joined_date"
    colMap += "u.activation" ->  "activation"
    colMap += "u.active_timestamp" ->  "active_timestamp"
    colMap += "u.active" ->  "active"

    pList.clear()
    pList += "email" ->  email

    val myCl = classOf[Uzer]
    val aggList = getList("allq", byEmailsql, colMap, pList, myCl) //, Uzer.class)
    //val result = genJson(Uzer.getColOrder().toArray, aggList)
    return Ok //(result)
  }

  def byUsername(username : String) : Result = {
    colMap.clear();
    colMap.put("m.id","id");
    colMap.put("m.email","email");
    colMap.put("m.fname","fname");
    colMap.put("m.phone_number","phone_number");
    colMap.put("m.type","type");
    colMap.put("m.street1","street1");
    colMap.put("m.street2", "street2");
    colMap.put("m.city", "city");
    colMap.put("m.state","state");
    colMap.put("m.country","country");
    colMap.put("m.joined_date","joined_date");
    colMap.put("m.ip","ip");
    colMap.put("m.lname","lname");
    colMap.put("m.zip","zip");
    colMap.put("m.userid", "userid");
    colMap.put("u.id","uid.id");

    pList.clear();
    pList.put("username", username);

    val myCl = classOf[Member]
    val aggList = getList("allq", byUsernamesql, colMap, pList, myCl) //, Member.class);
    //val result = genJson(Member.getColOrder(), aggList);
    return Ok //(result);
  }

  /**
   * delete properties of existing object
   * @param userid
   * @return
   */
  def dropUser(userid : Long) = Action {
    var ret = Ok
    val existingObj = Uzer.find(userid)
    if (existingObj == null) {
      ret = BadRequest
    } else {
      Uzer.delete(existingObj)
    }
    ret
  }

  /**
   * delete properties of existing object
   * @param memberid
   * @return
   */
  def dropMember(memberid : Long) : Result = {
    var ret = Ok
    val existingObj = Member.find(memberid)
    if (existingObj == null) {
      ret = BadRequest
    } else {
      Uzer.delete(existingObj)
    }
    ret
  }

  /**
   * delete properties of existing object
   * @param contactid
   * @return
   */
  def dropContact(contactid : Long) : Result = {
    var ret = Ok
    val existingObj = Contact.find(contactid)
    if (existingObj == null) {
      ret = BadRequest
    } else {
      Contact.delete(existingObj)
    }
    ret
  }

  /**
   * delete properties of existing object
   * @param xactid
   * @return
   */
  def dropTransaction(xactid : Long) : Result = {
    var ret = Ok
    val existingObj = Transactions.find(xactid)
    if (existingObj == null) {
      ret = BadRequest
    } else {
      Transactions.delete(existingObj)
    }
    ret
  }

implicit val placeReads: Reads[Uzer] = (
    (JsPath \ "username").read[String] and
    (JsPath \ "password").read[String] and
    (JsPath \ "role").read[String] and
    (JsPath \ "nodata").read[String] and
    (JsPath \ "joined_date").read[Date] and
    (JsPath \ "activation").read[String] and
    (JsPath \ "active_timestamp").read[Date] and
    (JsPath \ "active").read[String]
  )(Uzer.apply _)

  def addUser = Action(BodyParsers.parse.json) { request =>
    val uzerRes = request.body.validate[Uzer]
    val uzid = -1
    uzerRes.fold (
      errors => {
        BadRequest(Json.obj("status" -> jString("KO")).nospaces) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
    },
    uz => {
      val uzer = uzerRes.get
      Uzer.save(uzer)
      Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(uzer.id)).nospaces)
    }
    )
  }

}