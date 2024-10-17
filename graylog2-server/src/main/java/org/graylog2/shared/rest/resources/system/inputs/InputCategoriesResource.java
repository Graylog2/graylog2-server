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
package org.graylog2.shared.rest.resources.system.inputs;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.inputs.categories.InputCategoryService;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.Collection;

@RequiresAuthentication
@Api(value = "System/Inputs/Categories", description = "Input categories")
@Path("/system/inputs/categories")
@Produces(MediaType.APPLICATION_JSON)
public class InputCategoriesResource extends RestResource {
    private final InputCategoryService inputCategoryService;

    @Inject
    public InputCategoriesResource(InputCategoryService inputCategoryService) {
        this.inputCategoryService = inputCategoryService;
    }

    @GET
    @ApiOperation(value = "Get all available input categories")
    public CategoriesResponse categories() {
        return new CategoriesResponse(inputCategoryService.allCategories());
    }

    @GET
    @Path("subcategories/{category}")
    @ApiOperation(value = "Get all available subcategories for specified category")
    public SubCategoriesResponse subCategories(
            @ApiParam(name = "category", required = true)
            @PathParam("category") String category
    ) {
        return new SubCategoriesResponse(inputCategoryService.subCategoryByCategory(category));
    }

    public record CategoriesResponse(Collection<String> categories) {}

    public record SubCategoriesResponse(Collection<String> subCategories) {}
}
