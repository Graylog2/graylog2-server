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
import com.mongodb.client.model.Filters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.db.MarkerType;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.filtering.ComputedFieldRegistry;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.AttributeSortSpec;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.FilterOption;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.AttributeFieldFilters;
import org.graylog2.search.AttributeFieldSorts;
import org.graylog2.search.SearchQueryField;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toMap;
import static org.graylog.collectors.db.CollectorInstanceDTO.FIELD_LAST_SEEN;
import static org.graylog2.shared.utilities.StringUtils.f;

@PublicCloudAPI
@Tag(name = "Collectors", description = "Collector management")
@Path("/collectors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class CollectorInstancesResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorInstancesResource.class);

    private static final String DEFAULT_SORT_FIELD = "last_seen";
    private static final String DEFAULT_SORT_DIRECTION = "desc";

    private final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id("status")
                    .title("Status")
                    .filterable(true)
                    .sortable(true)
                    .filterOptions(Set.of(
                            FilterOption.create("online", "Online"),
                            FilterOption.create("offline", "Offline")))
                    .bsonFilterCreator((name, value) -> {
                        final Date offlineCutoff = Date.from(Instant.now().minus(getOfflineThreshold()));
                        return switch (value.getValue().toString()) {
                            case "online" -> Filters.gte(FIELD_LAST_SEEN, offlineCutoff);
                            case "offline" -> Filters.lt(FIELD_LAST_SEEN, offlineCutoff);
                            default -> Filters.gte(FIELD_LAST_SEEN, offlineCutoff);
                        };
                    })
                    .sortSpec(AttributeSortSpec.field(FIELD_LAST_SEEN))
                    .build(),
            EntityAttribute.builder().id("fleet_id")
                    .title("Fleet")
                    .relatedCollection(FleetService.COLLECTION_NAME)
                    .relatedIdentifier("_id")
                    .relatedDisplayFields(List.of(FleetDTO.FIELD_NAME))
                    .relatedDisplayTemplate("{name}")
                    .sortable(false)
                    .searchable(true)
                    .filterable(true)
                    .build(),
            EntityAttribute.builder().id("instance_uid").title("Instance UID").sortable(true).searchable(true).build(),
            EntityAttribute.builder().id("last_seen").title("Last Seen").type(SearchQueryField.Type.DATE).sortable(true).filterable(true).build(),
            EntityAttribute.builder().id("hostname").title("Hostname")
                    .dbField(CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES)
                    .bsonFilterCreator(AttributeFieldFilters.attributeArray("host.name"))
                    .sortSpec(AttributeFieldSorts.attributeArray(
                            CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES, "host.name"))
                    .sortable(true).searchable(true).build(),
            EntityAttribute.builder().id("os").title("OS")
                    .dbField(CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES)
                    .bsonFilterCreator(AttributeFieldFilters.attributeArray("os.type"))
                    .sortSpec(AttributeFieldSorts.attributeArray(
                            CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES, "os.type"))
                    .sortable(true).searchable(true).filterable(true).build(),
            EntityAttribute.builder().id("version").title("Version")
                    .dbField(CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES)
                    .bsonFilterCreator(AttributeFieldFilters.attributeArray("service.version"))
                    .sortSpec(AttributeFieldSorts.attributeArray(
                            CollectorInstanceDTO.FIELD_NON_IDENTIFYING_ATTRIBUTES, "service.version"))
                    .sortable(true).searchable(true).filterable(true).build()
    );

    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.DESC))
            .build();

    private final CollectorInstanceService collectorInstanceService;
    private final FleetService fleetService;
    private final SourceService sourceService;
    private final FleetTransactionLogService txnLogService;
    private final CollectorsConfigService collectorsConfigService;
    private final DbQueryCreator dbQueryCreator;

    @Inject
    public CollectorInstancesResource(CollectorInstanceService collectorInstanceService,
                                      FleetService fleetService,
                                      SourceService sourceService,
                                      ComputedFieldRegistry computedFieldRegistry,
                                      FleetTransactionLogService txnLogService,
                                      CollectorsConfigService collectorsConfigService) {
        this.collectorInstanceService = collectorInstanceService;
        this.fleetService = fleetService;
        this.sourceService = sourceService;
        this.txnLogService = txnLogService;
        this.dbQueryCreator = new DbQueryCreator(CollectorInstanceDTO.FIELD_INSTANCE_UID, ATTRIBUTES, computedFieldRegistry);
        this.collectorsConfigService = collectorsConfigService;
    }

    @GET
    @Path("/stats")
    @Timed
    @Operation(summary = "Get global collector statistics")
    public CollectorStatsResponse stats() {
        final long totalInstances = collectorInstanceService.count();
        final long onlineInstances = collectorInstanceService.countOnline(
                Instant.now().minus(getOfflineThreshold()));
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
    public PageListResponse<CollectorInstanceResponse> findInstances(
            @Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
            @Parameter(name = "sort",
                       description = "The field to sort the result on",
                       schema = @Schema(allowableValues = {"instance_uid", "last_seen", "status", "hostname", "os", "version"}))
            @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
            @Parameter(name = "order", description = "The sort direction",
                       schema = @Schema(allowableValues = {"asc", "desc"}))
            @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order
    ) {
        final Duration offlineThreshold = getOfflineThreshold();
        final Instant offlineCutoff = Instant.now().minus(offlineThreshold);
        final Bson dbQuery = dbQueryCreator.createDbQuery(filters, query);
        final var resolvedSort = DbSortResolver.resolve(ATTRIBUTES, sort, order);
        final var list = collectorInstanceService.findPaginated(dbQuery, resolvedSort, page, perPage);

        return PageListResponse.create(
                query,
                list.pagination(),
                list.pagination().total(),
                sort,
                order,
                list.stream().map(dto -> new CollectorInstanceResponse(
                        dto.lastSeen().isBefore(offlineCutoff) ? "offline" : "online",
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

    @DELETE
    @Path("/instances/{instanceUid}")
    @Timed
    @Operation(summary = "Delete a collector instance")
    @NoAuditEvent("TODO")
    @RequiresPermissions(FleetPermissions.INSTANCE_DELETE)
    public Response deleteInstance(
            @Parameter(name = "instanceUid", required = true) @PathParam("instanceUid") String instanceUid) {
        final boolean deleted = collectorInstanceService.deleteByInstanceUid(instanceUid);
        if (!deleted) {
            throw new NotFoundException(f("Collector instance <%s> not found", instanceUid));
        }
        return Response.noContent().build();
    }

    @POST
    @Path("/instances/reassign")
    @Timed
    @Operation(summary = "Reassign collector instances to a different fleet")
    @NoAuditEvent("TODO")
    public Response reassignInstances(ReassignCollectorsRequest request) {
        final String targetFleetId = request.fleetId();
        final Set<String> instanceUids = request.instanceUids();

        if (instanceUids == null || instanceUids.isEmpty()) {
            throw new BadRequestException("instance_uids must not be empty");
        }

        if (instanceUids.size() > FleetTransactionLogService.MAX_BULK_TARGET_SIZE) {
            throw new BadRequestException(f("instance_uids must not exceed %d elements", FleetTransactionLogService.MAX_BULK_TARGET_SIZE));
        }

        if (fleetService.get(targetFleetId).isEmpty()) {
            throw new NotFoundException(f("Target fleet <%s> not found", targetFleetId));
        }

        // TODO: Show pending configuration changes per collector instance (#25341)
        txnLogService.appendCollectorMarker(
                instanceUids,
                MarkerType.FLEET_REASSIGNED,
                new Document("new_fleet_id", targetFleetId));

        return Response.noContent().build();
    }

    private Duration getOfflineThreshold() {
        return collectorsConfigService.getOrDefault().collectorOfflineThreshold();
    }

    private static Map<String, Object> attributesToMap(Optional<List<Attribute>> attributes) {
        return attributes.orElse(List.of()).stream().collect(toMap(Attribute::key, Attribute::value));
    }
}
