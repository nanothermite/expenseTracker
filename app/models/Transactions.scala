package models

import java.util.Date

import com.avaje.ebean.RawSql
import common.Dao
import play.data.validation.Constraints
import javax.persistence._
import java.util

import scala.collection.JavaConverters._

/**
 * Created by hkatz on 9/5/14.
 */
object Transactions extends Dao(classOf[Transactions]){
  def all() : List[Transactions] = Transactions.find.findList().asScala.toList

  def allq(sql:RawSql) : List[Transactions] = {
    val q = find()
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
    var trans = new Transactions
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
}

@Entity
class Transactions {
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

  var debit: Double = 0d

  var credit: Double = 0d

  var trantype: String = null

  override def toString : String =  {
    var s = ""
    if (id != 0l && userid != null)
      s = f"$id%d - $userid"
    s
  }
}
