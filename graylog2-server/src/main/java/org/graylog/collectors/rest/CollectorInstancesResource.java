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
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
import org.bson.conversions.Bson;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.PendingChangesLookup;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.db.FleetReassignedPayload;
import org.graylog.collectors.db.MarkerType;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
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
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private static final String DEFAULT_SORT_FIELD = "last_seen";
    private static final String DEFAULT_SORT_DIRECTION = "desc";

    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.DESC))
            .build();

    private final CollectorInstanceService collectorInstanceService;
    private final FleetService fleetService;
    private final SourceService sourceService;
    private final ComputedFieldRegistry computedFieldRegistry;
    private final FleetTransactionLogService txnLogService;
    private final CollectorsConfigService collectorsConfigService;
    private final AuditEventSender auditEventSender;
    private final ActivityEntryMapper activityEntryMapper;

    @Inject
    public CollectorInstancesResource(CollectorInstanceService collectorInstanceService,
                                      FleetService fleetService,
                                      SourceService sourceService,
                                      ComputedFieldRegistry computedFieldRegistry,
                                      FleetTransactionLogService txnLogService,
                                      CollectorsConfigService collectorsConfigService,
                                      AuditEventSender auditEventSender,
                                      ActivityEntryMapper activityEntryMapper) {
        this.collectorInstanceService = collectorInstanceService;
        this.fleetService = fleetService;
        this.sourceService = sourceService;
        this.computedFieldRegistry = computedFieldRegistry;
        this.txnLogService = txnLogService;
        this.collectorsConfigService = collectorsConfigService;
        this.auditEventSender = auditEventSender;
        this.activityEntryMapper = activityEntryMapper;
    }

    private List<EntityAttribute> attributes(PendingChangesLookup pendingChangesLookup) {
        return List.of(
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
                EntityAttribute.builder().id("has_pending_changes").title("Sync")
                        .sortable(false)
                        .filterable(true)
                        .filterOptions(Set.of(
                                FilterOption.create("true", "Sync pending"),
                                FilterOption.create("false", "In sync")
                        ))
                        .type(SearchQueryField.Type.BOOLEAN)
                        .bsonFilterCreator((name, value) -> {
                            final var filter = CollectorInstanceService.hasPendingChangesFilter(pendingChangesLookup);
                            return (boolean) value.getValue() ? filter : Filters.nor(filter);
                        })
                        .build(),
                // Workaround: type(OBJECT_ID) is needed so the frontend sends identifier_type=OBJECT_ID
                // to the entity title service (POST /system/catalog/entities/titles), which converts the
                // string fleet_id to an ObjectId for the _id lookup in the fleets collection.
                // Without it, the title service receives identifier_type=STRING (the attribute type default),
                // fails to match the ObjectId _id, and filter pills show "Loading..." indefinitely.
                // The bsonFilterCreator prevents type(OBJECT_ID) from breaking filter queries on the
                // fleet_id field itself, which stores string values (not ObjectIds).
                EntityAttribute.builder().id("fleet_id")
                        .title("Fleet")
                        .relatedCollection(FleetService.COLLECTION_NAME)
                        .relatedIdentifier("_id")
                        .relatedProperty(FleetDTO.FIELD_NAME)
                        .relatedDisplayFields(List.of(FleetDTO.FIELD_NAME))
                        .relatedDisplayTemplate("{name}")
                        .type(SearchQueryField.Type.OBJECT_ID)
                        .bsonFilterCreator((name, value) -> Filters.eq(name, value.getValue().toString()))
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
    }


    @GET
    @Path("/stats")
    @Timed
    @Operation(summary = "Get global collector statistics")
    public CollectorStatsResponse stats() {
        // TODO for a permission check we would need to know which fleets are granted to the user
        // since we haven't implemented that yet, we can't add them as filters to the count queries, as a consequence
        // the counts would be wrong in case someone had explicit grants
        final var instanceCount = collectorInstanceService.countAcrossAllFleets(
                Instant.now().minus(getOfflineThreshold()));
        return new CollectorStatsResponse(
                instanceCount.total(),
                instanceCount.online(),
                instanceCount.offline(),
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
        final var pendingChangesLookup = txnLogService.pendingChangesLookup();
        final var attributes = attributes(pendingChangesLookup);
        final var dbQueryCreator = new DbQueryCreator("hostname", attributes, computedFieldRegistry);
        final Bson dbQuery = dbQueryCreator.createDbQuery(filters, query);
        final var resolvedSort = DbSortResolver.resolve(attributes, sort, order);
        final var list = collectorInstanceService.findPaginated(
                dbQuery,
                resolvedSort,
                page,
                perPage,
                dto -> isPermitted(CollectorsPermissions.FLEET_READ, dto.fleetId()));

        return PageListResponse.create(
                query,
                list.pagination(),
                list.pagination().total(),
                sort,
                order,
                list.stream().map(dto -> toResponse(dto, offlineCutoff, pendingChangesLookup.isPending(dto))).toList(),
                attributes,
                DEFAULTS);
    }

    @GET
    @Path("/instances/{instanceUid}")
    @Timed
    @Operation(summary = "Get a collector instance")
    public CollectorInstanceResponse getInstance(
            @Parameter(name = "instanceUid", required = true) @PathParam("instanceUid") String instanceUid) {
        final Optional<CollectorInstanceDTO> collector = collectorInstanceService.findByInstanceUid(instanceUid);
        if (collector.isEmpty()) {
            throw new NotFoundException(f("Collector instance <%s> not found", instanceUid));
        }
        final CollectorInstanceDTO dto = collector.get();
        // we scope instance read on the entire fleet, there's no sharing that makes sense per instance.
        checkPermission(CollectorsPermissions.FLEET_READ, dto.fleetId());

        final Duration offlineThreshold = getOfflineThreshold();
        final Instant offlineCutoff = Instant.now().minus(offlineThreshold);

        final var hasPendingChanges = !txnLogService.getUnprocessedMarkers(
                dto.fleetId(), dto.instanceUid(), dto.lastProcessedTxnSeq()).isEmpty();

        return toResponse(dto, offlineCutoff, hasPendingChanges);
    }

    @DELETE
    @Path("/instances/{instanceUid}")
    @Timed
    @Operation(summary = "Delete a collector instance")
    @NoAuditEvent("inline")
    public Response deleteInstance(
            @Parameter(name = "instanceUid", required = true) @PathParam("instanceUid") String instanceUid) {
        final Optional<CollectorInstanceDTO> collector = collectorInstanceService.findByInstanceUid(instanceUid);
        if (collector.isEmpty()) {
            throw new NotFoundException(f("Collector instance <%s> not found", instanceUid));
        }
        final CollectorInstanceDTO dto = collector.get();
        checkPermission(CollectorsPermissions.FLEET_INSTANCE_DELETE, dto.fleetId());

        final boolean deleted = collectorInstanceService.deleteByInstanceUid(instanceUid);
        if (!deleted) {
            throw new NotFoundException(f("Collector instance <%s> not found", instanceUid));
        }
        auditEventSender.success(AuditActor.user(getCurrentUser()), AuditEventTypes.COLLECTOR_INSTANCE_DELETE, Map.of(
                "instanceUid", instanceUid,
                "fleetId", dto.fleetId()
        ));
        return Response.noContent().build();
    }

    @GET
    @Path("/instances/{instanceUid}/pending_changes")
    @Timed
    @Operation(summary = "Get pending changes that a collector has not applied yet")
    public PendingChangesResponse instancePendingChanges(
            @Parameter(name = "instanceUid", required = true) @PathParam("instanceUid") String instanceUid) {
        final var collector = collectorInstanceService.findByInstanceUid(instanceUid).orElseThrow(() ->
                new NotFoundException(f("Collector instance <%s> not found", instanceUid)));

        checkPermission(CollectorsPermissions.FLEET_READ, collector.fleetId());

        final var markers = txnLogService.getUnprocessedMarkers(collector.fleetId(), collector.instanceUid(),
                collector.lastProcessedTxnSeq());
        final var coalesced = txnLogService.coalesce(markers);
        final var activities = activityEntryMapper.toEntries(
                markers.stream().filter(marker -> marker.type() != MarkerType.UNKNOWN).toList(),
                this::isPermitted);

        // Any unprocessed marker (including UNKNOWN, which is excluded from `activities`) means the
        // instance is still pending — matching the instances table's "Sync" column.
        return new PendingChangesResponse(!markers.isEmpty(),
                PendingChangesResponse.CoalescedActionsView.from(coalesced), activities);
    }

    @POST
    @Path("/instances/reassign")
    @Timed
    @Operation(summary = "Reassign collector instances to a different fleet")
    @NoAuditEvent("inline")
    public Response reassignInstances(@Valid @NotNull @RequestBody(required = true, useParameterTypeSchema = true) ReassignCollectorsRequest request) {
        final String targetFleetId = request.fleetId();
        final Set<String> instanceUids = request.instanceUids();

        checkPermission(CollectorsPermissions.FLEET_READ, targetFleetId);
        checkPermission(CollectorsPermissions.FLEET_INSTANCE_ASSIGN, targetFleetId);
        if (instanceUids == null || instanceUids.isEmpty()) {
            throw new BadRequestException("instance_uids must not be empty");
        }

        if (instanceUids.size() > FleetTransactionLogService.MAX_BULK_TARGET_SIZE) {
            throw new BadRequestException(f("instance_uids must not exceed %d elements", FleetTransactionLogService.MAX_BULK_TARGET_SIZE));
        }

        if (fleetService.get(targetFleetId).isEmpty()) {
            throw new NotFoundException(f("Target fleet <%s> not found", targetFleetId));
        }

        final Map<String, CollectorInstanceDTO> permittedInstances = collectorInstanceService.findByInstanceUids(instanceUids,
                instanceDTO -> isPermitted(CollectorsPermissions.FLEET_READ, instanceDTO.fleetId())
                        && isPermitted(CollectorsPermissions.FLEET_INSTANCE_ASSIGN, instanceDTO.fleetId()));
        final Set<String> permittedInstanceUids = permittedInstances.values().stream()
                .map(CollectorInstanceDTO::instanceUid)
                .collect(Collectors.toSet());

        txnLogService.appendCollectorMarker(
                permittedInstanceUids,
                MarkerType.FLEET_REASSIGNED,
                new FleetReassignedPayload(targetFleetId));

        final AuditActor auditActor = AuditActor.user(getCurrentUser());
        permittedInstances.values().forEach(dto ->
                auditEventSender.success(auditActor, AuditEventTypes.COLLECTOR_INSTANCE_REASSIGN, Map.of(
                        "instanceUid", dto.instanceUid(),
                        "fleetId", dto.fleetId(),
                        "targetFleetId", targetFleetId
                )));
        return Response.noContent().build();
    }

    private static @NonNull CollectorInstanceResponse toResponse(CollectorInstanceDTO dto, Instant offlineCutoff,
                                                                 boolean hasPendingChanges) {
        return new CollectorInstanceResponse(
                dto.lastSeen().isBefore(offlineCutoff) ? "offline" : "online",
                dto.instanceUid(),
                dto.fleetId(),
                dto.capabilities(),
                dto.enrolledAt(),
                dto.lastSeen(),
                dto.activeCertificateFingerprint(),
                dto.activeCertificateExpiresAt(),
                dto.nextCertificateFingerprint().orElse(null),
                dto.nextCertificateExpiresAt().orElse(null),
                attributesToMap(dto.identifyingAttributes()),
                attributesToMap(dto.nonIdentifyingAttributes()),
                hasPendingChanges
        );
    }

    private Duration getOfflineThreshold() {
        return collectorsConfigService.getOrDefault().collectorOfflineThreshold();
    }

    private static Map<String, Object> attributesToMap(Optional<List<Attribute>> attributes) {
        return attributes.orElse(List.of()).stream().collect(toMap(Attribute::key, Attribute::value));
    }
}
