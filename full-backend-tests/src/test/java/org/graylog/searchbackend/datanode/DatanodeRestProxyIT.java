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
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.conditions.EnabledIfSearchServer;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog2.storage.SearchVersion;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

@GraylogBackendConfiguration(
        serverLifecycle = Lifecycle.CLASS,
        env = @GraylogBackendConfiguration.Env(key = "GRAYLOG_DATANODE_PROXY_API_ALLOWLIST", value = "false")
)
@EnabledIfSearchServer(distribution = SearchVersion.Distribution.DATANODE)
public class DatanodeRestProxyIT {

    private GraylogApis apis;

    @BeforeEach
    void setUp(GraylogApis apis) {
        this.apis = apis;
    }

    @FullBackendTest
    void testTargetAllDatanodeInstance() {
        final List<String> datanodes = apis.system().datanodes().properJSONPath().read("elements.*.hostname");

        final ValidatableResponse res = apis.get("/datanodes/all/rest/", 200);

        for (String hostname : datanodes) {
            final String jsonpath = hostname + ".opensearch.node.rest_base_url";
            final String expectedRestUrl = "http://" + hostname + ":9200";
            res.assertThat().body(jsonpath, Matchers.equalTo(expectedRestUrl));
        }
    }

    @FullBackendTest
    void testTargetAnyDatanodeInstance() {
        apis.get("/datanodes/any/rest/", 200)
                .assertThat().body("opensearch.node.node_name", Matchers.equalTo("indexer"));
    }

    @FullBackendTest
    void testTargetSpecificDatanodeInstance() {
        final List<String> datanodes = apis.system().datanodes().properJSONPath().read("elements.*.hostname");
        Assertions.assertThat(datanodes).isNotEmpty();

        final String hostname = datanodes.getFirst();
        apis.get("/datanodes/" + hostname + "/rest/", 200)
                .assertThat().body("opensearch.node.node_name", Matchers.equalTo("indexer"));

    }
}
