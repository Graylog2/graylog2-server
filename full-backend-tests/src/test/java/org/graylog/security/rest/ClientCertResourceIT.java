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
package org.graylog.security.rest;

import io.restassured.response.ValidatableResponse;
import jakarta.ws.rs.core.Response;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.SearchServer;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.junit.Ignore;

@ContainerMatrixTestsConfiguration(searchVersions = SearchServer.DATANODE_DEV)
public class ClientCertResourceIT {

    private final GraylogApis api;

    public ClientCertResourceIT(GraylogApis api) {
        this.api = api;
    }

    @Ignore
    @ContainerMatrixTest
    void generateClientCert() {

        //TODO: this needs selfsigned_setup configured, so the security plugin in the opensearch inside datanode is
        // activated and avaliable.

        final ValidatableResponse responseCa = api.post("/ca/create", """
                {"organization":"graylog integration tests"}
                """, Response.Status.CREATED.getStatusCode());

        // these roles are supported: all_access,security_rest_api_access,readall
        final ValidatableResponse response = api.post("/ca/clientcert", """
                {
                    "principal": "admin",
                    "role": "readall",
                    "password": "asdfgh",
                    "certificate_lifetime": "P6M"
                }
                """, Response.Status.OK.getStatusCode());
        System.out.println(response);
    }
}
