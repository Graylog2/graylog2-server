package lib

/*
 * Copyright 2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import play.api.mvc.Result
import ExecutionContext.Implicits.global

/**
 * Adds a Cache-Control: no-cache header to all AJAX responses,
 * to prevent IE9 and earlier from blindly caching AJAX GET requests.
 *
 * Only adds a Cache-Control header if the Action hasn't set one by itself.
 */
class NoCacheHeader extends Filter {

  def apply(nextFilter: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    if (requestHeader.headers.get("X-Requested-With").getOrElse("").equalsIgnoreCase("xmlhttprequest")) {
      nextFilter(requestHeader).map { result =>
        if (! result.header.headers.contains("Cache-Control")) {
          result.withHeaders("Cache-Control" -> "no-cache")
        } else {
          result
        }
      }
    } else {
      nextFilter(requestHeader)
    }
  }

}
