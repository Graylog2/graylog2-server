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
import jakarta.inject.Singleton;
import org.graylog2.database.filtering.ComputedFieldProvider;
import org.graylog2.inputs.persistence.InputStateService;
import org.graylog2.plugin.IOState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Provides filtering support for input runtime status by querying MongoDB.
 * <p>
 * Runtime state is persisted to MongoDB by {@link org.graylog2.inputs.InputStateListener},
 * enabling efficient cluster-wide queries by state without HTTP fan-out.
 */
@Singleton
public class InputRuntimeStatusProvider implements ComputedFieldProvider {
    private static final Logger LOG = LoggerFactory.getLogger(InputRuntimeStatusProvider.class);
    private static final String FIELD_NAME = "runtime_status";

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

    private final InputStateService runtimeStateService;
    private final InputService inputService;

    @Inject
    public InputRuntimeStatusProvider(InputStateService runtimeStateService,
                                      InputService inputService) {
        this.runtimeStateService = runtimeStateService;
        this.inputService = inputService;
    }

    @Override
    public Set<String> getMatchingIds(String filterValue, String authToken) {
        final String key = filterValue.toUpperCase(Locale.ROOT);
        final Set<IOState.Type> targetStates = STATUS_GROUPS.get(key);
        if (targetStates == null) {
            LOG.debug("Invalid runtime status filter value: {}", filterValue);
            return Set.of();
        }

        // For NOT_RUNNING: query desired_state from DB since stopped inputs have no runtime state
        if ("NOT_RUNNING".equals(key)) {
            final Set<String> matching = inputService.findIdsByDesiredState(IOState.Type.STOPPED);
            LOG.debug("Found {} inputs with runtime_status group={}", matching.size(), key);
            return matching;
        }

        final Set<String> matching = new HashSet<>();
        for (IOState.Type type : targetStates) {
            matching.addAll(runtimeStateService.getByState(type));
        }

        LOG.debug("Found {} inputs with runtime_status group={}", matching.size(), key);
        return matching;
    }

    @Override
    public String getFieldName() {
        return FIELD_NAME;
    }
}
