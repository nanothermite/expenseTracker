package controllers

import javax.inject.Inject

import _root_.common.Shared
import actors._
import akka.actor.{ActorSystem, _}
import akka.pattern.ask
import akka.util.Timeout
import models.SecTokens
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import svcs.ValidatorService
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ValidationController @Inject()(system: ActorSystem, validator: ValidatorService) extends Controller {
  val secActor = system.actorOf(SecurityActor.props, "sec-actor")

  val r = Shared.r

  implicit val timeout: Timeout = 5.seconds
  val waitTime = 700.milli

  def invalidateUser(sessKey: String, checkid: Int) = Action {
    val endedFuture =
      (secActor ? CheckSession(sessKey)).mapTo[Int].map { uid: Int =>
        Logger.debug(s"got $uid")
        if (checkid == uid) {
          secActor ! EndSession(sessKey)
          true
        } else
          false
    }
    val ended = Await.result(endedFuture, waitTime)
    Ok(Json.obj("status" ->
      (if (ended) "loggedout" else "mismatched")))
  }

  def dumpMap = Action {
    val dumpFuture =
      (secActor ? DumpSessions).mapTo[Map[String, Int]].map { sessMap: Map[String, Int] =>
        Logger.debug(s"got ${sessMap.size} recs")
        sessMap
      }
    val sessMap: Map[String, Int] = Await.result(dumpFuture, waitTime)
    val sessJsonObj = {
      for {
        sessKey <- sessMap.keys
      } yield sessKey -> JsNumber(sessMap.get(sessKey).get)
    }.toMap
    Ok(JsObject(sessJsonObj))
  }

  def ranSession = (1 to 50).map(_ => r.nextPrintableChar()).mkString("")

  def validateSocial(email: String) = Action {
    val memberOpt =
      validator.checkSocial(SecTokens(None, None, Some(email)))

    Ok(if (memberOpt.isDefined) {
      secActor ! InitSession(ranSession, memberOpt.get.id.toInt)
      Json.obj("access" -> "auth", "uid" -> memberOpt.get.id, "welcome" -> memberOpt.get.fname, "sessAuth" -> ranSession)
    } else
      Json.obj("access" -> "denied")
    )
  }

  def validateUser(name: String, passwd: String) = Action {
    val isEmail = name.contains("@")
    val memberUserOpt =
      if (isEmail)
        validator.checkEmailPwd(SecTokens(None, Some(passwd), Some(name)))
      else
        validator.checkUserPwd(SecTokens(Some(name), Some(passwd), None))

    Ok(if (memberUserOpt.isDefined) {
      secActor ! InitSession(ranSession, memberUserOpt.get.id.toInt)
      Json.obj("access" -> "auth", "uid" -> memberUserOpt.get.id, "welcome" -> memberUserOpt.get.fname, "sessAuth" -> ranSession)
    } else
      Json.obj("access" -> "denied")
    )
  }
}