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
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.PermittedStreams;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesListService;
import org.graylog2.indexer.indexset.IndexSetFieldTypeSummaryService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.rest.resources.system.indexer.requests.FieldTypeSummaryRequest;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldType;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetFieldTypeSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import jakarta.inject.Inject;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
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
    private final PermittedStreams permittedStreams;
    private final IndexFieldTypesListService indexFieldTypesListService;

    @Inject
    public IndexSetsMappingResource(final IndexSetFieldTypeSummaryService indexSetFieldTypeSummaryService,
                                    final IndexFieldTypesListService indexFieldTypesListService,
                                    final PermittedStreams permittedStreams) {
        this.indexSetFieldTypeSummaryService = indexSetFieldTypeSummaryService;
        this.indexFieldTypesListService = indexFieldTypesListService;
        this.permittedStreams = permittedStreams;
    }

    @GET
    @Path("/{index_set_id}")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets list of field_name-field_type pairs for given index set, paginated")
    public PageListResponse<IndexSetFieldType> indexSetFieldTypesList(@ApiParam(name = "index_set_id") @PathParam("index_set_id") String indexSetId,
                                                                      @ApiParam(name = "fieldNameQuery") @QueryParam("fieldNameQuery") @DefaultValue("") String fieldNameQuery,
                                                                      @ApiParam(name = "filters") @QueryParam("filters") List<String> filters,
                                                                      @ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                                      @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                                      @ApiParam(name = "sort",
                                                                                value = "The field to sort the result on",
                                                                                required = true,
                                                                                allowableValues = "field_name,type,origin,is_reserved")
                                                                      @DefaultValue(IndexSetFieldType.DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                                      @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc,desc")
                                                                      @DefaultValue("asc") @QueryParam("order") String order,
                                                                      @Context SearchUser searchUser) {
        checkPermission(RestPermissions.INDEXSETS_READ, indexSetId);
        return indexFieldTypesListService.getIndexSetFieldTypesListPage(indexSetId,
                fieldNameQuery,
                filters,
                page,
                perPage,
                sort,
                Sorting.Direction.valueOf(order.toUpperCase(Locale.ROOT)));
    }

    @GET
    @Path("/{index_set_id}/all")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets list of all field_name-field_type pairs for given index set")
    public List<IndexSetFieldType> indexSetFieldTypesList(@ApiParam(name = "index_set_id") @PathParam("index_set_id") String indexSetId,
                                                          @ApiParam(name = "fieldNameQuery") @QueryParam("fieldNameQuery") @DefaultValue("") String fieldNameQuery,
                                                          @ApiParam(name = "filters") @QueryParam("filters") List<String> filters,
                                                          @ApiParam(name = "sort",
                                                                    value = "The field to sort the result on",
                                                                    required = true,
                                                                    allowableValues = "field_name,type,origin,is_reserved")
                                                          @DefaultValue(IndexSetFieldType.DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                          @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc,desc")
                                                          @DefaultValue("asc") @QueryParam("order") String order,
                                                          @Context SearchUser searchUser) {
        checkPermission(RestPermissions.INDEXSETS_READ, indexSetId);
        return indexFieldTypesListService.getIndexSetFieldTypesList(indexSetId,
                fieldNameQuery,
                filters,
                sort,
                Sorting.Direction.valueOf(order.toUpperCase(Locale.ROOT)));
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
                                                                         @DefaultValue("asc") @QueryParam("order") String order,
                                                                         @Context SearchUser searchUser) {
        final Set<String> streamsIds = normalizeStreamIds(request.streamsIds(), searchUser);
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

    private Set<String> normalizeStreamIds(Set<String> streams, SearchUser searchUser) {
        return (streams == null || streams.isEmpty())
                ? permittedStreams.loadAllMessageStreams(searchUser)
                : streams;
    }
}
