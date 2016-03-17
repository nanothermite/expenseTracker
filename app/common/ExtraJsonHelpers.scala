package common

import play.api.http.{ContentTypeOf, ContentTypes, Writeable}
import play.api.mvc.Codec

import scala.concurrent.ExecutionContext.Implicits.global

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
