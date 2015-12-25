package entities

import java.util.Date
import javax.persistence._

import com.avaje.ebean.RawSql
import com.avaje.ebean.annotation.Sql
import common.Dao

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

@Entity
@Sql class Years {
  var year: Integer = null
}

object Years extends Dao(classOf[Years]){
  /**
   *
   * @return
   */
  def all() : List[Years] = Years.find.findList().asScala.toList

  /**
   *
   * @param sql
   * @param pList
   * @return
   */
  def allq(sql:RawSql, pList:Option[java.util.HashMap[String, AnyRef]]) : List[Years] = {
    val q = Years.find()
    if (!pList.isEmpty)
      for ((k:String,v:Object) <- pList.get) {
        q.setParameter(k, v)
      }
    q.setRawSql(sql)
    var resList : java.util.List[Years] = q.findList()
    if (resList.isEmpty)
      resList = new java.util.ArrayList[Years]
    resList.asScala.toList
  }

  /**
   *
   * @param year
   */
  def create(year: Integer): Unit = {
    var y = new Years
    y.year = year
    save(y)
  }
}