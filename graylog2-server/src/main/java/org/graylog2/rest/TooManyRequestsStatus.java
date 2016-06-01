/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest;

import javax.ws.rs.core.Response;

/**
 * Implementing StatusType for 429 TOO MANY REQUEST (the most suitable HTTP status code for
 * requesting throttling), because javax.ws.rs.core.Response.Status does not implement it.
 */
public class TooManyRequestsStatus implements Response.StatusType {
    @Override
    public int getStatusCode() { return 429; }

    @Override
    public Response.Status.Family getFamily() { return Response.Status.Family.CLIENT_ERROR; }

    @Override
    public String getReasonPhrase() { return "Too many requests"; }
}
