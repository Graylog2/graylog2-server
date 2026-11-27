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

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationCatalogTest {

    private static ConfigurationParameter parameter(NodeType nodeType, String name, String category, int order) {
        return ConfigurationParameter.builder(nodeType, name, name)
                .category(category)
                .order(order)
                .build();
    }

    @Test
    void ordersByCategoryThenOrderThenName() {
        final var catalog = new ConfigurationCatalog(Set.of(
                parameter(NodeType.SERVER, "second", "A", 20),
                parameter(NodeType.SERVER, "third", "B", 10),
                parameter(NodeType.SERVER, "first", "A", 10)));

        assertThat(catalog.get(NodeType.SERVER))
                .extracting(ConfigurationParameter::name)
                .containsExactly("first", "second", "third");
    }

    @Test
    void keepsNodeTypesApart() {
        final var catalog = new ConfigurationCatalog(Set.of(
                parameter(NodeType.SERVER, "shared_name", "A", 10),
                parameter(NodeType.DATANODE, "shared_name", "A", 10),
                parameter(NodeType.DATANODE, "only_datanode", "A", 20)));

        assertThat(catalog.get(NodeType.SERVER)).extracting(ConfigurationParameter::name)
                .containsExactly("shared_name");
        assertThat(catalog.get(NodeType.DATANODE)).extracting(ConfigurationParameter::name)
                .containsExactly("shared_name", "only_datanode");
        assertThat(catalog.get(NodeType.SERVER, "only_datanode")).isEmpty();
        assertThat(catalog.get(NodeType.DATANODE, "only_datanode")).isPresent();
    }

    @Test
    void filtersByScope() {
        final var catalog = new ConfigurationCatalog(Set.of(
                ConfigurationParameter.builder(NodeType.SERVER, "per_node", "Per node").build(),
                ConfigurationParameter.builder(NodeType.SERVER, "cluster_wide", "Cluster wide")
                        .scope(ParameterScope.CLUSTER).build()));

        assertThat(catalog.get(NodeType.SERVER, ParameterScope.NODE))
                .extracting(ConfigurationParameter::name).containsExactly("per_node");
        assertThat(catalog.get(NodeType.SERVER, ParameterScope.CLUSTER))
                .extracting(ConfigurationParameter::name).containsExactly("cluster_wide");
    }

    @Test
    void refusesDuplicateRegistrations() {
        final var duplicates = Set.of(
                parameter(NodeType.SERVER, "a_parameter", "A", 10),
                parameter(NodeType.SERVER, "a_parameter", "A", 20));

        assertThatThrownBy(() -> new ConfigurationCatalog(duplicates))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("a_parameter");
    }

    @Test
    void registeredParametersAreValid() {
        final var catalog = new ConfigurationCatalog(Set.copyOf(DefaultConfigurationParameters.all()));

        assertThat(catalog.get(NodeType.DATANODE)).isNotEmpty();
        assertThat(catalog.get(NodeType.DATANODE, "opensearch_heap")).isPresent();
    }
}
