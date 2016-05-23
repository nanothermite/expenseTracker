package entities

import java.util.Date
import javax.persistence._
import javax.validation.constraints.{Digits, NotNull, Pattern, Size}

import com.avaje.ebean.RawSql
import com.avaje.ebean.annotation.Sql
import common.Dao

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.{mutable => mu, immutable => im}

/**
 * Created by hkatz on 3/26/15.
 */
@Entity
@Sql
class MemberUser {
  var id: Long = 0l

  var email: String = null

  @Pattern(regexp = "[A-Za-z]*", message = "must contain only letters")
  var fname: String = null

  @Pattern(regexp = "[A-Za-z]*", message = "must contain only letters")
  var lname: String = null

  var phone_number: String = null

  var `type`: String = null

  @Pattern(regexp = "[A-Za-z0-9\\- ]*", message = "must contain only letters, digits and spaces")
  var street1: String = null

  @Pattern(regexp = "[A-Za-z0-9\\- ]*", message = "must contain only letters, digits and spaces")
  var street2: String = null

  @Pattern(regexp = "[A-Za-z0-9\\- ]*", message = "must contain only letters, digits and spaces")
  var city: String = null

  @Pattern(regexp = "[A-Z0-9\\- ]*", message = "must contain only letters, digits and spaces")
  var state: String = null

  @Pattern(regexp = "[A-Z0-9\\.\\- ]*", message = "must contain only letters, digits and spaces")
  var country: String = null

  @NotNull(message = "join date necessary")
  var joined_date: Date = null

  var ip: String = null

  @Size(min = 4, max = 10, message = "must be between 4 and 10 digits only")
  @Digits(fraction = 0, integer = 10, message = "must be between 4 and 10 digits only")
  var zip: String = null

  @Size(min = 8, max = 16, message = "must be between 8 and 16 digits only")
  @Pattern(regexp = "[A-Za-z0-9]*", message = "must contain only letters, digits")
  var userid: String = null

  var password: String = null

  var uid: Long = 0l
}

object MemberUser extends Dao(classOf[MemberUser]){
  /**
   *
   * @return
   */
  def all(): List[MemberUser] = MemberUser.find.findList().asScala.toList

  /**
   *
   * @param sql query
   * @param pList params
   * @return
   */
  def allq(sql:RawSql, pList:Option[im.Map[String, AnyRef]]) : List[MemberUser] = {
    val q = MemberUser.find
    if (pList.isDefined)
      for ((k:String,v:Object) <- pList.get) {
        q.setParameter(k, v)
      }
    q.setRawSql(sql)
    var resList : java.util.List[MemberUser] = q.findList()
    if (resList.isEmpty)
      resList = new java.util.ArrayList[MemberUser]
    resList.asScala.toList
  }

  /**
   *
   * @param id id
   * @param email email
   * @param fname first name
   * @param lname last name
   * @param phone_number tel
   * @param `type` my type
   * @param street1 address
   * @param street2 address
   * @param city city
   * @param state st
   * @param country country
   * @param joined_date date
   * @param ip x.x.x.x
   * @param zip  99999
   * @param userid name
   * @param uid number
   */
  def create(id: Long, email: String, fname: String, lname: String, phone_number: String, `type`: String,
             street1: String, street2: String, city: String, state: String, country: String, joined_date: Date,
             ip: String, zip: String, userid: String, uid: Long, password: String): Unit = {
    var mu = new MemberUser
    mu.id = id
    mu.email = email
    mu.fname = fname
    mu.lname = lname
    mu.phone_number = phone_number
    mu.`type` = `type`
    mu.street1 = street1
    mu.street2 = street2
    mu.city = city
    mu.state = state
    mu.country = country
    mu.joined_date = joined_date
    mu.ip = ip
    mu.zip = zip
    mu.userid = userid
    mu.uid = uid
    mu.password = password
    save(mu)
  }

  def getColOrder: List[String] = List("id", "email", "fname", "lname", "phone_number", "type",
    "street1", "street2", "city", "state", "country", "joined_date", "ip", "zip", "userid", "uid", "password")

}

