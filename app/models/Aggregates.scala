package models

import javax.persistence._

import com.avaje.ebean.RawSql
import com.avaje.ebean.annotation.Sql
import common.Dao

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by hkatz on 3/20/15.
 */
@Entity
@Sql
class Aggregates {
  var credit: java.lang.Double = null
  var debit: java.lang.Double = null
  var period:String = null
  var periodType:String = null
}

object Aggregates extends Dao(classOf[Aggregates]){
  /**
   *
   * @return
   */
  def all() : List[Aggregates] = Aggregates.find.findList().asScala.toList

  /**
   *
   * @param sql
   * @param pList
   * @return
   */
  def allq(sql:RawSql, pList:Option[java.util.HashMap[String, AnyRef]]) : List[Aggregates] = {
    val q = Aggregates.find()
    if (!pList.isEmpty)
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
    var agg = new Aggregates
    agg.credit = credit
    agg.debit = debit
    agg.period = period
    agg.periodType = periodType
    save(agg)
  }
}
