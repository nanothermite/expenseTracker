package entities

import javax.persistence._

import argonaut.Argonaut._
import argonaut._
import com.avaje.ebean.RawSql
import com.avaje.ebean.annotation.Sql
import common.{BaseObject, Dao}
import models._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by hkatz on 3/20/15.
 */
@Entity
@Sql
class Aggregates extends BaseObject {
  var credit: java.lang.Double = null
  var debit: java.lang.Double = null
  var period:String = null
  var periodType:String = null

  override def toJSON: Json =
    Json(
      "credit" -> (if (credit == null) jNull else jNumber(credit)),
      "debit" -> (if (debit == null) jNull else jNumber(debit)),
      "period" -> (if (period == null) jNull else jString(period)),
      "period_type" -> (if (periodType == null) jNull else jString(periodType))
    )

}

object Aggregates extends Dao(classOf[Aggregates]){
  /**
   *
   * @return
   */
  def all() : List[Aggregates] = Aggregates.find.findList().asScala.toList

  /**
   *
   * @param sql query
   * @param pList param list
   * @return
   */
  def allq(sql:RawSql, pList:Option[java.util.HashMap[String, AnyRef]]) : List[Aggregates] = {
    val q = Aggregates.find
    if (pList.isDefined)
        for ((k:String,v:Object) <- pList.get) {
          q.setParameter(k, v)
        }
    q.setRawSql(sql)
    var resList : java.util.List[Aggregates] = q.findList()
    if (resList.isEmpty)
      resList = new java.util.ArrayList[Aggregates]
    resList.asScala.toList
  }

  def create(credit:Double,debit:Double,period:String,periodType:String): Unit = {
    val agg = new Aggregates
    agg.credit = credit
    agg.debit = debit
    agg.period = period
    agg.periodType = periodType
    save(agg)
  }

  def toAggregate(agg: Aggregates): Aggregate =
    Aggregate(agg.debit, agg.credit, agg.period, agg.periodType)
}
