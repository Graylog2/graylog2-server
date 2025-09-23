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

import com.github.rholder.retry.RetryException;
import io.restassured.response.ValidatableResponse;
import org.assertj.core.api.Assertions;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.containermatrix.annotations.FullBackendTest;
import org.graylog.testing.containermatrix.annotations.GraylogBackendConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.time.Duration.ofSeconds;
import static org.awaitility.Awaitility.waitAtMost;

@GraylogBackendConfiguration(serverLifecycle = Lifecycle.CLASS,
                             onlyOnDataNode = true,
                             additionalConfigurationParameters = {@GraylogBackendConfiguration.ConfigurationParameter(key = "GRAYLOG_DATANODE_PROXY_API_ALLOWLIST", value = "true")}
)
public class DatanodeOpensearchProxyIT {

    private GraylogApis apis;

    @BeforeEach
    void setUp(GraylogApis apis) {
        this.apis = apis;
    }

    @FullBackendTest
    void testProxyPlaintextGet() throws ExecutionException, RetryException {
        waitAtMost(ofSeconds(30)).untilAsserted(() -> {
            final ValidatableResponse response = apis.get("/datanodes/any/opensearch/_cat/indices", 200);
            final String responseBody = response.extract().body().asString();
            Assertions.assertThat(responseBody).contains("graylog_0").contains("gl-system-events_0");
        });
    }

    @FullBackendTest
    void testProxyJsonGet() {
        waitAtMost(ofSeconds(30)).untilAsserted(() -> {
            final ValidatableResponse response = apis.get("/datanodes/any/opensearch/_mapping", 200);
            response.assertThat().body("graylog_0.mappings.properties.gl2_accounted_message_size.type", Matchers.equalTo("long"));
        });
    }

    @FullBackendTest
    void testForbiddenUrl() {
        final String message = apis.get("/datanodes/any/opensearch/_search", 400).extract().body().asString();
        Assertions.assertThat(message).contains("This request is not allowed");
    }


    @FullBackendTest
    void testNonAdminUser() {
        //HTTP 401/unauthorized for any non-admin user
        apis.get("/datanodes/any/opensearch/_search", Users.JOHN_DOE, Collections.emptyMap(), 401);
    }

    @FullBackendTest
    void testTargetSpecificDatanodeInstance() {
        final List<String> datanodes = apis.system().datanodes().properJSONPath().read("elements.*.hostname");
        Assertions.assertThat(datanodes).isNotEmpty();

        final String hostname = datanodes.iterator().next();
        apis.get("/datanodes/" + hostname + "/opensearch/_mapping", 200).assertThat().body("graylog_0.mappings.properties.gl2_accounted_message_size.type", Matchers.equalTo("long"));
    }

    @FullBackendTest
    void testQueryParameters() {
        final ValidatableResponse response = apis.get("/datanodes/any/opensearch/_cluster/settings?include_defaults=true", 200);
        response.assertThat().body("defaults.cluster.name", Matchers.equalTo("datanode-cluster"));
    }
}
