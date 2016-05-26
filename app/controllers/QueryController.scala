package controllers

import java.util.Date
import javax.inject.Inject

import _root_.common.{Shared, myTypes}
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import entities._
import models.User
import models.services.DaoService
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsValue, _}
import play.api.mvc._

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.{immutable => im, mutable => mu}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect._
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

class QueryController @Inject() (val messagesApi: MessagesApi,
                                 val env: Environment[User, CookieAuthenticator],
                                 socialProviderRegistry: SocialProviderRegistry,
                                 val daoSVC: DaoService) extends Silhouette[User, CookieAuthenticator]
  with myTypes with SeqOps {

  val r = Shared.r

  val m = ru.runtimeMirror(getClass.getClassLoader)

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

  def myid = Action.async { implicit request =>
    SecuredRequestHandler { securedRequest =>
      Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
    }.map {
      case HandlerResult(r, Some(user)) => Ok(user.asInstanceOf[User].toJSON)
      case HandlerResult(r, None) => Ok(Json.obj("status" -> Json.toJson("no access")))
    }
  }

  override def onNotAuthenticated(request: RequestHeader): Option[Future[Result]] = {
    Some(Future.successful(Ok(Json.obj("status" -> Json.toJson("no access")))))
  }

  /**
   * generate Json return
    *
    * @param colOrder column seq
   * @param outputPage values
   * @tparam T  generic
   * @return
   */
  def genJson[T: TypeTag : ClassTag](colOrder: List[String], outputPage: List[T]): JsValue = {
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

  def mirrorObjMatch[T : TypeTag : ClassTag](obj : Any) : JsValue =
    obj match {
      case _: String => Json.toJson(obj.toString)
      case _: Double => Json.toJson(obj.asInstanceOf[Double])
      case _: Long => Json.toJson(obj.asInstanceOf[Long])
      case _: Integer => Json.toJson(obj.asInstanceOf[Integer].toDouble)
      case _: Date => Json.toJson(obj.asInstanceOf[Date].formatted("MM/dd/yyyy"))
      case _: Any => Json.toJson("")
    }

  def jsonByReflection[T : TypeTag : ClassTag](colOrder: List[String], m: ru.Mirror, agg: T): List[JsValue] = {
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
        val colMap = Map("sum(s.credit)" -> "credit",
          "sum(s.debit)" -> "debit",
          "cast(extract(month from s.trandate) as bigint)" -> "period",
        "'N'" -> "periodType")
        val pList = Map("year" -> year,
          "userid" -> uid)

        val aggList: List[Aggregates] = daoSVC.getList("allq", byMonthsql, colMap, pList, Aggregates).asInstanceOf[List[Aggregates]]
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
        val colMap = Map("sum(u.credit)" -> "credit",
          "sum(u.debit)" -> "debit",
          "u.trantype" -> "period",
          "'S'" -> "periodType")
        val pList = Map("year" -> year,
          "userid" -> uid)

        val aggList = daoSVC.getList("allq", byCategorysql, colMap, pList, Aggregates).asInstanceOf[List[Aggregates]]
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
        val colMap = Map("sum(s.credit)" -> "credit",
          "sum(s.debit)" -> "debit",
          "cast(extract(quarter from s.trandate) as bigint)" -> "period",
          "'Q'" -> "periodType")
        val pList = Map("year" -> year,
          "userid" -> uid)

        val aggList = daoSVC.getList("allq", byQuartersql, colMap, pList, Aggregates).asInstanceOf[List[Aggregates]]
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
   * find by email
    *
    * @param email key
   * @return
   */
  def byEmail(email: String)=  Action.async {
    val key = s"email-$email"
    getSeq(key).map {
      case None =>
        val colOrder = Seq("id","username","password","role","nodata","joined_date","activation","active_timestamp","active")
        val colMap = Map("u.id" -> "id",
         "u.username" -> "username",
          "u.password" -> "password",
          "u.role" -> "role",
          "u.nodata" -> "nodata",
          "u.joined_date" -> "joined_date",
          "u.activation" -> "activation",
          "u.active_timestamp" -> "active_timestamp",
          "u.active" -> "active")
        val pList = Map("email" -> email)

        val aggList = daoSVC.getList("allq", byEmailsql, colMap, pList, EmailUser).asInstanceOf[List[EmailUser]]
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
    val colMap = Map("m.id" -> "id",
      "m.email" -> "email",
      "m.fname" -> "fname",
      "m.phone_number" -> "phone_number",
      "m.type" -> "type",
      "m.street1" -> "street1",
      "m.street2" -> "street2",
      "m.city" -> "city",
      "m.state" -> "state",
      "m.country" -> "country",
      "m.joined_date" -> "joined_date",
      "m.ip" -> "ip",
      "m.lname" -> "lname",
      "m.zip" -> "zip",
      "m.userid" -> "userid",
      "u.id" -> "uid",
      "u.password" -> "password")
    val pList = Map("username" -> username)

    val aggList = daoSVC.getList("allq", byUsernamesql, colMap, pList, MemberUser)
    val result = genJson(MemberUser.getColOrder, aggList.asInstanceOf[List[MemberUser]]) //, m)
    Ok(result)
  }

  /**
   * show years for user
    *
    * @param uid key
   * @return
   */
  def byYears(uid: Integer) = Action {
    val colOrder = Seq("year")
    val colMap = Map("extract(year from t.trandate)" -> "year")
    val pList = Map("userid" -> uid)

    val aggList = daoSVC.getList("allq", getYearssql, colMap, pList, Years)
    val result = genJson(colOrder.toList, aggList.asInstanceOf[List[Years]]) //, m)
    Ok(result)
  }

}