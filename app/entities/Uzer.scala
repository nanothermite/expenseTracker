package entities

import java.util
import java.util.Date
import javax.persistence._
import javax.validation.constraints.{NotNull, Pattern}

import argonaut.Argonaut._
import argonaut._
import com.avaje.ebean.RawSql
import common.{BaseObject, Dao}
import org.joda.time.DateTime
import play.data.validation.Constraints
import utils.DateFormatter

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by hkatz on 3/21/15.
 */
@Entity
@Table(name = "uzer")
class Uzer extends BaseObject {
  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
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

  @OneToMany(targetEntity = classOf[Member], fetch = FetchType.LAZY, mappedBy = "uid")
  var Memberses: util.List[Member] = _

  @OneToMany(targetEntity = classOf[Transactions], fetch = FetchType.LAZY, mappedBy = "userid")
  var transactionses: util.List[Transactions] = _

  @OneToMany(targetEntity = classOf[Contact], fetch = FetchType.LAZY, mappedBy = "userid")
  var contactses: util.List[Contact] = _

  var activation: String = null

  var active_timestamp: Date = null

  var active: String = null

  override def toString: String = {
    var s = ""
    if (id != 0l && username != null)
      s = f"$id%d - $username"
    s
  }

  override def toJSON: Json =
  Json(
    "id" -> jNumber(id),
    "username" -> jString(username),
    "password" -> jString(password),
    "role" -> jString(role),
    "nodata" -> jsonNullCheck(nodata),
    "joined_date" -> jString(DateFormatter.formatDate(new DateTime(joined_date))),
    "activation" -> jsonNullCheck(activation),
    "active_timestamp" -> jsonNullCheck(DateFormatter.formatDate(new DateTime(active_timestamp))),
    "active" -> jsonNullCheck(active)
  )
}

object Uzer extends Dao(classOf[Uzer]) {
  def all: Option[List[Uzer]] = {
    val objList = Uzer.find.findList
    Some(if (objList.nonEmpty)
      objList.asScala.toList
    else
      List.empty[Uzer]
    )
  }

  def allq(sql: RawSql, pList:Option[java.util.HashMap[String, AnyRef]]): List[Uzer] = {
    var users: List[Uzer] = List.empty[Uzer]
    val q = Uzer.find
    if (pList.isDefined)
      for ((k:String,v:Object) <- pList.get) {
        q.setParameter(k, v)
      }
    q.setRawSql(sql)
    try {
      users = q.findList().asScala.toList
      users
    } catch {
      case e: Exception => users
    }
  }

  def create(username: String, password: String, role: String, nodata: String,
             joined_Date: Date, activation: String, active_timestamp: Date, active: String): Unit = {
    val uz = new Uzer
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

  def getColOrder: List[String] = List("id","username","password","role","nodata","joined_date",
    "activation","active_timestamp","active")

  def getReqd: Map[String, Integer] = Map(
    "username" -> 1,
    "password" -> 1,
    "role" -> 1,
    "joined_date" -> 1,
    "active" -> 1
  )

  def apply(username: String, password: String, role: String, nodata: Option[String],
            joined_Date: Date, activation: Option[String], active_timestamp: Option[Date], active: String): Uzer = {
    val uz = new Uzer
    uz.username = username
    uz.password = password
    uz.role = role
    if (nodata.isDefined)
      uz.nodata = nodata.get
    uz.joined_date = joined_Date
    if (activation.isDefined)
      uz.activation = activation.get
    if (active_timestamp.isDefined)
      uz.active_timestamp = active_timestamp.get
    uz.active = active
    uz
  }

  def apply2(username: Option[String], password: Option[String], role: Option[String], nodata: Option[String],
            joined_Date: Option[Date], activation: Option[String], active_timestamp: Option[Date], active: Option[String]): Uzer = {
    val uz = new Uzer
    if (username.isDefined)
      uz.username = username.get
    if (password.isDefined)
      uz.password = password.get
    if (role.isDefined)
      uz.role = role.get
    if (nodata.isDefined)
      uz.nodata = nodata.get
    if (joined_Date.isDefined)
      uz.joined_date = joined_Date.get
    if (activation.isDefined)
      uz.activation = activation.get
    if (active_timestamp.isDefined)
      uz.active_timestamp = active_timestamp.get
    if (active.isDefined)
      uz.active = active.get
    uz
  }
}
