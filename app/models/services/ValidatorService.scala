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

  override def checkSocial(token: SecTokens): Option[MemberUser] = {
    val email = token.email.get
    val memberMatched = Member.find.where.eq("email", email).findUnique
    if (memberMatched != null) {
      //val emailPattern = "(\\S+)@([\\S\\.]+)".r
      //val emailPattern(name, domain) = email
      val member = memberMatched
      val socialUser = member.uid
/*      val socialUser =
        if (userMatches)
          userMatches.get
        else
          Uzer.socialUser(s"$name${token.provider.get}",
            toHexString(ranStr(8), Charset.forName("UTF-8")),
            "D", (new DateTime()).toDate) */
      Some(MemberUser.socialCreate(member.email, member.fname, member.lname, member.`type`, member.country,
        member.joined_date, member.ip, member.zip, socialUser.id, socialUser.password))
    } else
      None
  }
}