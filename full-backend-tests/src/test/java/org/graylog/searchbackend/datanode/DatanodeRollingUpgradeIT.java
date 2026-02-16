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

import jakarta.annotation.Nonnull;
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;

// TODO:fix the test
//@GraylogBackendConfiguration(
//        serverLifecycle = Lifecycle.CLASS,
//        env = {
//                @GraylogBackendConfiguration.Env(key = "GRAYLOG_DATANODE_INSECURE_STARTUP", value = "false"),
//                @GraylogBackendConfiguration.Env(key = "GRAYLOG_SELFSIGNED_STARTUP", value = "true"),
//                @GraylogBackendConfiguration.Env(key = "GRAYLOG_ELASTICSEARCH_HOSTS", value = "")
//        }
//)
//@EnabledIfSearchServer(distribution = SearchVersion.Distribution.DATANODE)
public class DatanodeRollingUpgradeIT {

    private static GraylogApis apis;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) {
        apis = graylogApis;
    }

    //@FullBackendTest
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
