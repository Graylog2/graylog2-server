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
package org.graylog2.configuration.overrides;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Cluster config holding the configuration overrides for all nodes of a {@link NodeType}, keyed by parameter name.
 */
public record NodeTypeConfigurationOverrides(
        @Nullable @JsonProperty("server") Map<String, ConfigurationOverrideValue> server,
        @Nullable @JsonProperty("datanode") Map<String, ConfigurationOverrideValue> datanode
) {
    public NodeTypeConfigurationOverrides {
        server = server == null ? Map.of() : Map.copyOf(server);
        datanode = datanode == null ? Map.of() : Map.copyOf(datanode);
    }

    public static NodeTypeConfigurationOverrides empty() {
        return new NodeTypeConfigurationOverrides(Map.of(), Map.of());
    }

    public Map<String, ConfigurationOverrideValue> forNodeType(NodeType nodeType) {
        return switch (Objects.requireNonNull(nodeType, "nodeType")) {
            case SERVER -> server;
            case DATANODE -> datanode;
        };
    }

    public NodeTypeConfigurationOverrides withNodeType(NodeType nodeType, Map<String, ConfigurationOverrideValue> overrides) {
        return switch (Objects.requireNonNull(nodeType, "nodeType")) {
            case SERVER -> new NodeTypeConfigurationOverrides(overrides, datanode);
            case DATANODE -> new NodeTypeConfigurationOverrides(server, overrides);
        };
    }
}
