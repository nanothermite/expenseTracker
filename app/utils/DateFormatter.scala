package utils

import com.github.nscala_time.time.Imports._
import org.joda.time.format.DateTimeFormatter

/**
 * Created by hkatz on 12/24/15.
 */
object DateFormatter {

  val DateFormatTL:DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  val YearFormatTL = DateTimeFormat.forPattern("yyyy")
  val TimeFormatTL = DateTimeFormat.forPattern("HH:mm:ss")
  val ShortTimeFormatTL = DateTimeFormat.forPattern("HH:mm")
  val DateTimeFormatTL = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
  val DateDDMMMYYYYTL = DateTimeFormat.forPattern("dd-MMM-yyyy")
  val DateYYYYMMDD = DateTimeFormat.forPattern("yyyyMMdd")
  val DateTimestamp = DateTimeFormat.forPattern("yyyyMMdd HHmmss")
  val DateFileStamp = DateTimeFormat.forPattern("yyyyMMdd~HHmmss")

  def formatDate(date: DateTime) = DateFormatTL.print(date)
  def formatYear(date: DateTime) = YearFormatTL.print(date)
  def formatTime(date: DateTime) = TimeFormatTL.print(date)
  def formatShortTime(date: DateTime) = ShortTimeFormatTL.print(date)
  def formatDateTime(date: DateTime) = DateTimeFormatTL.print(date)
  def formatDDMMMYYYY(date: DateTime) = DateDDMMMYYYYTL.print(date)
  def formatYYYYMMDD(date: DateTime) = DateYYYYMMDD.print(date)
  def formatTimestamp(date: DateTime) = DateTimestamp.print(date)
  def formatFileStamp(date: DateTime) = DateFileStamp.print(date)

}
