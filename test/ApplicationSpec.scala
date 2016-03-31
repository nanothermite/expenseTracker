import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json._
import play.api.test._
import play.api.libs.ws._
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  val getSingleUserUrl = "http://berne:8880/expense/get/User/9"
  val validateUserUrl = "http://berne:8880/expense/valid/User/johngalt/12345678"
  val waitTime = 700.milli
  implicit val uzerReads = Json.reads[Uzer]
  implicit val authReads = Json.reads[Access]

  case class Uzer(activation: String, active: String, active_timestamp: String, id: Int, joined_date: String,
                  nodata: String, password: String, role: String, username: String)

  case class Access(access: String, sessAuth: String, uid: Int)

  "Application" should {

    "obtain single user record" in new WithApplication{
      val wsRespF: Future[WSResponse] = WS.url(getSingleUserUrl).get()
      val resultF: Future[JsResult[Uzer]] = wsRespF.map {
        response =>
            response.json.validate[Uzer]
      }
      val wsResp = Await.result(wsRespF.mapTo[WSResponse].map { resp => resp}, waitTime)
      val uzerJS = Await.result(resultF, waitTime)

      uzerJS match {
        case u: JsSuccess[Uzer] => u.get.username must equalTo("johngalt")
        case e: JsError => "bad return" must equalTo("johngalt")
      }
      val contentType = wsResp.header("content-type")
      if (contentType.isDefined)
        contentType.get.split(";").head must equalTo("application/json")
      wsResp.status must equalTo(OK)
    }

    "validate user access" in new WithApplication{
      val wsRespF: Future[WSResponse] = WS.url(validateUserUrl).get()
      val resultF: Future[JsResult[Access]] = wsRespF.map {
        response =>
          response.json.validate[Access]
      }
      val wsResp = Await.result(wsRespF.mapTo[WSResponse].map { resp => resp}, waitTime)
      val authJS = Await.result(resultF, waitTime)

      authJS match {
        case u: JsSuccess[Access] => u.get.access must equalTo("auth")
        case e: JsError => "bad return" must equalTo("auth")
      }
      val contentType = wsResp.header("content-type")
      if (contentType.isDefined)
        contentType.get.split(";").head must equalTo("application/json")
      wsResp.status must equalTo(OK)
    }
  }
}
