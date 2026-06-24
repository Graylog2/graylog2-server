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

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.conditions.EnabledIfSearchServer;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog2.storage.SearchVersion;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static io.restassured.RestAssured.given;
import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.waitAtMost;

@GraylogBackendConfiguration(
        serverLifecycle = Lifecycle.CLASS,
        env = @GraylogBackendConfiguration.Env(key = "GRAYLOG_DATANODE_PROXY_API_ALLOWLIST", value = "false"))
@EnabledIfSearchServer(distribution = SearchVersion.Distribution.DATANODE)
public class DatanodeOpensearchProxyDisabledAllowlistIT {
    private GraylogApis apis;

    @BeforeEach
    void setUp(GraylogApis apis) {
        this.apis = apis;

        waitAtMost(ofSeconds(120)).pollInterval(ofSeconds(1)).until(() -> {
            final List<String> statuses = given()
                    .spec(apis.requestSpecification())
                    .get("/system/cluster/datanodes")
                    .jsonPath()
                    .getList("elements.datanode_status");
            return statuses != null && statuses.contains("AVAILABLE");
        });
    }

    @FullBackendTest
    void testProtectedPath() {
        // this requests the /_search of the underlying opensearch. By default, it's disabled and should return HTTP 400
        // only if we disable the allowlist it should be accessible
        apis.get("/datanodes/any/opensearch/_search", 200)
                .assertThat()
                .body("_shards.successful", Matchers.greaterThanOrEqualTo(1));
    }
}
