package entities

import javax.persistence._
import play.api.libs.json._
import com.avaje.ebean.RawSql
import com.avaje.ebean.annotation.Sql
import common.{BaseObject, Dao}
import models._
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.{mutable => mu, immutable => im}

/**
 * Created by hkatz on 3/20/15.
 */
@Entity
@Sql
class Aggregates extends BaseObject {
  var credit: java.lang.Double = null
  var debit: java.lang.Double = null
  var period: String = null
  var periodType:String = null

  override def toJSON: JsValue =
    Json.obj(
      "credit" -> jsonNullCheck(credit),
      "debit" -> jsonNullCheck(debit),
      "period" -> jsonNullCheck(period),
      "period_type" -> jsonNullCheck(periodType)
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
  def allq(sql:RawSql, pList:Option[im.Map[String, AnyRef]]) : List[Aggregates] = {
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

  def toAggregate(agg: Aggregates): Aggregate =
    Aggregate(agg.debit, agg.credit, agg.period.toString, agg.periodType)
}
