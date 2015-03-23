package models

import java.util
import java.util.Date
import javax.persistence._
import javax.validation.constraints.Pattern
import javax.validation.constraints.NotNull

import com.avaje.ebean.RawSql

import common.Dao
import play.data.validation.Constraints

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

/**
 * Created by hkatz on 3/21/15.
 */
@Entity
@Table (name = "uzer")
class Uzer {
  @Id
  var id:Long = 0l

  @Pattern(regexp = "[A-Za-z0-9 ]*", message = "must contain only letters, digits and spaces")
  @Constraints.MinLength(8)
  @Column
  var username:String  = null

  @NotNull
  @Column
  var password:String = null

  @NotNull
  @Column
  var role:String = null

  @NotNull
  var nodata:String = null

  @NotNull
  var joined_date:Date = null

  @OneToMany(targetEntity = classOf[Member], fetch = FetchType.LAZY, mappedBy = "uid")
  var Memberses : util.List[Member] = _

  @OneToMany(targetEntity = classOf[Transactions], fetch = FetchType.LAZY, mappedBy = "userid")
  var transactionses : util.List[Transactions] = _

  @OneToMany(targetEntity = classOf[Contact], fetch = FetchType.LAZY, mappedBy = "userid")
  var contactses : util.List[Contact] = _

  var activation: String = null

  var active_timestamp : Date = null

  var active : String  = null

  override def toString : String =  {
    var s = ""
    if (id != 0l && username != null)
      s = f"$id%d - $username"
    s
  }
}

object Uzer extends Dao(classOf[Uzer]) {
  def all() : List[Uzer] = Uzer.find.findList().asScala.toList

  def allq(sql:RawSql) : List[Uzer] = {
    val q = find()
    q.setRawSql(sql)
    q.findList().asScala.toList
  }

  def create(username:String, password:String, role: String, nodata: String,
              joined_Date:Date, activation:String, active_timestamp: Date, active: String): Unit = {
    var uz = new Uzer
    uz.username = username
    uz.password = password
    uz.role = role
    uz.nodata = nodata
    uz.joined_date = joined_Date
    uz.activation = activation
    uz.active_timestamp = active_timestamp
    uz.active = active
    save(uz)
  }

  def getColOrder : ArrayBuffer[String]  = {
    var colOrder = new ArrayBuffer[String]()
    colOrder += "id"
    colOrder += "username"
    colOrder += "password"
    colOrder += "role"
    colOrder += "nodata"
    colOrder += "joined_date"
    colOrder += "activation"
    colOrder += "active_timestamp"
    colOrder += "active"
    return colOrder
  }

  def getReqd : java.util.Map[String, Integer] = {
    var reqd = new java.util.HashMap[String, Integer]
    reqd.put("username", 1)
    reqd.put("password", 1)
    reqd.put("role", 1)
    reqd.put("joined_date", 1)
    reqd.put("active", 1)
    return reqd.asInstanceOf[java.util.Map[String, Integer]]
  }
   def apply(username:String, password:String, role: String, nodata: String,
             joined_Date:Date, activation:String, active_timestamp: Date, active: String) : Uzer = {
     var uz = new Uzer
     uz.username = username
     uz.password = password
     uz.role = role
     uz.nodata = nodata
     uz.joined_date = joined_Date
     uz.activation = activation
     uz.active_timestamp = active_timestamp
     uz.active = active
     return uz
   }

  }
