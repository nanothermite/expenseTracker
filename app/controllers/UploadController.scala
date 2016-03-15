package controllers

import _root_.common.{ExtraJsonHelpers, Shared, myTypes}
import argonaut.Argonaut._
import argonaut._
import play.api.mvc._
import shade.memcached.Memcached

import scala.concurrent.duration._
import scala.reflect.runtime.{universe => ru}
import scala.util.Random

object UploadController extends Controller with myTypes with ExtraJsonHelpers {

  val m = ru.runtimeMirror(getClass.getClassLoader)

  var r = Random

  var colOrder = Seq.empty[String]
  var colMap = scala.collection.mutable.Map[String, String]()
  var pList = new java.util.HashMap[String, Object]()
  val datePattern = "yyyy-MM-dd"

  val byMonthsql = "select sum(s.credit) as credit, " +
    "sum(s.debit) as debit, " +
    "cast(extract(month from s.trandate) as bigint) as period, " +
    " 'N' as periodType " +
    "from Transactions s " +
    "where extract(year from s.trandate) = :year and " +
    "s.userid = :userid " +
    "group by cast(extract(month from s.trandate) as bigint) " +
    "order by cast(extract(month from s.trandate) as bigint)"

  val myCache: Memcached = Shared.memd.get
  val minDuration: Duration = 1.milli
  val maxDuration: Duration = 2.milli

  private def errorJson(reason: String): Json = Json.obj("error" -> jString(reason))

  private def successJson(reason: String): Json = Json.obj("id" -> jString(reason))

  def uploadFile = Action(parse.multipartFormData) { request =>
    request.body.file("fileUpload").map { picture =>
      import java.io.File
      val filename = picture.filename
      val randname = r.nextInt(999999);
      val contentType = picture.contentType
      val (ret, success) =
      if (contentType.nonEmpty && contentType.get.endsWith("ms-excel")) {
        val savedName = s"$randname-${filename}"
        picture.ref.moveTo(new File(s"/tmp/$savedName.upload"))
        (savedName, true)
      } else
        ("bad type", false)
      Ok(if (success) successJson(ret) else errorJson(ret))
    }.getOrElse {
      Redirect(routes.QueryController.index).flashing(
        "error" -> "Missing file")
    }
  }

  def uploadFile2 = Action(parse.temporaryFile) { request =>
    val filename = r.nextInt(999999)
    import java.io.File
    request.body.moveTo(new File(s"/tmp/${filename}.upload"))
    val myMulti = request.body.asInstanceOf[MultipartFormData[String]]
    val ret = errorJson("bad file type")
    Ok(ret)
  }
}