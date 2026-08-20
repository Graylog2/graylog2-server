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
package org.graylog.plugins.views.search.views.widgets.aggregation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkVisualizationConfigDTOTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider(
            getClass().getClassLoader(),
            Set.of(new NamedType(NetworkVisualizationConfigDTO.class, NetworkVisualizationConfigDTO.NAME)))
            .get();

    @Test
    void deserializesNetworkVisualizationConfigFromAggregationConfig() throws Exception {
        final String json = """
                {
                  "row_pivots": [],
                  "column_pivots": [],
                  "series": [],
                  "sort": [],
                  "visualization": "network",
                  "visualization_config": {
                    "color_scale": "YlOrRd",
                    "reverse_scale": true
                  }
                }
                """;

        final AggregationConfigDTO config = objectMapper.readValue(json, AggregationConfigDTO.class);

        assertThat(config.visualization()).isEqualTo("network");
        assertThat(config.visualizationConfig())
                .isInstanceOf(NetworkVisualizationConfigDTO.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(NetworkVisualizationConfigDTO.class))
                .satisfies(networkConfig -> {
                    assertThat(networkConfig.colorScale()).isEqualTo("YlOrRd");
                    assertThat(networkConfig.reverseScale()).isTrue();
                });
    }

    @Test
    void roundtripsNetworkVisualizationConfig() throws Exception {
        final NetworkVisualizationConfigDTO original = NetworkVisualizationConfigDTO.builder()
                .colorScale("Bluered")
                .reverseScale(true)
                .build();

        final String serialized = objectMapper.writeValueAsString(original);
        final NetworkVisualizationConfigDTO deserialized =
                objectMapper.readValue(serialized, NetworkVisualizationConfigDTO.class);

        assertThat(deserialized).isEqualTo(original);
    }
}
