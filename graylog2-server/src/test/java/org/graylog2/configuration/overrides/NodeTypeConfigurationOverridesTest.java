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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NodeTypeConfigurationOverridesTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    void nodeTypeUsesLowerCaseValues() throws Exception {
        assertThat(objectMapper.writeValueAsString(NodeType.DATANODE)).isEqualTo("\"datanode\"");
        assertThat(objectMapper.readValue("\"server\"", NodeType.class)).isEqualTo(NodeType.SERVER);
        assertThat(NodeType.fromString("DataNode")).isEqualTo(NodeType.DATANODE);
        assertThatThrownBy(() -> NodeType.fromString("forwarder")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void replacesOverridesOfOneNodeType() {
        final var value = new ConfigurationOverrideValue("5", Instant.parse("2026-10-09T12:00:00Z"), "admin");
        final var overrides = NodeTypeConfigurationOverrides.empty()
                .withNodeType(NodeType.SERVER, Map.of("a", value))
                .withNodeType(NodeType.DATANODE, Map.of("b", value));

        assertThat(overrides.withNodeType(NodeType.SERVER, Map.of()).forNodeType(NodeType.SERVER)).isEmpty();
        assertThat(overrides.withNodeType(NodeType.SERVER, Map.of()).forNodeType(NodeType.DATANODE)).containsOnlyKeys("b");
    }

    @Test
    void roundTripsThroughJson() throws Exception {
        final var value = new ConfigurationOverrideValue("5", Instant.parse("2026-10-09T12:00:00Z"), "admin");
        final var overrides = new NodeTypeConfigurationOverrides(Map.of("processbuffer_processors", value), null);

        final var json = objectMapper.writeValueAsString(overrides);

        assertThat(objectMapper.readValue(json, NodeTypeConfigurationOverrides.class)).isEqualTo(overrides);
        assertThat(objectMapper.readValue("{}", NodeTypeConfigurationOverrides.class))
                .isEqualTo(NodeTypeConfigurationOverrides.empty());
    }
}
