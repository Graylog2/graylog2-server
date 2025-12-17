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
package org.graylog2.rest.resources.streams;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.List;
import java.util.Locale;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Streams/Pipeline Rules", description = "Pipeline rules associated with a stream", tags = {CLOUD_VISIBLE})
@Path("/streams/pipeline_rules")
public class StreamPipelineRulesResource extends RestResource {
    private static final String ATTRIBUTE_PIPELINE_RULE = "pipeline_rule";
    private static final String ATTRIBUTE_PIPELINE = "pipeline";
    private static final String ATTRIBUTE_CONNECTED_STREAM = "connected_stream";
    private static final String DEFAULT_SORT_FIELD = ATTRIBUTE_PIPELINE_RULE;
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id(ATTRIBUTE_PIPELINE_RULE).title("Pipeline Rule").searchable(false).build(),
            EntityAttribute.builder().id(ATTRIBUTE_PIPELINE).title("Pipeline").searchable(false).build(),
            EntityAttribute.builder().id(ATTRIBUTE_CONNECTED_STREAM).title("Connected Stream").searchable(false).build()
    );
    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

//    @Inject
//    public StreamPipelineRulesResource() {
//    }

    @GET
    @Timed
    @Path("/paginated/{streamId}")
    @ApiOperation(value = "Get a paginated list of associated pipeline rules for the specified stream")
    @Produces(MediaType.APPLICATION_JSON)
    public PageListResponse<StreamPipelineRulesResponse> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                                 @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                                 @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                                 @ApiParam(name = "filters") @QueryParam("filters") List<String> filters,
                                                                 @ApiParam(name = "sort",
                                                                           value = "The field to sort the result on",
                                                                           required = true,
                                                                           allowableValues = "title,description,created_at,updated_at,status")
                                                                 @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                                 @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                                 @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order) {

        return null;
    }
}
