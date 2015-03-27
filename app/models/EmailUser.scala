package models

import java.util.Date
import javax.persistence._
import javax.validation.constraints.{NotNull, Pattern}

import com.avaje.ebean.RawSql
import com.avaje.ebean.annotation.Sql
import common.Dao
import play.data.validation.Constraints

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by hkatz on 3/26/15.
 */
@Entity
@Sql
class EmailUser {
  var id: Long = 0l

  @Pattern(regexp = "[A-Za-z0-9 ]*", message = "must contain only letters, digits and spaces")
  @Constraints.MinLength(8)
  @Column
  var username: String = null

  @NotNull
  @Column
  var password: String = null

  @NotNull
  @Column
  var role: String = null

  @NotNull
  var nodata: String = null

  @NotNull
  var joined_date: Date = null

  var activation: String = null

  var active_timestamp: Date = null

  var active: String = null
}

object EmailUser extends Dao(classOf[EmailUser]){
  /**
   *
   * @return
   */
  def all() : List[EmailUser] = EmailUser.find.findList().asScala.toList

  /**
   *
   * @param sql
   * @param pList
   * @return
   */
  def allq(sql:RawSql, pList:Option[java.util.HashMap[String, AnyRef]]) : List[EmailUser] = {
    val q = EmailUser.find()
    if (!pList.isEmpty)
      for ((k:String,v:Object) <- pList.get) {
        q.setParameter(k, v)
      }
    q.setRawSql(sql)
    var resList : java.util.List[EmailUser] = q.findList()
    if (resList.isEmpty)
      resList = new java.util.ArrayList[EmailUser]
    resList.asScala.toList
  }

  /**
   *
   * @param id
   * @param username
   * @param password
   * @param role
   * @param nodata
   * @param joined_date
   * @param activation
   * @param active_timestamp
   * @param active
   */
  def create(id: Long, username: String, password: String, role: String, nodata: String, joined_date: Date,
             activation: String, active_timestamp: Date, active: String): Unit = {
    var eu = new EmailUser
    eu.id = id
    eu.username = username
    eu.password = password
    eu.role = role
    eu.nodata = nodata
    eu.joined_date = joined_date
    eu.activation = activation
    eu.active_timestamp = active_timestamp
    save(eu)
  }
}

