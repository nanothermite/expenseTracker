package entities

import java.util.Date
import javax.persistence._
import javax.validation.constraints.{Digits, NotNull, Pattern, Size}
import play.api.libs.json._
import com.avaje.ebean.RawSql
import common.{BaseObject, Dao}
import org.joda.time.DateTime
import utils.DateFormatter
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by hkatz on 3/21/15.
 */
object Member extends Dao(classOf[Member]) {
  def all: Option[List[Member]] =  {
    val objList = Member.find.findList
    Some(if (objList.nonEmpty)
      objList.asScala.toList
    else
      List.empty[Member]
    )
  }

  def allq(sql:RawSql) : List[Member] = {
    val q = find
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

  def apply(email: String,fname: String,lname: String,phone_number: Option[String],`type`: String,
            street1: Option[String],street2: Option[String],city: Option[String],state: Option[String],country: String,joined_date: Date,
            ip: String,zip: String, userid: Option[String]): Member = {
    val member = new Member
    member.email = email
    member.fname = fname
    member.lname = lname
    if (phone_number.isDefined)
      member.phone_number = phone_number.get
    member.`type` = `type`
    if (street1.isDefined)
      member.street1 = street1.get
    if (street2.isDefined)
      member.street2 = street2.get
    if (city.isDefined)
      member.city = city.get
    if (state.isDefined)
      member.state = state.get
    member.country = country
    member.joined_date =joined_date
    member.ip = ip
    member.zip = zip
    if (userid.isDefined)
      member.userid = userid.get
    member
  }

  def apply2(email: Option[String],fname: Option[String],lname: Option[String],phone_number: Option[String],`type`: Option[String],
            street1: Option[String],street2: Option[String],city: Option[String],state: Option[String],country: Option[String],joined_date: Option[Date],
            ip: Option[String],zip: Option[String],userid: Option[String]): Member = {
    val member = new Member
    if (email.isDefined)
      member.email = email.get
    if (fname.isDefined)
      member.fname = fname.get
    if (lname.isDefined)
      member.lname = lname.get
    if (phone_number.isDefined)
      member.phone_number = phone_number.get
    if (`type`.isDefined)
      member.`type` = `type`.get
    if (street1.isDefined)
      member.street1 = street1.get
    if (street2.isDefined)
      member.street2 = street2.get
    if (city.isDefined)
      member.city = city.get
    if (state.isDefined)
      member.state = state.get
    if (country.isDefined)
      member.country = country.get
    if (joined_date.isDefined)
      member.joined_date =joined_date.get
    if (ip.isDefined)
      member.ip = ip.get
    if (zip.isDefined)
      member.zip = zip.get
    if (userid.isDefined)
      member.userid = userid.get
    member
  }
}

@Entity
@Table(name = "member")
class Member extends BaseObject {
  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
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

  override def toJSON =
    Json.obj(
      "id" -> id,
      "email" -> email,
      "fname" -> fname,
      "lname" -> lname,
      "userid" -> jsonNullCheck(userid),
      "type" -> `type`,
      "street1" -> jsonNullCheck(street1),
      "street2" -> jsonNullCheck(street2),
      "city" -> jsonNullCheck(city),
      "state" -> jsonNullCheck(state),
      "ip" -> jsonNullCheck(ip),
      "zip" -> jsonNullCheck(zip),
      "phone_number" -> jsonNullCheck(phone_number),
      "country" -> jsonNullCheck(country),
      "joined_date" -> jsonNullCheck(DateFormatter.formatDate(new DateTime(joined_date)))
    )
}
