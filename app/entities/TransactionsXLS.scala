package entities

import java.util.Date
import javax.persistence._
import com.avaje.ebean.annotation.Sql
import play.api.libs.json._
import com.avaje.ebean.RawSql
import common.{BaseObject, Dao}
import org.joda.time.DateTime
import play.data.validation.Constraints
import utils.{DateUtils, DateFormatter}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by hkatz on 9/5/14.
 */
@Entity
@Sql
class TransactionsXLS extends BaseObject {
  @Constraints.Required
  var trandate: Date = null

  var acct: String = null

  var vendor: String = null

  var description: String = null

  var debit: java.lang.Double = 0d

  var credit: java.lang.Double = 0d

  var trantype: String = null

  def toJSON = Json.obj(
    "trandate" -> DateFormatter.formatDate(new DateTime(trandate)),
    "acct" -> jsonNullCheck(acct),
    "vendor" -> jsonNullCheck(vendor),
    "description" -> jsonNullCheck(description),
    "debit" -> jsonNullCheck(debit),
    "credit" -> jsonNullCheck(credit),
    "trantype" -> jsonNullCheck(trantype)
  )
}

object TransactionsXLS extends Dao(classOf[TransactionsXLS]) {

  def all(userid: Long): Option[List[TransactionsXLS]] = {
    val uzer = Uzer.find(userid.toInt)
    val objList =
      if (uzer.isDefined) {
        val objList = TransactionsXLS.find.where.eq("userid", uzer.get).findList
        if (objList.nonEmpty)
          objList.asScala.toList
        else
          List.empty[TransactionsXLS]
      }
      else
        List.empty[TransactionsXLS]
    Some(objList)
  }

  def allq(sql:RawSql) : List[TransactionsXLS] = {
    val q = find
    q.setRawSql(sql)
    q.findList().asScala.toList
  }

  def getColOrder: List[String] = List("id","trandate","acct","vendor","description","debit","credit","trantype","contact","userid")

  def getMetas: Map[String, Class[_]] = Map(
    "trandate" -> classOf[Date],
    "acct" -> classOf[String],
    "description" -> classOf[String],
    "debit" -> classOf[Double],
    "credit" -> classOf[Double],
    "trantype" -> classOf[String]
  )

  def getReqd: Map[String, Integer] = Map("trandate" -> 1)

  def empty = apply(null, None, None, None, None, None, None /* None, None, None, */)

  def apply(trandate: Date, acct: Option[String], vendor: Option[String], description: Option[String],
            debit: Option[Double], credit: Option[Double], trantype: Option[String]): TransactionsXLS = {
    val trans = new TransactionsXLS
    trans.trandate = trandate
    if (acct.isDefined)
      trans.acct = acct.get
    if (vendor.isDefined)
      trans.vendor = vendor.get
    if (description.isDefined)
      trans.description = description.get
    if (debit.isDefined)
      trans.debit = debit.get
    if (credit.isDefined)
      trans.credit = credit.get
    if (trantype.isDefined)
      trans.trantype = trantype.get
    trans
  }

  private def dblCheck(dbl: String): Double = if (dbl == " ") 0.0 else dbl.toDouble

  def apply(strs: List[String]): TransactionsXLS = {
    val trandate = DateUtils.dateParse(strs.head, DateUtils.YMD).toDate
    strs.size match {
      case 1 => apply(trandate, None, None, None, None, None, None)
      case 2 => apply(trandate, Some(strs(1)), None, None, None, None, None)
      case 3 => apply(trandate, Some(strs(1)), Some(strs(2)), None, None, None, None)
      case 4 => apply(trandate, Some(strs(1)), Some(strs(2)), Some(strs(3)), None, None, None)
      case 5 => apply(trandate, Some(strs(1)), Some(strs(2)), Some(strs(3)), Some(dblCheck(strs(4))), None, None)
      case 6 => apply(trandate, Some(strs(1)), Some(strs(2)), Some(strs(3)), Some(dblCheck(strs(4))), Some(dblCheck(strs(5))), None)
      case 7 => apply(trandate, Some(strs(1)), Some(strs(2)), Some(strs(3)), Some(dblCheck(strs(4))), Some(dblCheck(strs(5))), Some(strs(6)))
    }
  }

  def apply4(xls: TransactionsXLS)(implicit uzer: Uzer, contact: Contact): Transactions = {
    val trans = new Transactions
    trans.trandate = xls.trandate
    trans.acct = xls.acct
    trans.description = xls.description
    trans.debit = xls.debit
    trans.credit = xls.credit
    trans.trantype = xls.trantype
    trans.userid = uzer
    trans.contact = contact
    trans
  }

  def findBiz(xls: TransactionsXLS)(implicit uzer: Uzer): (List[Transactions], Contact) = {
    val contacts = Contact.find.where.eq("userid", uzer).eq("bizname", xls.vendor).findList.asScala.toList
    if (contacts.nonEmpty && contacts.size == 1) {
      (Transactions.find.where.
        eq("userid", uzer).
        eq("contact", contacts.head).
        eq("trandate", xls.trandate).
        eq("debit", xls.debit).
        eq("credit", xls.credit).
        findList.asScala.toList, contacts.head)
    } else
      (List.empty[Transactions], Contact.empty)
  }

}