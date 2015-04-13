import java.util.concurrent.Executors

import common.Shared
import play.api._

/**
 * Created by hkatz on 4/8/15.
 */
object Global extends GlobalSettings {
  override def beforeStart(app : Application): Unit = {
    Shared.genMemHandle()
  }
}
