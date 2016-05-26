package utils

/**
 * Created by hkatz on 8/10/15.
 */

import java.nio.charset.Charset
import java.security.MessageDigest

import common.Shared

trait Sha256 extends OneWayHash {
  private val messageDigest = MessageDigest.getInstance("sha-256")
  val r = Shared.r

  def toHexString(source: String, charset: Charset): String = {
    val bytes = source.getBytes(charset)
    val digest = messageDigest.digest(bytes)
    val hexCharArray = digest.flatMap(OneWayHash.byteToHexString)
    val hexString = new String(hexCharArray)
    hexString
  }

  def ranStr(limit: Int) = (1 to limit).map(_ => r.nextPrintableChar()).mkString("")
}