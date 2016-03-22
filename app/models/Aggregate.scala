package models

import play.api.libs.json._
import utils.JSONConvertible

/**
 * Created by hkatz on 12/24/15.
 */
case class Aggregate(credit: Double, debit: Double, period: String, periodType: String) extends JSONConvertible {
  override def toJSON: JsValue = Json.obj(
    "credit"       -> jsonNullCheck(credit),
    "debit"        -> jsonNullCheck(debit),
    "period"       -> jsonNullCheck(period),
    "periodType"   -> jsonNullCheck(periodType)
  )
}
