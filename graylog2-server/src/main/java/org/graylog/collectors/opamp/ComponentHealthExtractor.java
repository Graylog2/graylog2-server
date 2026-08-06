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
package org.graylog.collectors.opamp;

import opamp.proto.Opamp;
import org.apache.commons.lang3.StringUtils;
import org.graylog.collectors.db.ComponentHealthDTO;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts an OpAMP component-health tree to the representation stored with a collector instance.
 * The extractor bounds agent-controlled data by retaining at most three levels and 128 nodes,
 * including the root. It also truncates component names to 256 Unicode code points and status and
 * error text to 4,096 Unicode code points.
 */
final class ComponentHealthExtractor {
    // The level and node limits include the root health component.
    private static final int MAX_COMPONENT_NAME_LENGTH = 256;
    private static final int MAX_LEVELS = 3;
    private static final int MAX_NODES = 128;
    private static final int MAX_TEXT_LENGTH = 4 * 1024;

    ComponentHealthDTO extract(Opamp.ComponentHealth health) {
        final var budget = new ExtractionBudget();
        return extract(health, 1, budget);
    }

    private ComponentHealthDTO extract(Opamp.ComponentHealth health,
                                       int level,
                                       ExtractionBudget budget) {
        final var builder = ComponentHealthDTO.builder().healthy(health.getHealthy());

        if (health.getStartTimeUnixNano() > 0) {
            builder.startTime(Instant.ofEpochSecond(0, health.getStartTimeUnixNano()));
        }
        final var lastError = truncate(health.getLastError(), MAX_TEXT_LENGTH);
        if (StringUtils.isNotBlank(lastError)) {
            builder.lastError(lastError);
        }
        final var status = truncate(health.getStatus(), MAX_TEXT_LENGTH);
        if (StringUtils.isNotBlank(status)) {
            builder.status(status);
        }
        if (health.getStatusTimeUnixNano() > 0) {
            builder.statusTime(Instant.ofEpochSecond(0, health.getStatusTimeUnixNano()));
        }

        final Map<String, ComponentHealthDTO> components = new LinkedHashMap<>();
        if (level < MAX_LEVELS) {
            // Protobuf map order is undefined, so the retained subset can vary when the node limit is reached.
            // Do not sort here: stopping at the budget avoids traversing and sorting the full agent-controlled map.
            for (final var entry : health.getComponentHealthMapMap().entrySet()) {
                final var componentName = truncate(entry.getKey(), MAX_COMPONENT_NAME_LENGTH);
                // Keep the first component health if two truncated names aren't unique anymore.
                if (components.containsKey(componentName)) {
                    continue;
                }
                if (!budget.tryConsumeNode()) {
                    break;
                }
                components.put(componentName, extract(entry.getValue(), level + 1, budget));
            }
        }
        builder.components(components);

        return builder.build();
    }

    /**
     * Truncates without splitting a Unicode surrogate pair. Commons Lang's
     * {@link StringUtils#truncate(String, int)} limits UTF-16 code units, while these limits are
     * defined in Unicode code points.
     */
    private static String truncate(String value, int maxCodePoints) {
        if (value.codePointCount(0, value.length()) <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }

    private static final class ExtractionBudget {
        // The root is always retained, so only the remaining node slots need to be tracked.
        private int remainingNodes = MAX_NODES - 1;

        private boolean tryConsumeNode() {
            if (remainingNodes == 0) {
                return false;
            }
            remainingNodes--;
            return true;
        }
    }
}
