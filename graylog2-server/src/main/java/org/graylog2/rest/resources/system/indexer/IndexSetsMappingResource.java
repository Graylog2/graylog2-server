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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.indexset.IndexSetFieldTypeSummaryService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.rest.resources.system.indexer.requests.FieldTypeSummaryRequest;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldTypeSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Locale;
import java.util.Set;

import static org.graylog2.indexer.indexset.IndexSetFieldTypeSummaryService.DEFAULT_SORT_FIELD;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/IndexSets/Types", description = "Index set field types", tags = {CLOUD_VISIBLE})
@Path("/system/indices/index_sets/types")
@Produces(MediaType.APPLICATION_JSON)
public class IndexSetsMappingResource extends RestResource {
    private final IndexSetFieldTypeSummaryService indexSetFieldTypeSummaryService;

    @Inject
    public IndexSetsMappingResource(IndexSetFieldTypeSummaryService indexSetFieldTypeSummaryService) {
        this.indexSetFieldTypeSummaryService = indexSetFieldTypeSummaryService;
    }

    @POST
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Get field type summaries for given index sets and field")
    public PageListResponse<IndexSetFieldTypeSummary> fieldTypeSummaries(@ApiParam(name = "JSON body", required = true)
                                                                         @Valid @NotNull FieldTypeSummaryRequest request,
                                                                         @ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                                         @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                                         @ApiParam(name = "sort",
                                                                                   value = "The field to sort the result on",
                                                                                   required = true,
                                                                                   allowableValues = "index_set_id,index_set_title")
                                                                         @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                                         @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc,desc")
                                                                         @DefaultValue("asc") @QueryParam("order") String order) {
        final Set<String> streamsIds = request.streamsIds();
        final String fieldName = request.fieldName();
        return indexSetFieldTypeSummaryService.getIndexSetFieldTypeSummary(streamsIds,
                fieldName,
                indexSetId -> isPermitted(RestPermissions.INDEXSETS_READ, indexSetId),
                page,
                perPage,
                sort,
                Sorting.Direction.valueOf(order.toUpperCase(Locale.ROOT))
        );
    }
}
