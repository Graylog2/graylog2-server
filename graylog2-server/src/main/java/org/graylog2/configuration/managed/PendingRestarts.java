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
package org.graylog2.configuration.managed;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tells which stored values a node doesn't run with yet, because they have been changed after the node last picked
 * up its configuration.
 * <p>
 * Nothing is flagged when a value is stored and nothing is cleared when a node restarts: a value is pending on a node
 * for as long as its change is newer than the {@link NodeConfigurationState#appliedAt()} of that node. A value shared
 * by all nodes therefore stops being pending node by node during a rolling restart, and stays pending on a node
 * which was down when it changed.
 */
@Singleton
public class PendingRestarts {
    /**
     * How long a node may take from loading its configuration until its process is available with it. A node which
     * takes longer most likely failed to start and shouldn't keep a restart from being requested again.
     */
    static final Duration RESTART_TIMEOUT = Duration.ofMinutes(10);

    private final ConfigurationCatalog catalog;
    private final ManagedConfigurationServices configurationServices;
    private final Clock clock;

    @Inject
    public PendingRestarts(ConfigurationCatalog catalog, ManagedConfigurationServices configurationServices, Clock clock) {
        this.catalog = catalog;
        this.configurationServices = configurationServices;
        this.clock = clock;
    }

    /**
     * The nodes which are restarting right now: they loaded their configuration to start their process with it, but
     * the process isn't available yet.
     *
     * @return the IDs of the restarting nodes among the given ones
     */
    public Set<String> restartingNodes(NodeType nodeType, Collection<String> nodeIds) {
        final Map<String, NodeConfigurationState> states = configurationServices.get(nodeType).configurationStates();
        final Instant timeout = clock.instant().minus(RESTART_TIMEOUT);
        final Set<String> restarting = new TreeSet<>();
        for (final String nodeId : nodeIds) {
            final NodeConfigurationState state = states.get(nodeId);
            if (state != null && isRestarting(state, timeout)) {
                restarting.add(nodeId);
            }
        }
        return restarting;
    }

    public boolean isRestarting(NodeType nodeType, String nodeId) {
        return !restartingNodes(nodeType, List.of(nodeId)).isEmpty();
    }

    private static boolean isRestarting(NodeConfigurationState state, Instant timeout) {
        final Instant loadedAt = state.loadedAt();
        if (loadedAt == null || loadedAt.isBefore(timeout)) {
            return false;
        }
        return state.appliedAt() == null || loadedAt.isAfter(state.appliedAt());
    }

    /**
     * The parameters each of the given nodes has to be restarted for, both the ones stored for the node itself and
     * the ones shared by all nodes of the type.
     *
     * @return the names of the pending parameters keyed by node ID, with an empty set for nodes which are up to date
     */
    public Map<String, Set<String>> pendingParameters(NodeType nodeType, Collection<String> nodeIds) {
        final ManagedConfigurationService service = configurationServices.get(nodeType);
        final Map<String, NodeConfigurationState> states = service.configurationStates();
        final Collection<ManagedConfigurationEntry> clusterEntries = service.get(null).values();

        final Map<String, Set<String>> pending = new LinkedHashMap<>();
        for (final String nodeId : nodeIds) {
            final NodeConfigurationState state = states.get(nodeId);
            final Set<String> names = new TreeSet<>();
            collectPending(nodeType, clusterEntries, state, names);
            collectPending(nodeType, service.get(nodeId).values(), state, names);
            pending.put(nodeId, names);
        }
        return pending;
    }

    private void collectPending(NodeType nodeType,
                                Collection<ManagedConfigurationEntry> entries,
                                @Nullable NodeConfigurationState state,
                                Set<String> names) {
        for (final ManagedConfigurationEntry entry : entries) {
            catalog.get(nodeType, entry.name())
                    .filter(parameter -> isPending(parameter, entry, state))
                    .ifPresent(parameter -> names.add(parameter.name()));
        }
    }

    static boolean isPending(ConfigurationParameter parameter,
                             ManagedConfigurationEntry entry,
                             @Nullable NodeConfigurationState state) {
        if (!parameter.requiresRestart() || entry.changedAt() == null) {
            return false;
        }
        // A node which never reported picking up its configuration can't have picked up the change either
        return state == null || state.appliedAt() == null || entry.changedAt().isAfter(state.appliedAt());
    }
}
