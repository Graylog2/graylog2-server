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

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = SearchServer.DATANODE_DEV, additionalConfigurationParameters = {@ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_DATANODE_PROXY_API_ALLOWLIST", value = "true")})
public class DatanodeOpensearchProxyIT {
    private GraylogApis apis;

    @BeforeEach
    void setUp(GraylogApis apis) {
        this.apis = apis;
    }

    @ContainerMatrixTest
    void testProxyPlaintextGet() {
        final ValidatableResponse response = apis.get("/datanodes/leader/request/_cat/indices", 200);
        final String responseBody = response.extract().body().asString();
        Assertions.assertThat(responseBody).contains(".ds-gl-datanode-metrics").contains("graylog_0").contains("gl-system-events_0");
    }

    @ContainerMatrixTest
    void testProxyJsonGet() {
        final ValidatableResponse response = apis.get("/datanodes/leader/request/_mapping", 200);
        response.assertThat().body("graylog_0.mappings.properties.gl2_accounted_message_size.type", Matchers.equalTo("long"));
    }

    @ContainerMatrixTest
    void testForbiddenUrl() {
        final String message = apis.get("/datanodes/leader/request/_search", 400).extract().body().asString();
        Assertions.assertThat(message).contains("This request is not allowed");
    }

    @ContainerMatrixTest
    void testTargetSpecificDatanodeInstance() {
        final ValidatableResponse response = apis.get("/system/cluster/datanodes", 200);
        final List<String> datanodes = response.extract().body().jsonPath().getList("elements.hostname");
        Assertions.assertThat(datanodes).isNotEmpty();

        final String hostname = datanodes.iterator().next();
        apis.get("/datanodes/" + hostname + "/request/_mapping", 200).assertThat().body("graylog_0.mappings.properties.gl2_accounted_message_size.type", Matchers.equalTo("long"));
    }
}
