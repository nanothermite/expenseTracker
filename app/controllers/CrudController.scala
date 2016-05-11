package controllers

import java.util.Date
import javax.inject.Inject

import _root_.common.BaseObject
import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import entities._
import models.User
import play.api.i18n.MessagesApi
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, JsValue, Reads, _}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global

class CrudController @Inject() (val messagesApi: MessagesApi,
                                val env: Environment[User, CookieAuthenticator],
                                socialProviderRegistry: SocialProviderRegistry) extends Silhouette[User, CookieAuthenticator]
  with SeqOps {

  /**
   * delete properties of existing object
    *
    * @param userid key
   * @return
   */
  def dropUser(userid: Long) = Action {
    val curResult = Uzer.find(userid)
    val stat =
      if (curResult.isDefined) {
        Uzer.delete(curResult.get)
        "OK"
      } else
        "NF"
    Ok(Json.obj("status" -> stat, "id" -> userid))
  }

  /**
   * delete properties of existing object
    *
    * @param memberid keys
   * @return
   */
  def dropMember(memberid: Long) = Action {
    val curResult = Member.find(memberid)
    val stat =
      if (curResult.isDefined) {
        Member.delete(curResult.get)
        "OK"
      } else
      "NF"
      Ok(Json.obj("status" -> stat, "id" -> memberid))
  }

  /**
   * delete properties of existing object
    *
    * @param contactid key
   * @return
   */
  def dropContact(contactid: Long) = Action {
    val curResult = Contact.find(contactid)
    val stat =
      if (curResult.isDefined) {
        Contact.delete(curResult.get)
        "OK"
      } else
        "NF"
    Ok(Json.obj("status" -> stat, "id" -> contactid))
  }

  /**
   * delete properties of existing object
    *
    * @param xactid key
   * @return
   */
  def dropTransaction(xactid: Long) = Action {
    val curResult = Transactions.find(xactid)
    val stat =
      if (curResult.isDefined) {
        Transactions.delete(curResult.get)
        "OK"
      } else
        "NF"
    Ok(Json.obj("status" -> stat, "id" -> xactid))
  }

  /**
   * not that nullable values become options
   */
  implicit val uzerReads: Reads[Uzer] = (
    (JsPath \ "username").read[String] and
      (JsPath \ "password").read[String] and
      (JsPath \ "role").read[String] and
      (JsPath \ "nodata").readNullable[String] and
      (JsPath \ "joined_date").read[Date] and
      (JsPath \ "activation").readNullable[String] and
      (JsPath \ "active_timestamp").readNullable[Date] and
      (JsPath \ "active").read[String]
    )(Uzer.apply _)

  /**
   * post new user object
    *
    * @return
   */
  def addUser() = Action(BodyParsers.parse.json) { request =>
    val uzerRes = request.body.validate[Uzer]
    uzerRes.fold(
      errors => {
        Ok(Json.obj("status" -> "KO")) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      },
      uz => {
        val uzer = uzerRes.get
        Uzer.save(uzer)
        Ok(Json.obj("status" -> "OK", "id" -> uzer.id))
      }
    )
  }

  /**
   * almost all nullable
   */
  implicit val contactReads: Reads[Contact] = (
      (JsPath \ "bizname").readNullable[String] and
      (JsPath \ "industry").readNullable[String] and
      (JsPath \ "phone").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "state").readNullable[String] and
      (JsPath \ "identifier").readNullable[String] and
      (JsPath \ "userid").readNullable[String]
    )(Contact.apply _)

  /**
   * post object as new contact, child of user
    *
    * @param userid key
   * @return async return
   */
  def addContact(userid: Long) = Action(BodyParsers.parse.json) { request =>
    //val contactJson = request.body.asJson
    val contactRes = request.body.validate[Contact]
    contactRes.fold(
      errors => {
        Ok(Json.obj("status" -> "KO")) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      },
      uz => {
        val contact = contactRes.get
        var parent = new Uzer
        val parentResult = Uzer.find(userid)
        val stat =
          if (parentResult.isDefined) {
            parent = parentResult.get
            contact.userid = parent
            Contact.save(contact)
            "OK"
          } else
            "NF"
        Ok(Json.obj("status" -> stat, "id" -> contact.id))
      }
    )
  }

  /**
   * member has a few reqd props
   */
  implicit val memberReads: Reads[Member] = (
    (JsPath \ "email").read[String] and
      (JsPath \ "fname").read[String] and
      (JsPath \ "lname").read[String] and
      (JsPath \ "phone_number").readNullable[String] and
      (JsPath \ "type").read[String] and
      (JsPath \ "street1").readNullable[String] and
      (JsPath \ "street2").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "state").readNullable[String] and
      (JsPath \ "country").read[String] and
      (JsPath \ "joined_date").read[Date] and
      (JsPath \ "ip").read[String] and
      (JsPath \ "zip").read[String] and
      (JsPath \ "userid").readNullable[String]
    )(Member.apply _)

  /**
   * post member object, also child of user
    *
    * @param userid key
   * @return
   */
  def addMember(userid: Long) = Action(BodyParsers.parse.json) { request =>
    val memberRes = request.body.validate[Member]
    memberRes.fold(
      errors => {
        Ok(Json.obj("status" -> "KO")) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      },
      uz => {
        val member = memberRes.get
        val parentResult = Uzer.find(userid)
        val stat =
          if (parentResult != null) {
            val parent = parentResult.get
            member.uid = parent
            Member.save(member)
            "OK"
          } else
            "NF"
        Ok(Json.obj("status" -> stat, "id" -> member.id))
      }
    )
  }

  /**
   * transaction is almost all nullable
   */
  implicit val transactionReads: Reads[Transactions] = (
    (JsPath \ "trandate").read[Date] and
      (JsPath \ "acct").readNullable[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "debit").readNullable[Double] and
      (JsPath \ "credit").readNullable[Double] and
      (JsPath \ "trantype").readNullable[String] and
      (JsPath \ "userid").read[Int]
    )(Transactions.apply _)

  /**
   * post a transaction object, child of user and contact
    *
    * @param userid    parent key
   * @param contactId child key
   * @return
   */
  def addTransaction(userid: Long, contactId: Long) = Action(BodyParsers.parse.json) { request =>
    val transRes = request.body.validate[Transactions]
    transRes.fold(
      errors => Ok(Json.obj("status" -> "KO")),
      uz => {
        val trans = transRes.get
        val parentResult = Uzer.find(userid)
        val contactResult = Contact.find(contactId)
        val stat =
          if (parentResult.isDefined && contactResult.isDefined) {
            val parent = parentResult.get
            val contact = contactResult.get
            trans.userid = parent
            trans.contact = contact
            Transactions.save(trans)
            "OK"
          } else
            "NF"
        Ok(Json.obj("status" -> stat, "id" -> trans.id))
      }
    )
  }

  /**
  * speed up by mem caching the crud value
 *
  * @param id synth
  * @param key pattern
    * @param valOpt optional
    * @return
    */
  def processGet(id: Long, key: String, valOpt: Option[BaseObject]): JsValue = {
    val jSon =
      if (valOpt.isDefined) {
        setSeq(key, valOpt.get.toJSON.toString())
        valOpt.get.toJSON
      } else
        Json.obj("badkey" -> id)
    jSon
  }

  /**
   * from crud operations - get User entity
    *
    * @param id synth
   * @return
   */
  def getUser(id: Long) = Action.async {
    val key = s"crud-user-$id"
    getSeq(key).map {
      case None =>
        if (id > 0)
          Ok(processGet(id, key, Uzer.find(id)))
        else {
          val objList = Uzer.all
          val json: JsValue =
            if (objList.get.nonEmpty) {
              val result = JsArray(objList.get.sortBy(_.id).map(_.toJSON))
              setSeq(key, result.toString())
              result
            } else {
              Json.obj("result" -> "none")
            }
          Ok(json)
        }
      case Some(i: String) =>
        Ok(Json.parse(i))
      case t: AnyRef => Ok("broke")
    }
  }

  /**
   * from crud operations - get Contact entity
    *
    * @param id synth
   * @return
   */
  def getContact(id: Long, userid: Long) = Action.async {
    val key = s"crud-contact-$id-$userid"
    getSeq(key).map {
      case None =>
        if (id > 0)
          Ok(processGet(id, key, Contact.find(id)))
        else {
          val objList = Contact.all(userid)
          val json: JsValue =
            if (objList.get.nonEmpty) {
              val result = JsArray(objList.get.sortBy(_.id).map(_.toJSON))
              setSeq(key, result.toString())
              result
            } else
              Json.obj("result" -> "none")
          Ok(json)
        }
      case Some(i: String) => Ok(Json.parse(i))
      case t: AnyRef => Ok("broke")
    }
  }

  /**
   * from crud operations - get Member entity
    *
    * @param id synth
   * @return
   */
  def getMember(id: Long, userid: Long) = Action.async {
    val key = s"crud-member-$id-$userid"
    getSeq(key).map {
      case None =>
        if (id > 0)
          Ok(processGet(id, key, Member.find(id)))
        else {
          val objList = Member.all(userid)
          val json: JsValue =
            if (objList.get.nonEmpty) {
              val result = JsArray(objList.get.sortBy(_.id).map(_.toJSON))
              setSeq(key, result.toString())
              result
            } else {
              Json.obj("result" -> "none")
            }
          Ok(json)
        }
      case Some(i: String) => Ok(Json.parse(i))
      case t: AnyRef => Ok("broke")
    }
  }

  /**
   * from crud operations - get Transactions entity
    *
    * @param id synth
   * @return
   */
  def getTransactions(id: Long, userid: Long) = Action.async {
    val key = s"crud-xaction-$id-$userid"
    getSeq(key).map {
      case None =>
        if (id > 0)
          Ok(processGet(id, key, Transactions.find(id)))
        else {
          val objList = Transactions.all(userid)
          val json: JsValue =
            if (objList.get.nonEmpty) {
              val result = JsArray(objList.get.sortBy(_.id).map(_.toJSON))
              setSeq(key, result.toString())
              result
            } else {
              Json.obj("result" -> "none")
            }
          Ok(json)
        }
      case Some(i: String) => Ok(Json.parse(i))
      case t: AnyRef => Ok("broke")
    }
  }

  /**
   * user is all nullable for updates
   */
  val uzerUpdateReads : Reads[Uzer] = (
    (JsPath \ "username").readNullable[String] and
      (JsPath \ "password").readNullable[String] and
      (JsPath \ "role").readNullable[String] and
      (JsPath \ "nodata").readNullable[String] and
      (JsPath \ "joined_date").readNullable[Date] and
      (JsPath \ "activation").readNullable[String] and
      (JsPath \ "active_timestamp").readNullable[Date] and
      (JsPath \ "active").readNullable[String]
    )(Uzer.apply2 _)

  /**
   * update user
    *
    * @param userid key
   * @return
   */
  def updateUser(userid: Long) = Action(BodyParsers.parse.json) { request =>
    val uzerRes = request.body.validate[Uzer](uzerUpdateReads)
    uzerRes.fold(
      errors => {
        Ok(Json.obj("status" -> "KO"))
      },
      uz => {
        val newuzer = uzerRes.get
        val curResult = Uzer.find(userid)
        if (curResult.isDefined) {
          val curuzer = curResult.get
          newuzer.id = userid
          Uzer.update(newuzer)
          val jsonVal = curuzer.toJSON
          setSeq(s"crud-user-$userid" ,jsonVal.toString())
          Ok(jsonVal)
        } else {
          Ok(Json.obj("status" -> "NF"))
        }
      }
    )
  }

  /**
   * update member nothing required
   */
  val memberUpdateReads: Reads[Member] = (
    (JsPath \ "email").readNullable[String] and
      (JsPath \ "fname").readNullable[String] and
      (JsPath \ "lname").readNullable[String] and
      (JsPath \ "phone_number").readNullable[String] and
      (JsPath \ "type").readNullable[String] and
      (JsPath \ "street1").readNullable[String] and
      (JsPath \ "street2").readNullable[String] and
      (JsPath \ "city").readNullable[String] and
      (JsPath \ "state").readNullable[String] and
      (JsPath \ "country").readNullable[String] and
      (JsPath \ "joined_date").readNullable[Date] and
      (JsPath \ "ip").readNullable[String] and
      (JsPath \ "zip").readNullable[String] and
      (JsPath \ "userid").readNullable[String]
    )(Member.apply2 _)

  /**
   * update member
    *
    * @param id key
   * @return
   */
  def updateMember(id: Long, userid: Long) = Action(BodyParsers.parse.json) { request =>
    val jsonReq = request.body
    val validEnt = jsonReq.validate[Member](memberUpdateReads)
    validEnt.fold(
      errors =>
        Ok(Json.obj("status" -> "KO")), //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      uz => {
        val curResult = Member.find(id)
        if (curResult.isDefined) {
          val curJson = curResult.get.toJSON.as[JsObject]
          val updatedEnt = curJson.deepMerge(jsonReq.as[JsObject]).validate[Member](memberUpdateReads)
          if (updatedEnt.isSuccess) {
            updatedEnt.get.id = id
            Member.update(updatedEnt.get)
            delSeq(s"crud-member-0-$userid")
            delSeq(s"crud-member-$id-$userid")
            Ok(Json.obj("status" -> "OK", "id" -> id))
          } else
            Ok(Json.obj("status" -> "KO"))
        } else
          Ok(Json.obj("status" -> "NF"))
      }
    )
  }

  /**
   * update contact object
    *
    * @param id key
   * @return
   */
  def updateContact(id: Long, userid: Long) = Action(BodyParsers.parse.json) { request =>
    val jsonReq = request.body
    val validEnt = jsonReq.validate[Contact]
    validEnt.fold(
      errors =>
        Ok(Json.obj("status" -> "KO")), //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      uz => {
        val curResult = Contact.find(id)
        if (curResult.isDefined) {
          val curJson = curResult.get.toJSON.as[JsObject]
          val updatedEnt = curJson.deepMerge(jsonReq.as[JsObject]).validate[Contact]
          if (updatedEnt.isSuccess) {
            updatedEnt.get.id = id
            Contact.update(updatedEnt.get)
            delSeq(s"crud-contact-0-$userid")
            delSeq(s"crud-contact-$id-$userid")
            Ok(Json.obj("status" -> "OK", "id" -> id))
          } else
            Ok(Json.obj("status" -> "KO"))
        } else
          Ok(Json.obj("status" -> "NF"))
      }
    )
  }

  /**
   * update transaction - nothing reqd
   */
  val transactionUpdateReads: Reads[Transactions] = (
    (JsPath \ "trandate").readNullable[Date] and
      (JsPath \ "acct").readNullable[String] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "debit").readNullable[Double] and
      (JsPath \ "credit").readNullable[Double] and
      (JsPath \ "trantype").readNullable[String] and
      (JsPath \ "userid").readNullable[String]
    )(Transactions.apply2 _)

  /**
   * update transaction object
    *
    * @param id key
   * @return
   */
  def updateTransaction(id: Long, userid: Long): Action[JsValue] = Action(BodyParsers.parse.json) { request =>
    val jsonReq = request.body
    val validEnt = request.body.validate[Transactions](transactionUpdateReads)
    validEnt.fold(
      errors =>
        Ok(Json.obj("status" -> "KO")), //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      uz => {
        val curResult = Transactions.find(id)
        if (curResult.isDefined) {
          val curJson = curResult.get.toJSON.as[JsObject]
          val updatedEnt = curJson.deepMerge(jsonReq.as[JsObject]).validate[Transactions]
          if (updatedEnt.isSuccess) {
            updatedEnt.get.id = id
            Transactions.update(updatedEnt.get)
            delSeq(s"crud-xaction-0-$userid")
            delSeq(s"crud-xaction-$id-$userid")
            Ok(Json.obj("status" -> "OK", "id" -> id))
          } else
            Ok(Json.obj("status" -> "KO"))
        } else
          Ok(Json.obj("status" -> "NF")) //, "message" -> play.api.libs.json.JsError.toFlatJson(errors)))
      }
    )
  }
}
