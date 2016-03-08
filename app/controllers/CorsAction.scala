package controllers

import play.api.http.HeaderNames
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by hkatz on 3/8/16.
 */
case class CorsAction[A](action: Action[A]) extends Action[A] {
  def apply(request: Request[A]): Future[Result] = {
		action(request).map(result => result.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*",
		HeaderNames.ALLOW -> "*",
		HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> "POST, GET, PUT, DELETE, OPTIONS",
		HeaderNames.ACCESS_CONTROL_ALLOW_HEADERS -> "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent"
		))
	}

  lazy val parser = action.parser
  //  override def parser: BodyParser[A] = ???
}
