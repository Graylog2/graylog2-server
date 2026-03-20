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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.users.UserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Collectors/Activity")
@Path("/collectors/activity")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class CollectorsActivityResource extends RestResource {

    private static final int RECENT_ACTIVITY_LIMIT = 20;

    private final FleetTransactionLogService transactionLogService;
    private final FleetService fleetService;
    private final CollectorInstanceService instanceService;
    private final UserService userService;

    @Inject
    public CollectorsActivityResource(FleetTransactionLogService transactionLogService,
                                      FleetService fleetService,
                                      CollectorInstanceService instanceService,
                                      UserService userService) {
        this.transactionLogService = transactionLogService;
        this.fleetService = fleetService;
        this.instanceService = instanceService;
        this.userService = userService;
    }

    @GET
    @Path("/recent")
    @Timed
    @Operation(summary = "Get recent activity across all fleets and collectors")
    @RequiresPermissions(CollectorsPermissions.ACTIVITY_READ)
    public RecentActivityResponse recent() {
        final List<TransactionMarker> markers = transactionLogService.getRecentMarkers(RECENT_ACTIVITY_LIMIT);
        if (markers.isEmpty()) {
            return new RecentActivityResponse(List.of());
        }

        // Batch-resolve fleet names
        final Map<String, String> fleetNames = fleetService.getAllFleets().stream()
                .collect(Collectors.toMap(FleetDTO::id, FleetDTO::name));

        // Collect all instance UIDs from collector-targeted markers
        final Set<String> instanceUids = new HashSet<>();
        for (final var marker : markers) {
            if (TransactionMarker.TARGET_COLLECTOR.equals(marker.target())) {
                instanceUids.addAll(marker.targetIds());
            }
        }

        // Batch-resolve instance hostnames
        final Map<String, CollectorInstanceDTO> instances = instanceUids.isEmpty()
                ? Map.of()
                : instanceService.findByInstanceUids(instanceUids);

        // Batch-resolve actor display names
        final Set<String> usernames = markers.stream()
                .map(TransactionMarker::createdByUser)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        final Map<String, String> userDisplayNames = resolveUserDisplayNames(usernames);

        // Build response entries
        final List<RecentActivityResponse.ActivityEntry> entries = new ArrayList<>();
        for (final var marker : markers) {
            entries.add(toActivityEntry(marker, fleetNames, instances, userDisplayNames));
        }
        return new RecentActivityResponse(entries);
    }

    private RecentActivityResponse.ActivityEntry toActivityEntry(
            TransactionMarker marker,
            Map<String, String> fleetNames,
            Map<String, CollectorInstanceDTO> instances,
            Map<String, String> userDisplayNames) {

        // Resolve actor
        final RecentActivityResponse.ActorInfo actor;
        if (marker.createdByUser() != null) {
            final String fullName = userDisplayNames.getOrDefault(marker.createdByUser(), marker.createdByUser());
            actor = new RecentActivityResponse.ActorInfo(marker.createdByUser(), fullName);
        } else {
            actor = null;
        }

        // Resolve targets
        final List<RecentActivityResponse.TargetInfo> targets = new ArrayList<>();
        for (final var targetId : marker.targetIds()) {
            final String name;
            if (TransactionMarker.TARGET_FLEET.equals(marker.target())) {
                // skip the target if it's a fleet we have no permission to
                if (!isPermitted(CollectorsPermissions.FLEET_READ, targetId)) {
                    continue;
                }
                name = fleetNames.getOrDefault(targetId, targetId);
            } else {
                // skip the target if the user cannot see the target's fleet
                if (instances.containsKey(targetId)
                        && !isPermitted(CollectorsPermissions.FLEET_READ, instances.get(targetId).fleetId())) {
                    continue;
                }
                name = resolveInstanceHostname(instances, targetId);
            }
            targets.add(new RecentActivityResponse.TargetInfo(targetId, name, marker.target()));
        }

        // Resolve details
        final Map<String, String> details = resolveDetails(marker, fleetNames);

        return new RecentActivityResponse.ActivityEntry(
                marker.seq(),
                marker.createdAt(),
                marker.type().name(),
                actor,
                targets,
                details);
    }

    private String resolveInstanceHostname(Map<String, CollectorInstanceDTO> instances, String instanceUid) {
        final var instance = instances.get(instanceUid);
        if (instance != null && instance.nonIdentifyingAttributes().isPresent()) {
            return instance.nonIdentifyingAttributes().get().stream()
                    .filter(attr -> "host.name".equals(attr.key()))
                    .map(attr -> attr.value().toString())
                    .findFirst()
                    .orElse(instanceUid);
        }
        return instanceUid;
    }

    private Map<String, String> resolveDetails(TransactionMarker marker, Map<String, String> fleetNames) {
        if (marker.type() == MarkerType.FLEET_REASSIGNED && marker.payload() != null) {
            final String newFleetId = marker.payload().getString("new_fleet_id");
            if (newFleetId != null && isPermitted(CollectorsPermissions.FLEET_READ, newFleetId)) {
                return Map.of(
                        "new_fleet_id", newFleetId,
                        "new_fleet_name", fleetNames.getOrDefault(newFleetId, newFleetId));
            }
        }
        return Map.of();
    }

    private Map<String, String> resolveUserDisplayNames(Set<String> usernames) {
        final Map<String, String> result = new HashMap<>();
        for (final var username : usernames) {
            final var fullName = Optional.ofNullable(userService.load(username))
                    .or(() -> Optional.ofNullable(userService.loadById(username)))
                    .map(user -> isPermitted(RestPermissions.USERS_READ, user.getId()) ? user.getFullName() : "Unknown")
                    .orElse(username);
            result.put(username, fullName);
        }
        return result;
    }
}
