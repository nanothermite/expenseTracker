package entities

import java.util
import javax.persistence._
import play.api.libs.json._
import com.avaje.ebean.RawSql
import common.{BaseObject, Dao}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by hkatz on 3/21/15.
 */
@Entity
@Table (name = "contact")
class Contact extends BaseObject {
  @Id
  @GeneratedValue(strategy=GenerationType.AUTO)
  var id:Long = 0l

  @Column
  var version: Integer = null

  @Column
  var bizname: String = null

  @Column
  var industry: String = null

  @Column
  var phone: String = null

  @Column
  var city: String = null

  @Column
  var state: String = null

  @Column
  var identifier: String = null

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "userid" )
  @Column(name = "userid")
  var userid:Uzer = _

  @OneToMany(targetEntity = classOf[Transactions], fetch = FetchType.LAZY, mappedBy = "userid")
  var transactionsSet: util.List[Transactions] = _

  override def toString : String =  {
    var s = ""
    if (id != 0l && bizname != null)
      s = f"$id%d - $bizname"
    s
  }

  def toJSON = Json.obj(
    "id" -> id,
    "version" -> (if (version == null) "" else version.toString),
    "bizname" -> jsonNullCheck(bizname),
    "industry" -> jsonNullCheck(industry),
    "phone" -> jsonNullCheck(phone),
    "city" -> jsonNullCheck(city),
    "state" -> jsonNullCheck(state),
    "identifier" -> jsonNullCheck(identifier)
  )
}

object Contact extends Dao(classOf[Contact]) {
  def all(userid: Long): Option[List[Contact]] = {
    val uzer = Uzer.find(userid.toInt)
    val objList =
      if (uzer.isDefined) {
        val objList = Contact.find.where.eq("userid", uzer.get).findList
        if (objList.nonEmpty)
          objList.asScala.toList
        else
          List.empty[Contact]
      }
      else
        List.empty[Contact]
    Some(objList)
  }

  def allq(sql:RawSql) : List[Contact] = {
    val q = find
    q.setRawSql(sql)
    q.findList().asScala.toList
  }

  def create(id:Long, version:Integer, bizname:String, industry:String, phone:String, city:String, state:String, identifier:String, userid:Uzer): Unit = {
    val contact = new Contact
    contact.id = id
    contact.version = version
    contact.bizname = bizname
    contact.industry = industry
    contact.phone = phone
    contact.city = city
    contact.state = state
    contact.identifier = identifier
    contact.userid = userid
    save(contact)
  }

  def getColOrder : List[String]  =
    List("bizname","industry","phone","city","state","identifier","userid")

  def getReqd : Map[String, Integer] = Map()

  def empty = apply(None, None, None, None, None, None, None)

  def apply(bizname:Option[String], industry:Option[String], phone:Option[String],
            city:Option[String], state:Option[String], identifier:Option[String], userid: Option[String]): Contact = {
    val contact = new Contact
    //contact.version = version
    if (bizname.isDefined)
      contact.bizname = bizname.get
    if (industry.isDefined)
      contact.industry = industry.get
    if (phone.isDefined)
      contact.phone = phone.get
    if (city.isDefined)
      contact.city = city.get
    if (state.isDefined)
      contact.state = state.get
    if (identifier.isDefined)
      contact.identifier = identifier.get
    if (userid.isDefined) {
      val uzerIdOpt = Uzer.find(userid.get.toInt)
      if (uzerIdOpt.isDefined) contact.userid = uzerIdOpt.get
    }
    contact
  }

  def findBiz(biz: String, userid: Int): List[Contact] = {
    val uzer = Uzer.find(userid.toInt)
    if (uzer.isDefined) {
      val bizList = find.where.
        eq("userid", uzer.get).
        eq("bizname", biz).findList.asScala.toList
      bizList
    } else
      List.empty[Contact]
  }
}
