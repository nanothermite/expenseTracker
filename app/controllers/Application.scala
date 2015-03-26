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

  def genSql(query: String, colMap: java.util.HashMap[String, String], pList: java.util.HashMap[String, AnyRef]): RawSql = {
    val rawSqlBld = RawSqlBuilder.parse(query)
    for (keyCol <- colMap.keySet) {
      rawSqlBld.columnMapping(keyCol, colMap.get(keyCol))
    }
    rawSqlBld.create()
  }

  def getList[_](getter: String, query: String, colMap: java.util.HashMap[String, String],
                 pList: java.util.HashMap[String, AnyRef], cl: Class[_]): List[_] = {
    val rawSql = genSql(query, colMap, pList)
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val obj = methodByReflectionO(getter, m, cl)
    obj(rawSql, Some(pList)).asInstanceOf[List[_]]
  }

  def genJson[_](colOrder: Array[String], outputPage: List[Aggregates], cl: Class[_]): Json = {
    val jTag: Json = jString("data")
    var jRows: ArrayBuffer[Json] = new ArrayBuffer[Json](outputPage.length)
    val m = ru.runtimeMirror(cl.getClassLoader)
    for (agg <- outputPage) {
      var jRowBuf: ArrayBuffer[Json] = jsonByReflection(colOrder, m, agg)
      jRows += Json.obj("vals" -> jArray(jRowBuf.toList))
    }
    val jRowsList: Json = jArray(jRows.toList)
    Json.obj("data" -> jRowsList)
  }

  def methodByReflectionC(name: String, m: ru.Mirror, obj: AnyRef): MethodMirror = {
    val methodX = ru.typeOf[obj.type].decl(ru.TermName(name)).asMethod
    val im = m.reflect(obj)
    im.reflectMethod(methodX)
  }

  def methodByReflectionO(name: String, m: ru.Mirror, obj: AnyRef): MethodMirror = {
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

  def byMonth(year: Integer, uid: Integer) = Action {
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
    val result = genJson(colOrder.toArray, aggList.asInstanceOf[List[Aggregates]], myCl)
    Ok(result.nospaces)
  }

  def byCategory(year: Integer, uid: Integer) = Action {
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

  def byQuarter(year: Integer, uid: Integer) = Action {
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

  def byEmail(email: String): Result = {
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

    val myCl = classOf[Uzer]
    val aggList = getList("allq", byEmailsql, colMap, pList, myCl) //, Uzer.class)
    //val result = genJson(Uzer.getColOrder().toArray, aggList)
    return Ok //(result)
  }

  def byUsername(username: String): Result = {
    colMap.clear();
    colMap.put("m.id", "id");
    colMap.put("m.email", "email");
    colMap.put("m.fname", "fname");
    colMap.put("m.phone_number", "phone_number");
    colMap.put("m.type", "type");
    colMap.put("m.street1", "street1");
    colMap.put("m.street2", "street2");
    colMap.put("m.city", "city");
    colMap.put("m.state", "state");
    colMap.put("m.country", "country");
    colMap.put("m.joined_date", "joined_date");
    colMap.put("m.ip", "ip");
    colMap.put("m.lname", "lname");
    colMap.put("m.zip", "zip");
    colMap.put("m.userid", "userid");
    colMap.put("u.id", "uid.id");

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
  def dropUser(userid: Long) = Action {
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
  def dropMember(memberid: Long) = Action {
    val curResult = Member.find(memberid)
    if (curResult != None) {
      Uzer.delete(curResult.get)
      Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(memberid)).nospaces)
    } else {
      BadRequest(Json.obj("status" -> jString("NF")).nospaces)
    }
  }

  /**
   * delete properties of existing object
   * @param contactid
   * @return
   */
  def dropContact(contactid: Long) = Action {
    val curResult = Contact.find(contactid)
    if (curResult != None) {
      Contact.delete(curResult.get)
      Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(contactid)).nospaces)
    } else {
      BadRequest(Json.obj("status" -> jString("NF")).nospaces)
    }
  }

  /**
   * delete properties of existing object
   * @param xactid
   * @return
   */
  def dropTransaction(xactid: Long) = Action {
    val curResult = Transactions.find(xactid)
    if (curResult != None) {
      Transactions.delete(curResult.get)
      Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(xactid)).nospaces)
    } else {
      BadRequest(Json.obj("status" -> jString("NF")).nospaces)
    }
  }

  implicit val uzerReads: Reads[Uzer] = (
    (JsPath \ "username").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "role").read[String] and
      (JsPath \ "nodata").readNullable[String] and
      (JsPath \ "joined_date").read[Date] and
      (JsPath \ "activation").readNullable[String] and
      (JsPath \ "active_timestamp").readNullable[Date] and
      (JsPath \ "active").read[String]
    )(Uzer.apply _)

  def addUser = Action(BodyParsers.parse.json) { request =>
    val uzerRes = request.body.validate[Uzer]
    uzerRes.fold(
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

  implicit val contactReads: Reads[Contact] = (
    //(JsPath \ "version").read[Integer] and
      (JsPath \ "bizname").readNullable[String] and
      (JsPath \ "industry").readNullable[String] and
      (JsPath \ "phone").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "state").readNullable[String] and
      (JsPath \ "identifier").readNullable[String]// and
      //(JsPath \ "userid").read[Uzer]
    )(Contact.apply _)

  def addContact(userid: Long) = Action(BodyParsers.parse.json) { request =>
    //val contactJson = request.body.asJson
    val contactRes = request.body.validate[Contact]
    contactRes.fold(
      errors => {
        BadRequest(Json.obj("status" -> jString("KO")).nospaces) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      },
      uz => {
        val contact = contactRes.get
        var parent = new Uzer
        val parentResult = Uzer.find(userid)
        if (parentResult != None) {
          parent = parentResult.get
          contact.userid = parent
          Contact.save(contact)
          Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(contact.id)).nospaces)
        } else {
          BadRequest(Json.obj("status" -> jString("NF")).nospaces)
        }
      }
    )
  }

  implicit val memberReads: Reads[Member] = (
    (JsPath \ "email").read[String] and
      (JsPath \ "fname").read[String] and
      (JsPath \ "lname").read[String] and
      (JsPath \ "phone_number").readNullable[String] and
      (JsPath \ "type").read[String] and
      (JsPath \ "street1").readNullable[String] and
      (JsPath \ "street2").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "state").readNullable[String] and
      (JsPath \ "country").read[String] and
      (JsPath \ "joined_date").read[Date] and
      (JsPath \ "ip").read[String] and
      (JsPath \ "zip").read[String] and
      (JsPath \ "userid").readNullable[String]
    )(Member.apply _)

  def addMember(userid: Long) = Action(BodyParsers.parse.json) { request =>
    val memberRes = request.body.validate[Member]
    memberRes.fold(
      errors => {
        BadRequest(Json.obj("status" -> jString("KO")).nospaces) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      },
      uz => {
        val member = memberRes.get
        val parentResult = Uzer.find(userid)
        if (parentResult != null) {
          val parent = parentResult.get
          member.uid = parent
          Member.save(member)
          Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(member.id)).nospaces)
        } else {
          BadRequest(Json.obj("status" -> jString("NF")).nospaces)
        }
      }
    )
  }

  implicit val transactionReads: Reads[Transactions] = (
    (JsPath \ "trandate").read[Date] and
      (JsPath \ "acct").readNullable[String] and
      (JsPath \ "vendor").readNullable[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "phone").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "state").readNullable[String] and
      (JsPath \ "debit").readNullable[Double] and
      (JsPath \ "credit").readNullable[Double] and
      (JsPath \ "trantype").readNullable[String]
    )(Transactions.apply _)

  def addTransaction(userid: Long, contactId: Long) = Action(BodyParsers.parse.json) { request =>
    val transRes = request.body.validate[Transactions]
    transRes.fold(
      errors => {
        BadRequest(Json.obj("status" -> jString("KO")).nospaces) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      },
      uz => {
        val trans = transRes.get
        val parentResult = Uzer.find(userid)
        val contactResult = Contact.find(contactId)
        if (parentResult != None && contactResult != None) {
          val parent = parentResult.get
          val contact = contactResult.get
          trans.userid = parent
          trans.contact = contact
          Transactions.save(trans)
          Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(trans.id)).nospaces)
        } else {
          Ok(Json.obj("status" -> jString("KO")).nospaces)
        }
      }
    )
  }

  val uzerUpdateReads : Reads[Uzer] = (
    (JsPath \ "username").readNullable[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "role").readNullable[String] and
      (JsPath \ "nodata").readNullable[String] and
      (JsPath \ "joined_date").readNullable[Date] and
      (JsPath \ "activation").readNullable[String] and
      (JsPath \ "active_timestamp").readNullable[Date] and
      (JsPath \ "active").readNullable[String]
    )(Uzer.apply2 _)

  def updateUser(userid: Long) = Action(BodyParsers.parse.json) { request =>
    val uzerRes = request.body.validate[Uzer](uzerUpdateReads)
    uzerRes.fold(
      errors => {
        BadRequest(Json.obj("status" -> jString("KO")).nospaces) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      },
      uz => {
        val newuzer = uzerRes.get
        val curResult = Uzer.find(userid)
        if (curResult != None) {
          val curuzer = curResult.get
          newuzer.id = userid
          Uzer.update(newuzer)
          Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(userid)).nospaces)
        } else {
          BadRequest(Json.obj("status" -> jString("NF")).nospaces)
        }
      }
    )
  }

  val memberUpdateReads: Reads[Member] = (
    (JsPath \ "email").readNullable[String] and
      (JsPath \ "fname").readNullable[String] and
      (JsPath \ "lname").readNullable[String] and
      (JsPath \ "phone_number").readNullable[String] and
      (JsPath \ "type").readNullable[String] and
      (JsPath \ "street1").readNullable[String] and
      (JsPath \ "street2").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "state").readNullable[String] and
      (JsPath \ "country").readNullable[String] and
      (JsPath \ "joined_date").readNullable[Date] and
      (JsPath \ "ip").readNullable[String] and
      (JsPath \ "zip").readNullable[String] and
      (JsPath \ "userid").readNullable[String]
    )(Member.apply2 _)

  def updateMember(memberid: Long) = Action(BodyParsers.parse.json) { request =>
    val memberRes = request.body.validate[Member](memberUpdateReads)
    memberRes.fold(
      errors => {
        BadRequest(Json.obj("status" -> jString("KO")).nospaces) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      },
      uz => {
        val newmember = memberRes.get
        val curResult = Member.find(memberid)
        if (curResult != None) {
          val curmember = curResult.get
          newmember.id = memberid
          Member.update(newmember)
          Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(memberid)).nospaces)
        } else {
          BadRequest(Json.obj("status" -> jString("NF")).nospaces)
        }
      }
    )
  }

  val contactUpdateReads: Reads[Contact] = (
    //(JsPath \ "version").read[Integer] and
    (JsPath \ "bizname").readNullable[String] and
      (JsPath \ "industry").readNullable[String] and
      (JsPath \ "phone").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "state").readNullable[String] and
      (JsPath \ "identifier").readNullable[String]
    )(Contact.apply2 _)

  def updateContact(contactid: Long) = Action(BodyParsers.parse.json) { request =>
    val contactRes = request.body.validate[Contact](contactUpdateReads)
    contactRes.fold(
      errors => {
        BadRequest(Json.obj("status" -> jString("KO")).nospaces) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      },
      uz => {
        val newcontact = contactRes.get
        val curResult = Contact.find(contactid)
        if (curResult != None) {
          val curcontact = curResult.get
          newcontact.id = contactid
          Contact.update(newcontact)
          Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(contactid)).nospaces)
        } else {
          BadRequest(Json.obj("status" -> jString("NF")).nospaces) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
        }
      }
    )
  }

  val transactionUpdateReads: Reads[Transactions] = (
    (JsPath \ "trandate").readNullable[Date] and
      (JsPath \ "acct").readNullable[String] and
      (JsPath \ "vendor").readNullable[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "phone").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "state").readNullable[String] and
      (JsPath \ "debit").readNullable[Double] and
      (JsPath \ "credit").readNullable[Double] and
      (JsPath \ "trantype").readNullable[String]
    )(Transactions.apply2 _)

  def updateTransaction(xactid: Long) = Action(BodyParsers.parse.json) { request =>
    val xactRes = request.body.validate[Transactions](transactionUpdateReads)
    xactRes.fold(
      errors => {
        BadRequest(Json.obj("status" -> jString("KO")).nospaces) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      },
      uz => {
        val newxact = xactRes.get
        val curResult = Transactions.find(xactid)
        if (curResult != None) {
          val curxact = curResult.get
          newxact.id = xactid
          Transactions.update(newxact)
          Ok(Json.obj("status" -> jString("OK"), "id" -> jNumber(xactid)).nospaces)
        } else {
          BadRequest(Json.obj("status" -> jString("NF")).nospaces) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
        }
      }
    )
  }
}
