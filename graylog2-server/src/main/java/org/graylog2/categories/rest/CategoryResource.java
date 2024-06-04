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
package org.graylog2.categories.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;

import org.graylog2.categories.CategoryService;
import org.graylog2.categories.model.Category;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.shared.rest.resources.RestResource;

import jakarta.inject.Inject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Api(value = "Categories", description = "Manage Categories")
@Path("/categories")
@RequiresAuthentication
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource extends RestResource implements PluginRestResource {

    private final CategoryService categoryService;

    @Inject
    public CategoryResource(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GET
    @ApiOperation(value = "Get a list of categories")
    @RequiresPermissions(CategoryPermissions.CATEGORY_READ)
    public PaginatedList<Category> listStatuses(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("15") int perPage,
                                                @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                @ApiParam(name = "sort",
                                                          value = "The field to sort the result on",
                                                          required = true,
                                                          allowableValues = "category")
                                                @DefaultValue(Category.FIELD_CATEGORY) @QueryParam("sort") String sort,
                                                @ApiParam(name = "direction", value = "The sort direction", allowableValues = "asc,desc")
                                                @DefaultValue("asc") @QueryParam("direction") SortOrder order) {

        return categoryService.findPaginated(query, page, perPage, order, sort, null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Create a new category")
    @RequiresPermissions(CategoryPermissions.CATEGORY_EDIT)
    public Response create(@ApiParam(name = "Category") String value) {

        Category category = categoryService.create(value);
        return Response.ok().entity(category).build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Update a category")
    @RequiresPermissions(CategoryPermissions.CATEGORY_EDIT)
    public Response update(@ApiParam(name = "id", required = true) @PathParam("id") String id,
                           @ApiParam(name = "Category") String request) {

        Category category = categoryService.update(id, request);
        return Response.ok().entity(category).build();
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation("Delete a category")
    @RequiresPermissions(CategoryPermissions.CATEGORY_EDIT)
    public Response delete(@ApiParam(name = "id", required = true) @PathParam("id") String id) {
        categoryService.delete(id);
        return Response.ok().build();
    }
}
