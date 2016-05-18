/**
 * Created by hkatz on 3/16/16.
 */
import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.cors.CORSFilter
import play.filters.headers.SecurityHeadersFilter
import utils.LoggingFilter

class Filters @Inject() (corsFilter: CORSFilter,
                         secFilter: SecurityHeadersFilter,
                         log: LoggingFilter) extends HttpFilters {
  override def filters =
    Seq(corsFilter, secFilter, log)
}
