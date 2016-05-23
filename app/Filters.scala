/**
 * Created by hkatz on 3/16/16.
 */
import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.cors.CORSFilter
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter

class Filters @Inject() (corsFilter: CORSFilter,
                         secFilter: SecurityHeadersFilter,
                         csrfFilter: CSRFFilter) extends HttpFilters {
  override def filters =
    Seq(corsFilter, secFilter, csrfFilter)
}
