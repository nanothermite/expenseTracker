import common.Shared
import play.api._
import play.api.http.HeaderNames
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Created by hkatz on 4/8/15.
 */
object Global extends WithFilters(CorsFilter) with GlobalSettings {
  override def beforeStart(app : Application): Unit = {
    Shared.genMemHandle()
  }

  override def onBadRequest(request: RequestHeader, error: String) = Future.successful(
    BadRequest(s"Bad Request: $error")
  )
}

object CorsFilter extends Filter {
  override def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] =
  nextFilter(requestHeader).map { result =>
      result.withHeaders(HeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN -> "*",
        HeaderNames.ALLOW -> "*",
        HeaderNames.ACCESS_CONTROL_ALLOW_METHODS -> "POST, GET, PUT, DELETE, OPTIONS",
        HeaderNames.ACCESS_CONTROL_ALLOW_HEADERS -> "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent"
      )
  }
}
