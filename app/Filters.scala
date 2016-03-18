/**
 * Created by hkatz on 3/16/16.
 */
import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.cors.CORSFilter
import play.filters.headers.SecurityHeadersFilter

class Filters @Inject() (corsFilter: CORSFilter, secFilter: SecurityHeadersFilter) extends HttpFilters {
  def filters = Seq(corsFilter, secFilter)
}
