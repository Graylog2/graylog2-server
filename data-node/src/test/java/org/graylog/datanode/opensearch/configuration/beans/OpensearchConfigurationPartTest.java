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
package org.graylog.datanode.opensearch.configuration.beans;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class OpensearchConfigurationPartTest {

    @Test
    void testConfigBuild() {
        final OpensearchConfigurationPart configurationPart = OpensearchConfigurationPart.builder()
                .addNodeRole("cluster_manager")
                .addNodeRole("data")
                .addNodeRole("search")
                .keystoreItems(Collections.singletonMap("foo", "bar"))
                .properties(Collections.singletonMap("reindex.remote.allowlist", "localhost:9201"))
                .build();

        Assertions.assertThat(configurationPart.nodeRoles())
                .hasSize(3)
                .contains("cluster_manager", "data", "search");

        Assertions.assertThat(configurationPart.keystoreItems())
                .hasSize(1)
                .containsEntry("foo", "bar");

        Assertions.assertThat(configurationPart.properties())
                .hasSize(1)
                .containsEntry("reindex.remote.allowlist", "localhost:9201");
    }
}
