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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * Posted on the cluster event bus after configuration values have been changed through the API.
 * <p>
 * The event only says what changed, never what it changed to: {@code cluster_events} is a capped collection, so a node
 * which was down long enough misses events entirely and has to read the current state from the database anyway.
 * <p>
 * Subscribe to the regular event bus rather than the cluster event bus: replicated events are re-posted there by
 * {@link org.graylog2.events.ClusterEventService}, which data nodes run as well.
 *
 * @param nodeId {@code null} if the change applies to all nodes of the type
 */
public record ManagedConfigurationChangedEvent(@JsonProperty("node_type") NodeType nodeType,
                                               @JsonProperty("node_id") @Nullable String nodeId,
                                               @JsonProperty("names") Set<String> names) {
}
