package common

import play.api.http.{Writeable, ContentTypeOf, ContentTypes}
import play.api.mvc.Codec

import scala.reflect._
import scala.reflect.runtime.universe._

/**
 * Created by hkatz on 3/15/16.
 */
trait ExtraJsonHelpers {
  implicit def contentTypeOf_ArgonautJson(implicit codec: Codec): ContentTypeOf[argonaut.Json] = {
    ContentTypeOf[argonaut.Json](Some(ContentTypes.JSON))
  }

  implicit def writeableOf_ArgonautJson(implicit codec: Codec): Writeable[argonaut.Json] = {
    Writeable(jsval => codec.encode(jsval.toString))
  }
}
