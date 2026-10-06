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

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.FleetDTO;
import org.graylog.collectors.db.FleetReassignedPayload;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog.collectors.rest.RecentActivityResponse.ActivityDetails;
import org.graylog.collectors.rest.RecentActivityResponse.FleetReassignedDetails;
import org.graylog.collectors.rest.RecentActivityResponse.TargetInfo;
import org.graylog.security.HasPermissions;
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

/**
 * Maps {@link TransactionMarker}s to display-ready {@link RecentActivityResponse.ActivityEntry}s, batch-resolving
 * fleet names, collector hostnames, and actor display names. Shared between the activity feed
 * ({@link CollectorsActivityResource}) and the per-instance pending-changes view
 * ({@link CollectorInstancesResource}) so both render transaction history identically.
 * <p>
 * Permission checks are caller-supplied (a {@link HasPermissions}, typically the resource's
 * {@code this::isPermitted}) because visibility filtering must run against the requesting user's subject.
 */
@Singleton
public class ActivityEntryMapper {

    private final FleetService fleetService;
    private final CollectorInstanceService instanceService;
    private final UserService userService;

    @Inject
    ActivityEntryMapper(FleetService fleetService,
                        CollectorInstanceService instanceService,
                        UserService userService) {
        this.fleetService = fleetService;
        this.instanceService = instanceService;
        this.userService = userService;
    }

    List<RecentActivityResponse.ActivityEntry> toEntries(List<TransactionMarker> markers, HasPermissions permissions) {
        if (markers.isEmpty()) {
            return List.of();
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
        final Map<String, String> userDisplayNames = resolveUserDisplayNames(usernames, permissions);

        final List<RecentActivityResponse.ActivityEntry> entries = new ArrayList<>();
        for (final var marker : markers) {
            entries.add(toActivityEntry(marker, fleetNames, instances, userDisplayNames, permissions));
        }
        return entries;
    }

    private RecentActivityResponse.ActivityEntry toActivityEntry(
            TransactionMarker marker,
            Map<String, String> fleetNames,
            Map<String, CollectorInstanceDTO> instances,
            Map<String, String> userDisplayNames,
            HasPermissions permissions) {

        // Resolve actor
        final RecentActivityResponse.ActorInfo actor;
        if (marker.createdByUser() != null) {
            final String fullName = userDisplayNames.getOrDefault(marker.createdByUser(), marker.createdByUser());
            actor = new RecentActivityResponse.ActorInfo(marker.createdByUser(), fullName);
        } else {
            actor = null;
        }

        // Resolve targets
        final List<TargetInfo> targets = new ArrayList<>();
        for (final var targetId : marker.targetIds()) {
            final String id;
            final String name;
            if (TransactionMarker.TARGET_FLEET.equals(marker.target())) {
                if (fleetNames.containsKey(targetId)) {
                    // skip the target if it's a fleet we have no permission to
                    if (!permissions.isPermitted(CollectorsPermissions.FLEET_READ, targetId)) {
                        continue;
                    }
                    id = targetId;
                    name = fleetNames.get(targetId);
                } else {
                    id = null;
                    name = "[deleted]";
                }
            } else {
                if (instances.containsKey(targetId)) {
                    // skip the target if the user cannot see the target's fleet
                    if (!permissions.isPermitted(CollectorsPermissions.FLEET_READ, instances.get(targetId).fleetId())) {
                        continue;
                    }
                    id = targetId;
                    name = resolveInstanceHostname(instances, targetId);
                } else {
                    id = null;
                    name = "[deleted]";
                }
            }
            targets.add(new TargetInfo(id, name, marker.target()));
        }

        return new RecentActivityResponse.ActivityEntry(
                marker.seq(),
                marker.createdAt(),
                marker.type().name(),
                actor,
                targets,
                resolveDetails(marker, fleetNames, permissions));
    }

    private static String resolveInstanceHostname(Map<String, CollectorInstanceDTO> instances, String instanceUid) {
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

    @Nullable
    private static ActivityDetails resolveDetails(TransactionMarker marker,
                                                  Map<String, String> fleetNames,
                                                  HasPermissions permissions) {
        if (marker.type() == MarkerType.FLEET_REASSIGNED
                && marker.payload() instanceof FleetReassignedPayload(String newFleetId)) {
            if (fleetNames.containsKey(newFleetId)) {
                if (permissions.isPermitted(CollectorsPermissions.FLEET_READ, newFleetId)) {
                    return new FleetReassignedDetails(
                            new TargetInfo(newFleetId, fleetNames.get(newFleetId), TransactionMarker.TARGET_FLEET));
                }
                return null;
            }
            return new FleetReassignedDetails(
                    new TargetInfo(null, "[deleted]", TransactionMarker.TARGET_FLEET));
        }
        return null;
    }

    private Map<String, String> resolveUserDisplayNames(Set<String> usernames,
                                                        HasPermissions permissions) {
        final Map<String, String> result = new HashMap<>();
        for (final var username : usernames) {
            final var fullName = Optional.ofNullable(userService.load(username))
                    .or(() -> Optional.ofNullable(userService.loadById(username)))
                    .map(user -> permissions.isPermitted(RestPermissions.USERS_READ, user.getId()) ? user.getFullName() : "Unknown")
                    .orElse(username);
            result.put(username, fullName);
        }
        return result;
    }
}
