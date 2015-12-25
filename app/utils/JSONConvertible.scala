package utils

import argonaut.Argonaut._
import argonaut._
import org.joda.time.DateTime

/**
 * Created by hkatz on 12/24/15.
 */
trait JSONConvertible {

  def toJSON: Json

  def asJSON(elt: Option[Any]) = elt match {
    case Some(value: Int)             => jNumber(value)
    case Some(value: Double)          => jNumber(value)
    case Some(value: String)          => jString(value)
    case Some(value: DateTime)        => jString(DateFormatter.formatDateTime(value))
    case Some(value: JSONConvertible) => value.toJSON
    case Some(value: Any)             => jString(value.toString)
    case None                         => jNull
  }

  def asJSON(elts: Iterable[JSONConvertible]) = elts.map(_.toJSON)
}
