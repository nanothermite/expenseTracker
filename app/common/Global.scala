package common

import play.api
import play.api.inject.{Binding, Module}

import scala.concurrent.ExecutionContext.Implicits.{global => ec}

/**
 * Created by hkatz on 3/16/16.
 */

/**
 * Created by hkatz on 4/8/15.
 */

class Global extends Module {
  override def bindings(environment: api.Environment, configuration: api.Configuration): Seq[Binding[_]] = Seq(
    bind[Shared].to(new Shared(environment, configuration)).eagerly()
  )
}
