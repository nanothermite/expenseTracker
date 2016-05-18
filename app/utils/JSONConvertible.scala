package utils

import play.api.libs.json._
import org.joda.time.DateTime

/**
 * Created by hkatz on 12/24/15.
 */
trait JSONConvertible {

  def toJSON: JsValue

  def asJSON(elt: Option[Any]) = elt match {
    case Some(value: Int)             => Json.toJson(value)
    case Some(value: Double)          => Json.toJson(value)
    case Some(value: String)          => Json.toJson(value)
    case Some(value: DateTime)        => Json.toJson(DateFormatter.formatDateTime(value))
    case Some(value: JSONConvertible) => value.toJSON
    case Some(value: Any)             => Json.toJson(value.toString)
    case None                         => JsNull
  }

  def asJSON(elts: Iterable[JSONConvertible]) = elts.map(_.toJSON)

  def jsonNullCheck(obj: String) = if (obj != null) Json.toJson(obj) else JsNull

  def jsonNullCheck(obj: Double) = if (obj != 0.0) Json.toJson(obj) else JsNull    // TODO
}
