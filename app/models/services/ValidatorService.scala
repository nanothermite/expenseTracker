package models.services

import java.nio.charset.Charset
import javax.inject.Inject

import common.myTypes
import entities.{Member, MemberUser}
import models.SecTokens
import utils.Sha256

import scala.reflect.runtime.{universe => ru}

trait ValidatorService {
  def checkUserPwd(token: SecTokens): Option[MemberUser]

  def checkEmailPwd(token: SecTokens): Option[MemberUser]

  def checkSocial(token: SecTokens): Option[MemberUser]
}

class ValidatorServiceImpl @Inject()(daoSvc: DaoService) extends ValidatorService with myTypes with Sha256 {

  val m = ru.runtimeMirror(getClass.getClassLoader)

  val validateUserSql = "select u.password, u.id, m.fname " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "u.username = :username"
  val validateEmailSql = "select u.password, u.id, m.fname " +
    "from Member m, Uzer u " +
    "where m.uid = u.id and " +
    "m.email = :email"

  /**
    * username & pwd to authenticate
    * @param token username & pwd
    * @return
    */
  override def checkUserPwd(token: SecTokens): Option[MemberUser] = {
    val name = token.userid.get
    val passwd = token.passwd.get
    val colMap = Map("u.password" -> "password", "m.fname" -> "fname")
    val pList = Map("username" -> name)
    val pwdList = daoSvc.getList("allq", validateUserSql, colMap, pList, MemberUser)
    val valid =
      if (pwdList.nonEmpty) {
        val membUser = pwdList.head.asInstanceOf[MemberUser]
        val pwdHash = membUser.password
        if (toHexString(passwd, Charset.forName("UTF-8")) == pwdHash) Some(membUser) else None
      } else
        None
    valid
  }

  /**
    * use email & pwd to authenticate
    * @param token email and pwd
    * @return
    */
  override def checkEmailPwd(token: SecTokens): Option[MemberUser] = {
    val email = token.email.get
    val passwd = token.passwd.get
    val colMap = Map("u.password" -> "password", "m.fname" -> "fname")
    val pList = Map("email" -> email)
    val pwdList = daoSvc.getList("allq", validateEmailSql, colMap, pList, MemberUser)
    val valid =
      if (pwdList.nonEmpty) {
        val membUser = pwdList.head.asInstanceOf[MemberUser]
        val pwdHash = membUser.password
        if (toHexString(passwd, Charset.forName("UTF-8")) == pwdHash) Some(membUser) else None
      } else
        None
    valid
  }

  /**
    * only using email locate existing matching member and build MemberUser
    * @param token email and provider
    * @return
    */
  override def checkSocial(token: SecTokens): Option[MemberUser] = {
    val email = token.email.get
    val member = Member.find.where.eq("email", email).findUnique
    if (member != null) {
      val socialUser = member.uid
      Some(MemberUser.socialCreate(member.email, member.fname, member.lname, member.`type`, member.country,
        member.joined_date, member.ip, member.zip, socialUser.id, socialUser.password))
    } else
      None
  }
}