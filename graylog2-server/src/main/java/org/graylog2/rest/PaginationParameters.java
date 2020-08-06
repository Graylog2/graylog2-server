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

import com.google.common.base.MoreObjects;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

/**
 * Shareable pagination parameters to be used with {@link org.graylog2.database.PaginatedDbService}.
 * <p>
 * Example usage:
 * <pre>{@code
 * @GET
 * public Response get(@BeanParam PaginationParameters params) {
 *     // Do something with the parameters
 *     return Response.ok().build();
 * }
 * }</pre>
 */
public class PaginationParameters {
    @ApiParam
    @QueryParam("query")
    @DefaultValue("")
    private String query;

    @ApiParam
    @QueryParam("page")
    @DefaultValue("1")
    private int page;

    @ApiParam
    @QueryParam("per_page")
    @DefaultValue("50")
    private int perPage;

    @ApiParam
    @QueryParam("sort")
    @DefaultValue("")
    private String sortBy;

    @ApiParam
    @QueryParam("order")
    @DefaultValue("asc")
    private String order;

    public String getQuery() {
        return query;
    }

    public int getPage() {
        return page;
    }

    public int getPerPage() {
        return perPage;
    }

    public String getSortBy() {
        return sortBy;
    }

    public String getOrder() {
        return order;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("query", query)
                .add("page", page)
                .add("perPage", perPage)
                .add("sortBy", sortBy)
                .add("order", order)
                .toString();
    }
}
