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
package org.graylog.searchbackend.datanode;

import io.restassured.response.ValidatableResponse;
import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import java.util.List;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = SearchServer.DATANODE_DEV, additionalConfigurationParameters = {@ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_DATANODE_INSECURE_STARTUP", value = "false"), @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_SELFSIGNED_STARTUP", value = "true"), @ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_ELASTICSEARCH_HOSTS", value = ""),})
public class DatanodeRollingUpgradeIT {

    private final GraylogApis apis;

    public DatanodeRollingUpgradeIT(GraylogApis apis) {
        this.apis = apis;
    }

    @ContainerMatrixTest
    public void testClusterStatus() {
        final GraylogApiResponse response = getDatanodeClusterStatus();

        Assertions.assertThat(getDatanodeClusterStatus())
                .satisfies(res -> Assertions.assertThat((String) res.properJSONPath().read("$.cluster_state.status")).isEqualTo("GREEN"))
                .satisfies(res -> Assertions.assertThat((String) res.properJSONPath().read("$.cluster_state.shard_replication")).isEqualTo("ALL"))
                .satisfies(res -> {
                    final String serverVersion = res.properJSONPath().read("$.server_version.version");
                    List<String> datanodeVersion = response.properJSONPath().read("$.up_to_date_nodes.*.datanode_version");
                    Assertions.assertThat(datanodeVersion).hasSize(1).contains(serverVersion);
                });

        apis.post("/datanodes/upgrade/replication/stop", 200);

        Assertions.assertThat(getDatanodeClusterStatus())
                .satisfies(res -> Assertions.assertThat((String) res.properJSONPath().read("$.cluster_state.status")).isEqualTo("GREEN"))
                .satisfies(res -> Assertions.assertThat((String) res.properJSONPath().read("$.cluster_state.shard_replication")).isEqualTo("PRIMARIES"));

        apis.post("/datanodes/upgrade/replication/start", 200);

        Assertions.assertThat(getDatanodeClusterStatus())
                .satisfies(res -> Assertions.assertThat((String) res.properJSONPath().read("$.cluster_state.status")).isEqualTo("GREEN"))
                .satisfies(res -> Assertions.assertThat((String) res.properJSONPath().read("$.cluster_state.shard_replication")).isEqualTo("ALL"));
    }

    @Nonnull
    private GraylogApiResponse getDatanodeClusterStatus() {
        return new GraylogApiResponse(apis.get("/datanodes/upgrade/status", 200));
    }

}
