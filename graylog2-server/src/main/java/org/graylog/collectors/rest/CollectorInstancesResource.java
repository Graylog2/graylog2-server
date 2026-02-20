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
package org.graylog.collectors.rest;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQuery;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;

@PublicCloudAPI
@Tag(name = "Collectors", description = "Collector management")
@Path("/collectors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class CollectorInstancesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorInstancesResource.class);

    private static final Duration ONLINE_THRESHOLD = Duration.ofMinutes(5);

    private final CollectorInstanceService collectorInstanceService;
    private final FleetService fleetService;
    private final SourceService sourceService;

    @Inject
    public CollectorInstancesResource(CollectorInstanceService collectorInstanceService,
                                      FleetService fleetService, SourceService sourceService) {
        this.collectorInstanceService = collectorInstanceService;
        this.fleetService = fleetService;
        this.sourceService = sourceService;
    }

    @GET
    @Path("/stats")
    @Timed
    @Operation(summary = "Get global collector statistics")
    public CollectorStatsResponse stats() {
        final long totalInstances = collectorInstanceService.count();
        final long onlineInstances = collectorInstanceService.countOnline(
                Instant.now().minus(ONLINE_THRESHOLD));
        return new CollectorStatsResponse(
                totalInstances,
                onlineInstances,
                totalInstances - onlineInstances,
                fleetService.count(),
                sourceService.count());
    }

    @GET
    @Timed
    @Operation(summary = "Get a paginated list of collector instances")
    public PageListResponse<CollectorInstanceResponse> findInstances(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                                     @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage
//                                                   @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
//                                                   @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
//                                                   @Parameter(name = "sort",
//                                                              description = "The field to sort the result on",
//                                                              required = true,
//                                                              schema = @Schema(allowableValues = {"title", "description", "priority", "status"}))
//                                                       @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
//                                                   @Parameter(name = "order", description = "The sort direction",
//                                                              schema = @Schema(allowableValues = {"asc", "desc"}))
//                                                       @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order
    ) {


        final PaginatedList<CollectorInstanceDTO> list = collectorInstanceService.findPaginated(
                new SearchQuery(""),
                SortOrder.ASCENDING.toBsonSort("instance_uid"),
                page,
                perPage);

        return PageListResponse.create(
                "",
                list.pagination(),
                list.pagination().total(),
                null,
                (String) null,
                list.stream().map(dto -> new CollectorInstanceResponse(
                        dto.instanceUid(),
                        dto.fleetId(),
                        dto.capabilities(),
                        dto.enrolledAt(),
                        dto.lastSeen(),
                        dto.certificateFingerprint(),
                        attributesToMap(dto.identifyingAttributes()),
                        attributesToMap(dto.nonIdentifyingAttributes())
                )).toList(),
                List.of(),
                EntityDefaults.builder().sort(Sorting.create("instance_uid", Sorting.Direction.ASC)).build());
    }

    private static Map<String, Object> attributesToMap(Optional<List<Attribute>> attributes) {
        return attributes.orElse(List.of()).stream().collect(toMap(Attribute::key, Attribute::value));
    }
}
