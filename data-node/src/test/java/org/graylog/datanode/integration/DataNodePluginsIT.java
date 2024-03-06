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
package org.graylog.datanode.integration;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog.datanode.testinfra.DatanodeTestExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

@ExtendWith(DatanodeTestExtension.class)
public class DataNodePluginsIT {
    private static final Logger LOG = LoggerFactory.getLogger(DataNodePluginsIT.class);
    private final DatanodeContainerizedBackend backend;

    public DataNodePluginsIT(DatanodeContainerizedBackend backend) {
        this.backend = backend;
    }

    @Test
    void ensureUnneededPluginsAreNotLoaded() throws Exception {
        final var opensearchRestPort = backend.getOpensearchRestPort();
        final var baseUrl = "http://localhost:" + opensearchRestPort;
        try {
            waitForOpensearch(baseUrl);

            given()
                    .get(baseUrl + "/_cat/plugins")
                    .then()
                    .statusCode(200)
                    .body(
                            Matchers.not(Matchers.containsString("opensearch-alerting")),
                            Matchers.not(Matchers.containsString("opensearch-custom-codecs")),
                            Matchers.not(Matchers.containsString("opensearch-geospatial")),
                            Matchers.not(Matchers.containsString("opensearch-knn")),
                            Matchers.not(Matchers.containsString("opensearch-neural-search")),
                            Matchers.not(Matchers.containsString("opensearch-notifications")),
                            Matchers.not(Matchers.containsString("opensearch-notifications-core")),
                            Matchers.not(Matchers.containsString("opensearch-performance-analyzer")),
                            Matchers.not(Matchers.containsString("opensearch-reports-scheduler")),
                            Matchers.not(Matchers.containsString("opensearch-security-analytics")),
                            Matchers.not(Matchers.containsString("opensearch-sql"))
                    );
        } catch (Exception exception) {
            LOG.error("DataNode Container logs follow:\n" + backend.getLogs());
            throw exception;
        }
    }

    private void waitForOpensearch(String baseUrl) throws ExecutionException, RetryException {
        final var retryer = RetryerBuilder.newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .retryIfException(e -> e instanceof IOException)
                .build();

        retryer.call(() -> given()
                .get(baseUrl)
                .then()
                .statusCode(200));
    }
}
