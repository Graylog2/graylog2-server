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
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.search.AttributeFieldFilters;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
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
    private static final String DEFAULT_SORT_FIELD = "last_seen";
    private static final String DEFAULT_SORT_DIRECTION = "desc";

    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id("fleet_id").title("Fleet").sortable(false).searchable(true).build(),
            EntityAttribute.builder().id("instance_uid").title("Instance UID").sortable(true).searchable(true).build(),
            EntityAttribute.builder().id("last_seen").title("Last Seen").type(SearchQueryField.Type.DATE).sortable(true).build(),
            EntityAttribute.builder().id("hostname").title("Hostname").sortable(false).searchable(true).build(),
            EntityAttribute.builder().id("os").title("OS").sortable(false).searchable(true).build(),
            EntityAttribute.builder().id("version").title("Version").sortable(false).searchable(true).build()
    );

    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.DESC))
            .build();

    private static final Map<String, SearchQueryField> SEARCH_FIELD_MAPPING = Map.of(
            "fleet_id", SearchQueryField.create(CollectorInstanceDTO.FIELD_FLEET_ID, SearchQueryField.Type.OBJECT_ID),
            "instance_uid", SearchQueryField.create(CollectorInstanceDTO.FIELD_INSTANCE_UID),
            "last_seen", SearchQueryField.create(CollectorInstanceDTO.FIELD_LAST_SEEN, SearchQueryField.Type.DATE),
            "hostname", SearchQueryField.create(CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES, SearchQueryField.Type.STRING,
                    AttributeFieldFilters.attributeArray("host.name")),
            "os", SearchQueryField.create(CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES, SearchQueryField.Type.STRING,
                    AttributeFieldFilters.attributeArray("os.type")),
            "version", SearchQueryField.create(CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES, SearchQueryField.Type.STRING,
                    AttributeFieldFilters.attributeArray("service.version"))
    );

    private final CollectorInstanceService collectorInstanceService;
    private final FleetService fleetService;
    private final SourceService sourceService;
    private final SearchQueryParser searchQueryParser;

    @Inject
    public CollectorInstancesResource(CollectorInstanceService collectorInstanceService,
                                      FleetService fleetService, SourceService sourceService) {
        this.collectorInstanceService = collectorInstanceService;
        this.fleetService = fleetService;
        this.sourceService = sourceService;
        this.searchQueryParser = new SearchQueryParser(CollectorInstanceDTO.FIELD_INSTANCE_UID, SEARCH_FIELD_MAPPING);
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
                                                                     @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                   @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
//  TODO do we need this? seems to be for scopes  @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
                                                   @Parameter(name = "sort",
                                                              description = "The field to sort the result on",
                                                              schema = @Schema(allowableValues = {"instance_uid", "last_seen"}))
                                                       @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                   @Parameter(name = "order", description = "The sort direction",
                                                              schema = @Schema(allowableValues = {"asc", "desc"}))
                                                       @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order
    ) {

        final SearchQuery searchQuery = searchQueryParser.parse(query);

        final PaginatedList<CollectorInstanceDTO> list = collectorInstanceService.findPaginated(
                searchQuery,
                order.toBsonSort(sort),
                page,
                perPage);

        return PageListResponse.create(
                query,
                list.pagination(),
                list.pagination().total(),
                sort,
                order,
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
                ATTRIBUTES,
                DEFAULTS);
    }

    private static Map<String, Object> attributesToMap(Optional<List<Attribute>> attributes) {
        return attributes.orElse(List.of()).stream().collect(toMap(Attribute::key, Attribute::value));
    }
}
