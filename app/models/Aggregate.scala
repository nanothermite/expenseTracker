package models

import argonaut.Argonaut._
import argonaut._
import utils.JSONConvertible

/**
 * Created by hkatz on 12/24/15.
 */
case class Aggregate(credit: Double, debit: Double, period: String, periodType: String) extends JSONConvertible {
  override def toJSON: Json = Json(
    "credit"       -> jNumber(credit),
    "debit"        -> jNumber(debit),
    "period"       -> jString(period),
    "periodType"   -> jString(periodType)
  )
}
