package controllers

import java.nio.charset.CodingErrorAction

import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json
import play.api.mvc._

import scala.io.Source

/**
 * Created by hkatz on 3/26/16.
 */
class XLSController extends Controller {

  def sendTemplate(tempType: String) = Action {
    val URLBASE = "http://wengen/DOWNLOAD"
    val (templateContent, tempName) =
      try {
        val tempFile = tempType match {
          case "C" => "contacts_template.xls"
          case "E" => "expenses_template.xls"
          case "_" => "BAD"
        }
        implicit val codec = scala.io.Codec("ISO-8859-1")

        codec.onMalformedInput(CodingErrorAction.IGNORE)
        codec.onUnmappableCharacter(CodingErrorAction.IGNORE)
        val data = Source.fromURL(s"${URLBASE}/$tempFile")
        val asStr = data.mkString
        (asStr, tempFile)
      } catch {
        case e: java.nio.charset.MalformedInputException =>
          (s"template not found ${e.printStackTrace}", "BAD")
        case e: java.io.IOException =>
          (s"template not found ${e.printStackTrace}", "BAD")
      }
    val tempEnum = Enumerator(templateContent.toCharArray.map(_.toByte))
    if (tempName == "BAD")
      Ok(Json.obj("request" -> "incorrect type"))
    else
      Result(
        header = ResponseHeader(200,
          Map(
            CONTENT_LENGTH -> templateContent.size.toString,
            CONTENT_DISPOSITION -> s"attachment; filename=$tempName"
          )) ,
        body = tempEnum
      )
  }
}
