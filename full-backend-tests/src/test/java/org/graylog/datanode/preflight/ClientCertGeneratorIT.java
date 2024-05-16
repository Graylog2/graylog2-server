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
package org.graylog.datanode.preflight;

import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.graylog.datanode.DatanodeProvisioningIT;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.jupiter.api.BeforeEach;

import static io.restassured.RestAssured.given;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS, searchVersions = SearchServer.DATANODE_DEV)
public class ClientCertGeneratorIT {
    private GraylogApis apis;

    @BeforeEach
    void setUp(GraylogApis apis) {
        this.apis = apis;
    }

    @ContainerMatrixTest
    public void testClientCertGeneration() {
        createSelfSignedCA();
        configureAutomaticCertRenewalPolicy();

        final ValidatableResponse certResponse = apis.post("/ca/clientcert", """
                {
                "principal" : "user1",
                "role" : "all_access",
                "password" : "12test34"
                }
                """, 201);

        System.out.println(certResponse.extract().body().asPrettyString());
    }

    private ValidatableResponse createSelfSignedCA() {
        return apis.post("/ca/create", "{\"organization\":\"Graylog CA\"}", 201);
    }

    private ValidatableResponse configureAutomaticCertRenewalPolicy() {
        return apis.put("/system/cluster_config/org.graylog2.plugin.certificates.RenewalPolicy","{\"mode\":\"Automatic\",\"certificate_lifetime\":\"P30D\"}", HttpStatus.SC_ACCEPTED);
    }
}
