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
package org.graylog2.inputs;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.graylog2.cluster.Node;
import org.graylog2.cluster.NodeService;
import org.graylog2.database.filtering.ComputedFieldProvider;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.rest.resources.system.inputs.RemoteInputStatesResource;
import org.graylog2.shared.inputs.InputRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Provides filtering support for input runtime status by querying InputRegistry across all cluster nodes.
 * <p>
 * This provider enables filtering inputs by their actual runtime state (RUNNING, FAILED, STOPPED, etc.)
 * rather than just the desired_state stored in the database. It aggregates runtime status information
 * from all nodes in the cluster to provide a cluster-wide view.
 * </p>
 * <p>
 * <b>Optimizations:</b>
 * </p>
 * <ul>
 *   <li>Reads the local node's InputRegistry directly (no HTTP call to itself)</li>
 *   <li>Queries remote nodes in parallel</li>
 *   <li>Caches results for 5 seconds to avoid repeated cluster-wide fetches</li>
 * </ul>
 */
@Singleton
public class InputRuntimeStatusProvider implements ComputedFieldProvider {
    private static final Logger LOG = LoggerFactory.getLogger(InputRuntimeStatusProvider.class);
    private static final String FIELD_NAME = "runtime_status";
    private static final long CACHE_TTL_MS = 5_000;

    public static final Map<String, Set<IOState.Type>> STATUS_GROUPS = Map.of(
            "RUNNING", Set.of(IOState.Type.RUNNING),
            "NOT_RUNNING", Set.of(IOState.Type.STOPPED, IOState.Type.TERMINATED, IOState.Type.STOPPING, IOState.Type.CREATED),
            "SETUP", Set.of(IOState.Type.SETUP, IOState.Type.INITIALIZED, IOState.Type.STARTING),
            "FAILED", Set.of(IOState.Type.FAILED, IOState.Type.FAILING, IOState.Type.INVALID_CONFIGURATION)
    );

    public static final Map<String, String> STATUS_GROUP_TITLES = Map.of(
            "RUNNING", "Running",
            "NOT_RUNNING", "Not Running",
            "SETUP", "Setup Mode",
            "FAILED", "Failed"
    );

    private final NodeService nodeService;
    private final RemoteInterfaceProvider remoteInterfaceProvider;
    private final InputRegistry inputRegistry;
    private final InputService inputService;
    private final NodeId nodeId;
    private final ExecutorService executorService;

    private volatile Map<String, Set<String>> cachedStatuses;
    private volatile long cacheTimestamp = 0;

    @Inject
    public InputRuntimeStatusProvider(NodeService nodeService,
                                     RemoteInterfaceProvider remoteInterfaceProvider,
                                     InputRegistry inputRegistry,
                                      InputService inputService,
                                     NodeId nodeId,
                                     @Named("proxiedRequestsExecutorService") ExecutorService executorService) {
        this.nodeService = nodeService;
        this.remoteInterfaceProvider = remoteInterfaceProvider;
        this.inputRegistry = inputRegistry;
        this.inputService = inputService;
        this.nodeId = nodeId;
        this.executorService = executorService;
    }

    @Override
    public Set<String> getMatchingIds(String filterValue, String authToken) {
        final String key = filterValue.toUpperCase(Locale.ROOT);
        final Set<IOState.Type> targetStates = STATUS_GROUPS.get(key);
        if (targetStates == null) {
            LOG.debug("Invalid runtime status filter value: {}", filterValue);
            return Set.of();
        }

        // For NOT_RUNNING: query desired_state from DB since stopped inputs are absent from the InputRegistry
        if ("NOT_RUNNING".equals(key)) {
            final Set<String> matching = inputService.findIdsByDesiredState(IOState.Type.STOPPED);
            LOG.debug("Found {} inputs with runtime_status group={}", matching.size(), key);
            return matching;
        }


        final Set<String> targetStrings = targetStates.stream()
                .map(IOState.Type::toString)
                .collect(Collectors.toSet());

        final Map<String, Set<String>> allStatuses = getClusterStatuses(authToken);
        final Set<String> matching = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : allStatuses.entrySet()) {
            for (String status : entry.getValue()) {
                if (targetStrings.contains(status)) {
                    matching.add(entry.getKey());
                    break;
                }
            }
        }

        LOG.debug("Found {} inputs with runtime_status group={}", matching.size(), key);
        return matching;
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }

    private Map<String, Set<String>> getClusterStatuses(String authToken) {
        long now = System.currentTimeMillis();
        if (cachedStatuses != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return cachedStatuses;
        }
        synchronized (this) {
            // Double-check after acquiring lock
            if (cachedStatuses != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TTL_MS) {
                return cachedStatuses;
            }
            cachedStatuses = fetchAllClusterStatuses(authToken);
            cacheTimestamp = System.currentTimeMillis();
            return cachedStatuses;
        }
    }

    private Map<String, Set<String>> fetchAllClusterStatuses(String authToken) {
        final Map<String, Set<String>> result = new ConcurrentHashMap<>();
        final String localNodeId = nodeId.getNodeId();

        // 1. Local node: read InputRegistry directly (no HTTP)
        inputRegistry.getStatusesByInputId().forEach((inputId, status) ->
                result.computeIfAbsent(inputId, k -> ConcurrentHashMap.newKeySet()).add(status));

        // 2. Remote nodes: parallel HTTP calls
        final Map<String, Node> activeNodes = nodeService.allActive();
        final Map<String, Future<Map<String, String>>> futures = new HashMap<>();

        for (Map.Entry<String, Node> entry : activeNodes.entrySet()) {
            if (entry.getKey().equals(localNodeId)) {
                continue; // skip local node, already handled above
            }
            final Node node = entry.getValue();
            futures.put(entry.getKey(), executorService.submit(() -> {
                final RemoteInputStatesResource remote = remoteInterfaceProvider.get(
                        node, authToken, RemoteInputStatesResource.class);
                final Response<Map<String, String>> response = remote.getLocalStatuses().execute();
                if (response.isSuccessful() && response.body() != null) {
                    return response.body();
                }
                LOG.debug("Failed to get response from node {}: {}", node.getNodeId(), response.code());
                return Map.<String, String>of();
            }));
        }

        // 3. Collect results with timeout
        for (Map.Entry<String, Future<Map<String, String>>> entry : futures.entrySet()) {
            try {
                final Map<String, String> nodeStatuses = entry.getValue().get(30, TimeUnit.SECONDS);
                nodeStatuses.forEach((id, status) ->
                        result.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet()).add(status));
            } catch (Exception e) {
                LOG.debug("Error fetching input states from node {}: {}", entry.getKey(), e.getMessage());
            }
        }

        return result;
    }
}
