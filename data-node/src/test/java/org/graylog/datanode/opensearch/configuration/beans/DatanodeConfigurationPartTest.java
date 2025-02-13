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
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class DatanodeConfigurationPartTest {

    @Test
    void testConfigBuild() {
        final DatanodeConfigurationPart configurationPart = DatanodeConfigurationPart.builder()
                .addNodeRole("cluster_manager")
                .addNodeRole("data")
                .addNodeRole("search")
                .keystoreItems(Collections.singletonMap("foo", "bar"))
                .properties(Collections.singletonMap("reindex.remote.allowlist", "localhost:9201"))
                .systemProperty("file.encoding", "utf-8")
                .systemProperty("java.home", "/jdk")
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

        Assertions.assertThat(configurationPart.systemProperties())
                .hasSize(2)
                .containsEntry("file.encoding", "utf-8")
                .containsEntry("java.home", "/jdk");
    }
}
