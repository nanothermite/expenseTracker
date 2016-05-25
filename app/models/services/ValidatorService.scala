package models.services

import java.nio.charset.Charset

import common.myTypes
import com.avaje.ebean._
import entities.{Member, MemberUser}
import models.SecTokens
import utils.Sha256

import scala.collection.{immutable => im, mutable => mu}
import scala.reflect.runtime.universe._
import scala.reflect.runtime.{universe => ru}

trait ValidatorService {
  def checkUserPwd(token: SecTokens): Option[MemberUser]

  def checkEmailPwd(token: SecTokens): Option[MemberUser]

  def checkSocial(token: SecTokens): Option[Member]
}

class ValidatorServiceImpl /*@Inject()(cache: CacheApi)*/ extends ValidatorService with myTypes with Sha256 {

  val m = ru.runtimeMirror(getClass.getClassLoader)

  val validateUserSql = "select u.password, u.id, m.fname " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "u.username = :username"
  val validateEmailSql = "select u.password, u.id, m.fname " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "m.email = :email"
  val validateSocialSql = "select m.fname " +
    "from Member m " +
    "where m.email = :email"

  /**
    * reflection on Object
    *
    * @param name method
    * @param m    runtime
    * @param tru  reflection
    * @tparam T   internals
    * @return
    */
  def methodByReflectionO[T : TypeTag](name: String, m: ru.Mirror, tru: Type): MethodMirror = {
    val modX = tru.termSymbol.asModule
    val methodX = tru.decl(ru.TermName(name)).asMethod
    val mm = m.reflectModule(modX)
    val im = m.reflect(mm.instance)
    im.reflectMethod(methodX)
  }

  def genSql(query: String, colMap: im.Map[String, String]): RawSql = {
    val rawSqlBld = RawSqlBuilder.parse(query)
    for ((k, v) <- colMap) {
      rawSqlBld.columnMapping(k, v)
    }
    rawSqlBld.create()
  }

  /**
    * generate collection of T objects using getter
    *
    * @param getter method to invoke
    * @param query  actual
    * @param colMap for ebean
    * @param pList  parameters to query
    * @param t      for reflection
    * @tparam T     reflection on return type
    * @return
    */
  def getList[T : TypeTag](getter: String, query: String, colMap: im.Map[String, String],
                           pList: im.Map[String, AnyRef], t: T): List[T] = {
    val rawSql = genSql(query, colMap)
    val myType = getType(t)
    val obj = methodByReflectionO[T](getter, m, myType)
    obj(rawSql, Some(pList)).asInstanceOf[List[T]]
  }

  override def checkUserPwd(token: SecTokens): Option[MemberUser] = {
    val name = token.userid.get
    val passwd = token.passwd.get
    val colMap = Map("u.password" -> "password", "m.fname" -> "fname")
    val pList = Map("username" -> name)
    val pwdList = getList("allq", validateUserSql, colMap, pList, MemberUser)
    val valid =
      if (pwdList.nonEmpty) {
        val membUser = pwdList.head.asInstanceOf[MemberUser]
        val pwdHash = membUser.password
        if (toHexString(passwd, Charset.forName("UTF-8")) == pwdHash) Some(membUser) else None
      } else
        None
    valid
  }

  override def checkEmailPwd(token: SecTokens): Option[MemberUser] = {
    val email = token.email.get
    val passwd = token.passwd.get
    val colMap = Map("u.password" -> "password", "m.fname" -> "fname")
    val pList = Map("email" -> email)
    val pwdList = getList("allq", validateEmailSql, colMap, pList, MemberUser)
    val valid =
      if (pwdList.nonEmpty) {
        val membUser = pwdList.head.asInstanceOf[MemberUser]
        val pwdHash = membUser.password
        if (toHexString(passwd, Charset.forName("UTF-8")) == pwdHash) Some(membUser) else None
      } else
        None
    valid
  }

  override def checkSocial(token: SecTokens): Option[Member] = {
    val email = token.email.get
    val colMap = Map("m.fname" -> "fname")
    val pList = Map("email" -> email)
    val pwdList = getList("allq", validateSocialSql, colMap, pList, Member)
    if (pwdList.nonEmpty) Some(pwdList.head.asInstanceOf[Member]) else None
  }

}
