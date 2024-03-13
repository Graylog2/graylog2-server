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
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeEach;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = SearchServer.DATANODE_DEV, additionalConfigurationParameters = {@ContainerMatrixTestsConfiguration.ConfigurationParameter(key = "GRAYLOG_DATANODE_PROXY_API_ALLOWLIST", value = "false")})
public class DatanodeOpensearchProxyDisabledAllowlistIT {
    private GraylogApis apis;

    @BeforeEach
    void setUp(GraylogApis apis) {
        this.apis = apis;
    }

    @ContainerMatrixTest
    void testProtectedPath() {
        // this requests the /_search of the underlying opensearch. By default, it's disabled and should return HTTP 400
        // only if we disable the allowlist it should be accessible
        final ValidatableResponse response = apis.get("/datanodes/any/request/_search", 200);
        System.out.println(response.extract().body().asString());
    }
}
