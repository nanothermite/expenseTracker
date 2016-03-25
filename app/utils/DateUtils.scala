package utils

import com.github.nscala_time.time.Imports._

/**
 * Thread-safe date and time formatting functions using joda-time.
 */
object DateUtils {
  val YMD = "yyyy-MM-dd"
  val YMDHMS = "yyyy-MM-dd H:m:s"

  def dateParse(text: String, pattern: String): DateTime = DateTime.parse(text, DateTimeFormat.forPattern(pattern))
}
