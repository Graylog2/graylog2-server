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
package org.graylog.datanode;

import io.restassured.response.ValidatableResponse;
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = SearchServer.DATANODE_DEV, additionalConfigurationParameters = {@ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_DATANODE_PROXY_API_ALLOWLIST", value = "false")})
public class DatanodeRestProxyIT {

    private GraylogApis apis;

    @BeforeEach
    void setUp(GraylogApis apis) {
        this.apis = apis;
    }

    @ContainerMatrixTest
    void testTargetAllDatanodeInstance() {
        final List<String> datanodes = apis.system().datanodes().properJSONPath().read("elements.*.hostname");

        final ValidatableResponse res = apis.get("/datanodes/all/rest/", 200);

        for (String hostname : datanodes) {
            final String jsonpath = hostname + ".opensearch.node.rest_base_url";
            final String expectedRestUrl = "http://" + hostname + ":9200";
            res.assertThat().body(jsonpath, Matchers.equalTo(expectedRestUrl));
        }
    }

    @ContainerMatrixTest
    void testTargetAnyDatanodeInstance() {
        apis.get("/datanodes/any/rest/", 200)
                .assertThat().body("opensearch.node.node_name", Matchers.equalTo("indexer"));
    }

    @ContainerMatrixTest
    void testTargetSpecificDatanodeInstance() {
        final List<String> datanodes = apis.system().datanodes().properJSONPath().read("elements.*.hostname");
        Assertions.assertThat(datanodes).isNotEmpty();

        final String hostname = datanodes.iterator().next();
        apis.get("/datanodes/" + hostname + "/rest/", 200)
                .assertThat().body("opensearch.node.node_name", Matchers.equalTo("indexer"));

    }
}
