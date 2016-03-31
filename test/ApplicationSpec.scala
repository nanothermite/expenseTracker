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

  val getUserUrl = "http://berne:8880/expense/get/User/9"

  case class Uzer(activation: String, active: String, active_timestamp: String, id: Int, joined_date: String,
                  nodata: String, password: String, role: String, username: String)

  "Application" should {

    /*"send 404 on a bad request" in new WithApplication{
      val testRet = FakeRequest(GET, "/boum", FakeHeaders() ,body = "", remoteAddress = "wengen:8880")
      route(testRet).get mustNotEqual beNone
    }*/

    "render the index page" in new WithApplication{
      implicit val personReads = Json.reads[Uzer]
      val wsRespF: Future[WSResponse] = WS.url(getUserUrl).get()
      val resultF: Future[JsResult[Uzer]] = wsRespF.map {
        response =>
            response.json.validate[Uzer]
          //(response.json \ "username").as[String]
      }
      val wsResp = Await.result(wsRespF.mapTo[WSResponse].map { resp => resp}, 5.milli)
      val uzerJS = Await.result(resultF, 700.milli)

      val (uzer, valid) = uzerJS match {
        case u: JsSuccess[Uzer] => (u.get, true)
        case e: JsError => (e.get, false)
      }

      val contentType = wsResp.header("content-type")

      if (contentType.isDefined)
        contentType.get.split(";")(0) must equalTo("application/json")

      if (valid)
        uzer.username must equalTo("johngalt")
      wsResp.status must equalTo(OK)

      //val home = route(FakeRequest(GET, "/expense/get/User/9", FakeHeaders(), body = "", remoteAddress = "wengen:8880")).get

      //contentType(home) must beSome.which(_ == "application/json")
      //contentAsString(home) must contain ("activation")
    }
  }
}
