package models

import java.util
import javax.persistence._

import com.avaje.ebean.RawSql
import common.Dao

import scala.collection.JavaConverters._

/**
 * Created by hkatz on 3/21/15.
 */
@Entity
@Table (name = "contact")
class Contact {
  @Id
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
}

object Contact extends Dao(classOf[Contact]) {
  def all() : List[Contact] = Contact.find.findList().asScala.toList

  def allq(sql:RawSql) : List[Contact] = {
    val q = find()
    q.setRawSql(sql)
    q.findList().asScala.toList
  }

  def create(id:Long, version:Integer, bizname:String, industry:String, phone:String, city:String, state:String, identifier:String, userid:Uzer): Unit = {
    var contact = new Contact
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

  def getReqd : Map[String, Integer] = Map("username" -> 1, "password" ->1, "role" -> 1, "joined_date" -> 1, "active" -> 1)
}
