package controllers

import java.io.File

import play.api.Environment
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Created by hkatz on 3/26/16.
 */
class XLSController extends Controller {

  def sendTemplate(tempType: String) = Action {
    val myEnv = new Environment(new File("."), this.getClass.getClassLoader, play.api.Mode.Dev)
    val (tempFile, tempName) = tempType match {
      case "C" => (myEnv.getFile("public/contacts_template.xls"), "contact.xls")
      case "E" => (myEnv.getFile("public/expenses_template.xls"), "template.xls")
      case "_" => (myEnv.getFile("public/contacts_template.xls"), "BAD")
    }
    if (tempName == "BAD")
      Ok(Json.obj("request" -> "incorrect type"))
    else
      Ok.sendFile(
        content = tempFile,
        fileName = _ => tempName
      )
  }
}
