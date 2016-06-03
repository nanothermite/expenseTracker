package controllers

import java.nio.charset.Charset
import javax.inject.{Inject, Named, Singleton}

import actors.{CheckSession, EndSession}
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.mohiva.play.silhouette.api.{Environment, LogoutEvent, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import entities.{Member, MemberUser, Uzer}
import forms._
import models.services.ValidatorService
import models.{SecTokens, User}
import org.joda.time.DateTime
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.Action
import utils.Sha256

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * The basic application controller.
 *
 * @param messagesApi The Play messages API.
 * @param env The Silhouette environment.
 * @param socialProviderRegistry The social provider registry.
 */
@Singleton
class ApplicationController @Inject()(
  val messagesApi: MessagesApi,
  val env: Environment[User, CookieAuthenticator],
  socialProviderRegistry: SocialProviderRegistry,
  val validator: ValidatorService,
  @Named("sec-actor") secActor: ActorRef) extends Silhouette[User, CookieAuthenticator] with Sha256 {

  implicit val timeout: Timeout = 5.seconds

  /**
   * Handles the index action.
   *
   * @return The result to display.
   */
  def index = SecuredAction.async { implicit request =>
    Future.successful {
      val u = request.identity
      Logger.info(s"got user before secure: ${u.firstName}")

      // generate member, user based on social provider info (email) and memberuser
      val memberUser: MemberUser =
        if (u.email.isDefined) {
          val provider = request.session.get("provider").map { prov => prov
          }.getOrElse("facebook")
          val memberUserOpt = validator.checkSocial(SecTokens(None, None, u.email, Some(provider)))
          if (!memberUserOpt.isDefined) {
            val emailPattern = "(\\S+)@([\\S\\.]+)".r
            val emailPattern(name, domain) = u.email.get
            val joinedDate = (new DateTime()).toDate
            val role = "D"
            //TODO params & geoip to track
            val socialUser =
              Uzer.socialUser(s"$name${provider}".take(16), toHexString(ranStr(8), Charset.forName("UTF-8")), role,  joinedDate)
            val socialMember = Member.socialMember(u.email.get, name, provider, "D", "DEFAULT", joinedDate,
              request.remoteAddress, "XXXX", socialUser)
            val newMemberuser = MemberUser.socialCreate(socialMember.email, socialMember.fname, socialMember.lname,
              socialMember.`type`, socialMember.country, socialMember.joined_date, socialMember.ip, socialMember.zip,
              socialUser.id, socialUser.password)
            newMemberuser
          } else
            memberUserOpt.get
        } else
        MemberUser.socialCreate("", "", "", "", "", (new DateTime()).toDate, "", "", 0L, "")
      Ok(views.html.secure(memberUser))
    }
  }

  def landing = Action.async { implicit request =>
    Future.successful {
      Logger.info(s"unsecure landing insecure")
      Ok(views.html.insecure())
    }
  }

  /**
    * Handles the Sign In action.
    *
    * @return The result to display.
    */
  def pwdSignIn = UserAwareAction.async { request =>
    Logger.info("in pwd")
    //request.identity match {
    //  case Some(user) =>
    val sessKey =
      request.session.get("sessAuth").map { sessAuth =>
        sessAuth
      }
    (secActor ? CheckSession(sessKey.get)).mapTo[Int].map { uid: Int =>
      Logger.debug(s"got $uid")
      uid
      }

        Future.successful{
          Logger.info("got user pwd}")
          Redirect(routes.ApplicationController.index())
        }
      //case None => Future.successful {
      //  Logger.info(s"authenticating user")
      //  Ok(views.html.signIn(SignInForm.form, socialProviderRegistry))
      //}
    //}
  }

  /**
   * Handles the Sign In action.
   *
   * @return The result to display.
   */
  def signIn = UserAwareAction.async { implicit request =>
    Logger.info("in signin")
    request.identity match {
      case Some(user) =>
        Future.successful{
          Logger.info(s"got user: ${user.firstName}")
          Redirect(routes.ApplicationController.index())
        }
      case None => Future.successful {
        Logger.info(s"authenticating user")
        Ok(views.html.signIn(SignInForm.form, socialProviderRegistry))
      }
    }
  }

  /**
   * Handles the Sign Up action.
   *
   * @return The result to display.
   */
  def signUp = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => Future.successful(Redirect(routes.ApplicationController.index()))
      case None => Future.successful(Ok(views.html.insecure()))
    }
  }

  /**
   * Handles the Sign Out action.
   *
   * @return The result to display.
   */
  def signOut = SecuredAction.async { implicit request =>
    val result = Redirect(routes.ApplicationController.landing())
    env.eventBus.publish(LogoutEvent(request.identity, request, request2Messages))

    env.authenticatorService.discard(request.authenticator, result)
  }
}
