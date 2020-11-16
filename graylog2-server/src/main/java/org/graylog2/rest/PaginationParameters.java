/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
    private String query = "";

    @ApiParam
    @QueryParam("page")
    @DefaultValue("1")
    private int page = 1;

    @ApiParam
    @QueryParam("per_page")
    @DefaultValue("50")
    private int perPage = 50;

    @ApiParam
    @QueryParam("sort")
    @DefaultValue("")
    private String sortBy = "";

    @ApiParam
    @QueryParam("order")
    @DefaultValue("asc")
    private String order = "asc";

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPerPage() {
        return perPage;
    }

    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
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
