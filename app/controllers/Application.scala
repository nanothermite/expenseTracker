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
import scala.reflect._

object Application extends Controller {

  val m = ru.runtimeMirror(getClass.getClassLoader)

  var colOrder = new ArrayBuffer[String]()
  var colMap = scala.collection.mutable.Map[String, String]()
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
  val byUsernamesql = "select m.id, m.email, m.fname, m.lname, m.phone_number, m.type, m.street1, m.street2, " +
    "m.city, m.state, m.country, m.joined_date, m.ip,  m.zip, m.userid, u.id " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "u.username = :username"

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def genSql(query: String, colMap: scala.collection.mutable.Map[String, String], pList: java.util.HashMap[String, AnyRef]): RawSql = {
    val rawSqlBld = RawSqlBuilder.parse(query)
    for ((k, v) <- colMap) {
      rawSqlBld.columnMapping(k, v)
    }
    rawSqlBld.create()
  }

  def getType[T: TypeTag](obj: T) = typeOf[T]
  def getTypeTag[T: TypeTag](obj: T) = typeTag[T]
  def getClassTag[T: ClassTag](obj: T) = classTag[T]

  def getList[T : TypeTag](getter: String, query: String, colMap: scala.collection.mutable.Map[String, String],
                 pList: java.util.HashMap[String, AnyRef], t: T): List[T] = {
    val rawSql = genSql(query, colMap, pList)
    val myType = getType(t)
    val obj = methodByReflectionO[T](getter, m, myType)
    obj(rawSql, Some(pList)).asInstanceOf[List[T]]
  }

  def genJson[T: TypeTag : ClassTag](colOrder: Array[String], outputPage: List[T], m: ru.Mirror): Json = {
    var jRows: ArrayBuffer[Json] = new ArrayBuffer[Json](outputPage.length)
    for (agg <- outputPage) {
      val jRowBuf: ArrayBuffer[Json] = jsonByReflection[T](colOrder, m, agg)
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

  def methodByReflectionO[T : TypeTag](name: String, m: ru.Mirror, tru: Type): MethodMirror = {
    val modX = tru.termSymbol.asModule
    val methodX = tru.decl(ru.TermName(name)).asMethod
    val mm = m.reflectModule(modX)
    val im = m.reflect(mm.instance)
    im.reflectMethod(methodX)
  }

  def jsonByReflection[T : TypeTag : ClassTag](colOrder: Array[String], m: ru.Mirror, agg: T): ArrayBuffer[Json] = {
    var jRowBuf: ArrayBuffer[Json] = new ArrayBuffer[Json]()
    val myType = getType(agg)
    for (col <- colOrder) {
      val fieldTermSymb = myType.decl(ru.TermName(col)).asTerm
      val im = m.reflect[T](agg)(getClassTag(agg))
      val fieldMirror = im.reflectField(fieldTermSymb)

      jRowBuf += (if (fieldMirror.get == null) jString("")
      else {
        fieldMirror.get match {
          case _: String => jString(fieldMirror.get.toString)
          case _: lang.Double => jNumber(fieldMirror.get.asInstanceOf[lang.Double])
          case _: lang.Long => jNumber(fieldMirror.get.asInstanceOf[lang.Long].toLong)
          case _: lang.Integer => jNumber(fieldMirror.get.asInstanceOf[lang.Integer].toDouble)
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
    colMap += "sum(s.credit)" -> "credit"
    colMap += "sum(s.debit)" -> "debit"
    colMap += "cast(extract(month from s.trandate) as text)" -> "period"
    colMap += "'N'" -> "periodType"

    pList.clear()
    pList += "year" -> year
    pList += "userid" -> uid

    val aggList = getList("allq", byMonthsql, colMap, pList, Aggregates)
    val result = genJson[Aggregates](colOrder.toArray, aggList.asInstanceOf[List[Aggregates]], m)
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

    val aggList = getList("allq", byCategorysql, colMap, pList, Aggregates)
    val result = genJson[Aggregates](colOrder.toArray, aggList.asInstanceOf[List[Aggregates]], m)
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

    val aggList = getList("allq", byQuartersql, colMap, pList, Aggregates)
    val result = genJson[Aggregates](colOrder.toArray, aggList.asInstanceOf[List[Aggregates]], m)
    Ok(result.nospaces)
  }

  def byEmail(email: String)=  Action {
    colOrder.clear()
    colOrder += "id"
    colOrder += "username"
    colOrder += "password"
    colOrder += "role"
    colOrder += "nodata"
    colOrder += "joined_date"
    colOrder += "activation"
    colOrder += "active_timestamp"
    colOrder += "active"

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

    val aggList = getList("allq", byEmailsql, colMap, pList, EmailUser)
    val result = genJson(colOrder.toArray, aggList.asInstanceOf[List[EmailUser]], m)
    Ok(result.nospaces)
  }

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

    pList.clear()
    pList += "username" -> username

    val aggList = getList("allq", byUsernamesql, colMap, pList, MemberUser)
    val result = genJson(Member.getColOrder.toArray[String], aggList.asInstanceOf[List[MemberUser]], m)
    Ok(result.nospaces)
  }

  def byYears(uid: Integer) = Action {
    colOrder.clear()
    colOrder +="year"

    colMap.clear()
    colMap += "extract(year from t.trandate)" -> "year"

    pList.clear()
    pList += "userid" -> uid

    val aggList = getList("allq", getYearssql, colMap, pList, Years)
    val result = genJson(colOrder.toArray[String], aggList.asInstanceOf[List[Years]], m)
    Ok(result.nospaces)
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
