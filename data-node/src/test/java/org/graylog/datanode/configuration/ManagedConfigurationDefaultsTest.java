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
package org.graylog.datanode.configuration;

import org.graylog.datanode.Configuration;
import org.graylog.datanode.opensearch.configuration.beans.impl.OpensearchCommonConfigurationBean;
import org.graylog2.configuration.managed.ConfigurationParameter;
import org.graylog2.configuration.managed.DefaultConfigurationParameters;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The catalog repeats the defaults of the data node configuration, since it is declared in the server. They are
 * shown while nothing is stored and no node reported a value, so they have to stay in line.
 */
class ManagedConfigurationDefaultsTest {
    private final Configuration configuration = new Configuration();

    private static String defaultOf(String name) {
        return DefaultConfigurationParameters.all().stream()
                .filter(parameter -> parameter.name().equals(name))
                .map(ConfigurationParameter::defaultValue)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void catalogDefaultsMatchTheDataNodeConfiguration() {
        assertThat(defaultOf("opensearch_heap")).isEqualTo(configuration.getOpensearchHeap());
        assertThat(defaultOf("node_search_cache_size")).isEqualTo(configuration.getNodeSearchCacheSize());
        assertThat(defaultOf("opensearch_indices_query_bool_max_clause_count"))
                .isEqualTo(String.valueOf(configuration.getIndicesQueryBoolMaxClauseCount()));
        assertThat(defaultOf("node_roles"))
                .isEqualTo(String.join(",", OpensearchCommonConfigurationBean.getNodeRoles(configuration)));
    }
}
