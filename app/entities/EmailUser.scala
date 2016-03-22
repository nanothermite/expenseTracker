package entities

import java.util.Date
import javax.persistence._
import javax.validation.constraints.{NotNull, Pattern}
import play.api.libs.json._
import com.avaje.ebean.RawSql
import com.avaje.ebean.annotation.Sql
import common.{BaseObject, Dao}
import org.joda.time.DateTime
import play.data.validation.Constraints
import utils.DateFormatter
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by hkatz on 3/26/15.
 */
@Entity
@Sql
class EmailUser extends BaseObject {
  var id: Long = 0l

  @Pattern(regexp = "[A-Za-z0- ]*", message = "must contain only letters, digits and spaces")
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

  override def toJSON: JsValue = Json.obj(
      "id" -> id,
      "username" -> username,
      "password" -> password,
      "role" -> role,
      "joined_date" -> DateFormatter.formatDate(new DateTime(joined_date)),
      "activation" -> jsonNullCheck(activation),
      "active_timestamp" -> jsonNullCheck(DateFormatter.formatDate(new DateTime(active_timestamp))),
      "active" -> active
  )
}

object EmailUser extends Dao(classOf[EmailUser]){
  /**
   *
   * @return
   */
  def all() : List[EmailUser] = EmailUser.find.findList().asScala.toList

  /**
   *
   * @param sql query
   * @param pList params
   * @return
   */
  def allq(sql:RawSql, pList:Option[java.util.HashMap[String, AnyRef]]) : List[EmailUser] = {
    val q = EmailUser.find
    if (pList.isDefined)
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
   * @param id synth #
   * @param username user supplied
   * @param password pwd
   * @param role test or active
   * @param nodata  Y/N
   * @param joined_date date responded
   * @param activation  hex key
   * @param active_timestamp  date stamp
   * @param active  Y/N
   */
  def create(id: Long, username: String, password: String, role: String, nodata: String, joined_date: Date,
             activation: String, active_timestamp: Date, active: String): Unit = {
    val eu = new EmailUser
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

