package entities

import java.util.Date
import javax.persistence._

import argonaut.Argonaut._
import argonaut._

import com.avaje.ebean.RawSql
import common.{BaseObject, Dao}
import org.joda.time.DateTime
import play.data.validation.Constraints
import utils.DateFormatter

import scala.collection.JavaConverters._

/**
 * Created by hkatz on 9/5/14.
 */

object Transactions extends Dao(classOf[Transactions]) {
  val byMonth = "byMonth"
  val byQuarter = "byQuarter"
  val year = "year"

  def all() : List[Transactions] = Transactions.find.findList().asScala.toList

  def allq(sql:RawSql) : List[Transactions] = {
    val q = find
    q.setRawSql(sql)
    q.findList().asScala.toList
  }

  def getColOrder: List[String] = List("id","trandate","acct","vendor","description","phone","city",
    "state","debit","credit","trantype","contact","userid")

  def getMetas: Map[String, Class[_]] = Map(
    "trandate" -> classOf[Date],
    "acct" -> classOf[String],
    "vendor" -> classOf[String],
    "description" -> classOf[String],
    "phone" -> classOf[String],
    "city" -> classOf[String],
    "state" -> classOf[String],
    "debit" -> classOf[Double],
    "credit" -> classOf[Double],
    "trantype" -> classOf[String]
  )

  def getReqd: Map[String, Integer] = Map("trandate" -> 1)

  def create(id: Long,contact: Contact,userid: Uzer,trandate: Date,acct: String,vendor: String,
             description: String,phone: String,city: String,state: String,debit: Double,credit: Double,
             trantype: String): Unit = {
    val trans = new Transactions
    trans.id = id
    trans.contact = contact
    trans.userid = userid
    trans.trandate = trandate
    trans.acct = acct
    trans.vendor = vendor
    trans.description = description
    trans.city = city
    trans.state = state
    trans.debit = debit
    trans.credit = credit
    trans.trantype = trantype
    save(trans)
  }

  def apply(trandate: Date,acct: Option[String],vendor: Option[String],
            description: Option[String],phone: Option[String],city: Option[String],state: Option[String],
            debit: Option[Double],credit: Option[Double], trantype: Option[String]): Transactions = {
    val trans = new Transactions
    trans.trandate = trandate
    if (acct.isDefined)
      trans.acct = acct.get
    if (vendor.isDefined)
    trans.vendor = vendor.get
    if (description.isDefined)
      trans.description = description.get
    if (phone.isDefined)
    trans.phone = phone.get
    if (city.isDefined)
      trans.city = city.get
    if (state.isDefined)
      trans.state = state.get
    if (debit.isDefined)
      trans.debit = debit.get
    if (credit.isDefined)
      trans.credit = credit.get
    if (trantype.isDefined)
      trans.trantype = trantype.get
    trans
  }

  def apply2(trandate: Option[Date],acct: Option[String],vendor: Option[String],
            description: Option[String],phone: Option[String],city: Option[String],state: Option[String],
            debit: Option[Double],credit: Option[Double], trantype: Option[String]): Transactions = {
    val trans = new Transactions
    if (trandate.isDefined)
      trans.trandate = trandate.get
    if (acct.isDefined)
      trans.acct = acct.get
    if (vendor.isDefined)
      trans.vendor = vendor.get
    if (description.isDefined)
      trans.description = description.get
    if (phone.isDefined)
      trans.phone = phone.get
    if (city.isDefined)
      trans.city = city.get
    if (state.isDefined)
      trans.state = state.get
    if (debit.isDefined)
      trans.debit = debit.get
    if (credit.isDefined)
      trans.credit = credit.get
    if (trantype.isDefined)
      trans.trantype = trantype.get
    trans
  }
}

@Entity
class Transactions extends BaseObject {
  @Id
  var id: Long = 0l

  @Constraints.Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "contact")
  var contact: Contact = null

  @Constraints.Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "userid")
  var userid: Uzer = null

  @Constraints.Required
  var trandate: Date = null

  var acct: String = null

  var vendor: String = null

  var description: String = null

  var phone: String = null

  var city: String = null

  var state: String = null

  var debit: java.lang.Double = 0d

  var credit: java.lang.Double = 0d

  var trantype: String = null

  override def toString : String =  {
    var s = ""
    if (id != 0l && userid != null)
      s = f"$id%d - $userid"
    s
  }

  def toJSON: Json = Json(
    "id" -> jNumber(id),
    "trandate" -> jString(DateFormatter.formatDate(new DateTime(trandate))),
    "acct" -> jsonNullCheck(acct),
    "vendor" -> jsonNullCheck(vendor),
    "description" -> jsonNullCheck(description),
    "phone" -> jsonNullCheck(phone),
    "city" -> jsonNullCheck(city),
    "state" -> jsonNullCheck(state),
    "debit" -> jsonNullCheck(debit),
    "credit" -> jsonNullCheck(credit),
    "trantype" -> jsonNullCheck(trantype)
  )
}
