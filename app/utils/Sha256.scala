package utils

/**
 * Created by hkatz on 8/10/15.
 */

import java.nio.charset.Charset
import java.security.MessageDigest

trait Sha256 extends OneWayHash {
  private val messageDigest = MessageDigest.getInstance("sha-256")

  def toHexString(source: String, charset: Charset): String = {
    val bytes = source.getBytes(charset)
    val digest = messageDigest.digest(bytes)
    val hexCharArray = digest.flatMap(OneWayHash.byteToHexString)
    val hexString = new String(hexCharArray)
    hexString
  }
}