package controllers

import javax.inject.Inject

import actors._
import akka.actor.{ActorSystem, _}
import akka.pattern.ask
import akka.util.Timeout
import entities.MemberUser
import models.SecTokens
import models.services.ValidatorService
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import utils.Sha256

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ValidationController @Inject()(system: ActorSystem, validator: ValidatorService) extends Controller with Sha256 {
  val secActor = system.actorOf(SecurityActor.props, "sec-actor")

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

  private def coreValidate(mu: Option[MemberUser], request: Request[AnyContent]): JsObject =
    if (mu.isDefined) {
      val curSessAuth =
        request.session.get("sessAuth").map { sessAuth =>
          Some(sessAuth)
        }.getOrElse(
          Some(ranStr(50))
        )

      secActor ! InitSession(curSessAuth.get, mu.get.id.toInt)
      Json.obj("access" -> "auth", "uid" -> mu.get.uid, "welcome" -> mu.get.fname,
        "sessAuth" -> curSessAuth.get)
    } else
      Json.obj("access" -> "denied")

  def validateSocial(email: String, provider: String) = Action { request =>
    val memberUserOpt =
      validator.checkSocial(SecTokens(None, None, Some(email), Some(provider)))
    Ok(coreValidate(memberUserOpt, request))
  }

  def validateUser(name: String, passwd: String) = Action { request =>
    val isEmail = name.contains("@")
    val memberUserOpt =
      if (isEmail)
        validator.checkEmailPwd(SecTokens(None, Some(passwd), Some(name), None))
      else
        validator.checkUserPwd(SecTokens(Some(name), Some(passwd), None, None))
      Ok(coreValidate(memberUserOpt, request))
  }
}