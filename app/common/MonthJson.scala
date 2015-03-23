package common

import play.api.libs.json._
import scala.language.implicitConversions

/**
 * Created by hkatz on 3/19/15.
 */
case class MonthVals(id1: String, val1: List[String])

case class MonthData(id2: String, val2: List[MonthVals])

class MonthJson(id1: String, id2: String, vals: List[MonthVals])
/*  val mdata = new MonthData(id2, vals)

  implicit val monthValsWrites = Json.writes[MonthVals]
     def writes(monvals: MonthVals) = Json.obj(
      "id1" -> monvals.id1,
      "val1" -> monvals.val1
    )
  }


   implicit val monthDataWrites = Json.writes[MonthData]
    * def writes(mondata: MonthData) = Json.obj(
      "id2" -> mondata.id2,
      "val2" -> mondata.val2
    )
  } */
