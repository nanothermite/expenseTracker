package controllers

import _root_.common.{ExtraJsonHelpers, myTypes}
import argonaut.Argonaut._
import argonaut._
import play.api.mvc._
import scala.util.Random

class UploadController extends Controller with myTypes with ExtraJsonHelpers {

  var r = Random

  private def errorJson(reason: String): Json = Json.obj("error" -> jString(reason))

  private def successJson(reason: String): Json = Json.obj("id" -> jString(reason))

  def uploadFile = Action(parse.multipartFormData) { request =>
    request.body.file("fileUpload").map { picture =>
      import java.io.File
      val filename = picture.filename
      val randname = r.nextInt(999999)
      val contentType = picture.contentType
      val (ret, success) =
        if (contentType.nonEmpty && contentType.get.endsWith("ms-excel")) {
          val savedName = s"$randname-$filename"
          picture.ref.moveTo(new File(s"/tmp/$savedName.upload"))
          (savedName, true)
        } else
          ("bad type", false)
      Ok(if (success) successJson(ret) else errorJson(ret))
    }.getOrElse {
      Redirect(routes.QueryController.index()).flashing(
        "error" -> "Missing file")
    }
  }
}