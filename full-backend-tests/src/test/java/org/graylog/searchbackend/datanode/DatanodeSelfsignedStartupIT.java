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

import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.RetryException;
import io.restassured.response.ValidatableResponse;
import jakarta.annotation.Nonnull;
import org.graylog.testing.completebackend.ContainerizedGraylogBackend;
import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.conditions.EnabledIfSearchServer;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.restoperations.DatanodeOpensearchWait;
import org.graylog.testing.restoperations.RestOperationParameters;
import org.graylog2.security.JwtSecret;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.graylog2.security.jwt.IndexerJwtAuthTokenProvider;
import org.graylog2.storage.SearchVersion;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.concurrent.ExecutionException;

@GraylogBackendConfiguration(
        serverLifecycle = Lifecycle.CLASS,
        env = {
                @GraylogBackendConfiguration.Env(key = "GRAYLOG_DATANODE_INSECURE_STARTUP", value = "false"),
                @GraylogBackendConfiguration.Env(key = "GRAYLOG_SELFSIGNED_STARTUP", value = "true"),
                @GraylogBackendConfiguration.Env(key = "GRAYLOG_ELASTICSEARCH_HOSTS", value = ""),
        }
)
@EnabledIfSearchServer(distribution = SearchVersion.Distribution.DATANODE)
public class DatanodeSelfsignedStartupIT {


    private final Logger log = LoggerFactory.getLogger(DatanodeProvisioningIT.class);

    private static GraylogApis apis;

    @BeforeAll
    static void beforeAll(GraylogApis graylogApis) {
        apis = graylogApis;
    }

    @FullBackendTest
    public void testSelfsignedStartup() throws ExecutionException, RetryException {
        testEncryptedConnectionToOpensearch();
    }


    private int getOpensearchPort() {
        final String indexerHostAddress = apis.backend().searchServerInstance().getHttpHostAddress();
        return Integer.parseInt(indexerHostAddress.split(":")[1]);
    }

    private void testEncryptedConnectionToOpensearch() throws ExecutionException, RetryException {
        try {
            final ValidatableResponse response = new DatanodeOpensearchWait(RestOperationParameters.builder()
                    .port(getOpensearchPort())
                    .relaxedHTTPSValidation(true)
                    .jwtAuthToken(createJwtAuthToken())
                    .build())
                    .waitForNodesCount(1);

            response.assertThat().body("status", Matchers.equalTo("green"));
        } catch (Exception e) {
            log.error("Could not connect to Opensearch\n{}", apis.backend().getSearchLogs());
            throw e;
        }
    }

    @Nonnull
    private static IndexerJwtAuthToken createJwtAuthToken() {
        final IndexerJwtAuthTokenProvider provider = new IndexerJwtAuthTokenProvider(new JwtSecret(ContainerizedGraylogBackend.PASSWORD_SECRET), Duration.seconds(120), Duration.seconds(60), Duration.seconds(30), true, Clock.systemDefaultZone());
        return provider.get();
    }
}
