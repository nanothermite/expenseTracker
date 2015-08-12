package utils

import java.nio.charset.Charset

/**
 * Created by hkatz on 8/11/15.
 */
trait OneWayHash {
  def toHexString(source: String, charset: Charset): String
}

object OneWayHash {
  def byteToHexString(b: Byte): String = f"$b%02x"

  def byteArrayToHexString(bytes: Array[Byte]): String = new String(bytes.flatMap(OneWayHash.byteToHexString))
}
