package models


import java.util
import java.util.Date

import com.avaje.ebean.RawSql
import common.Dao
import javax.persistence._
import javax.validation.constraints.Digits
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size
import scala.collection.JavaConverters._

/**
 * Created by hkatz on 3/21/15.
 */
object Member extends Dao(classOf[Member]) {
  def all() : List[Member] = Member.find.findList().asScala.toList

  def allq(sql:RawSql) : List[Member] = {
    val q = find()
    q.setRawSql(sql)
    q.findList().asScala.toList
  }

  def getColOrder: List[String] = List("id", "email", "fname", "lname", "phone_number", "type",
    "street1", "street2", "city", "state", "country", "joined_date", "ip", "zip", "userid", "uid")

  def getMetas: Map[String, Class[_]] = Map(
    "email" -> classOf[String],
    "fname" -> classOf[String],
    "lname" -> classOf[String],
    "phone_number" -> classOf[String],
    "type" -> classOf[String],
    "street1" -> classOf[String],
    "street2" -> classOf[String],
    "city" -> classOf[String],
    "state" -> classOf[String],
    "country" -> classOf[String],
    "joined_date" -> classOf[Date],
    "ip" -> classOf[String],
    "zip" -> classOf[String])

  def getReqd: Map[String, Integer] = Map("email" ->1, "fname" -> 1, "lname" -> 1, "type" -> 1, "country" -> 1, "ip" -> 1, "zip" -> 1)

  def create(id: Long,email: String,fname: String,lname: String,phone_number: String,`type`: String,
             street1: String,street2: String,city: String,state: String,country: String,joined_date: Date,
             ip: String,zip: String,userid: String,uid: Uzer): Unit = {
    val member = new Member
    member.id = id
    member.email = email
    member.fname = fname
    member.lname = lname
    member.phone_number = phone_number
    member.`type` = `type`
    member.street1 = street1
    member.street2 = street2
    member.city = city
    member.state = state
    member.country = country
    member.joined_date = joined_date
    member.ip = ip
    member.zip = zip
    member.userid = userid
    member.uid = uid
    save(member)
  }
}

@Entity
@Table(name = "member")
class Member {
  @Id
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

  @NotNull(message = "userid necessary")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "uid")
  var uid: Uzer = null

  override def toString : String =  {
    var s = ""
    if (id != 0l && email != null)
      s = f"$id%d - $email"
    s
  }
}
