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
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.apache.http.NoHttpResponseException;
import org.assertj.core.api.Assertions;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import java.net.SocketException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Verifies that {@code OpensearchProcessImpl#checkConfiguredHeap()} correctly detects the container's actual
 * cgroup memory limit and raises the heap size warning when the configured opensearch heap (the default of
 * 1g is used here) is far smaller than the memory available to the container.
 */
public class DatanodeHeapWarningIT {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeHeapWarningIT.class);

    // well above the default 1g opensearch heap, so the container-aware memory check flags the mismatch
    private static final long CONTAINER_MEMORY_LIMIT_BYTES = 8L * 1024 * 1024 * 1024;
    private static final String HEAP_WARNING_LOG_MESSAGE = "times more available memory than the heap size configured for this data node";

    private Network network;
    private MongoDBTestService mongoDB;
    private DatanodeContainerizedBackend backend;

    @BeforeEach
    void setUp() {
        network = Network.newNetwork();
        mongoDB = MongoDBTestService.createStarted(network);

        backend = new DatanodeContainerizedBackend(network, mongoDB, "datanode", datanodeContainer ->
                datanodeContainer.withCreateContainerCmdModifier(cmd ->
                        cmd.getHostConfig().withMemory(CONTAINER_MEMORY_LIMIT_BYTES)));

        try {
            backend.start();
        } catch (Exception e) {
            LOG.warn(backend.getLogs());
            throw e;
        }
    }

    @AfterEach
    void tearDown() {
        backend.stop();
        mongoDB.close();
        network.close();
    }

    @Test
    void testHeapSizeWarningTriggeredByContainerMemoryLimit() throws ExecutionException, RetryException {
        try {
            final Retryer<ValidatableResponse> retryer = RetryerBuilder.<ValidatableResponse>newBuilder()
                    .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                    .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                    .retryIfException(input -> input instanceof NoHttpResponseException)
                    .retryIfException(input -> input instanceof SocketException)
                    .retryIfResult(input -> !input.extract().body().path("opensearch.node.state").equals("AVAILABLE"))
                    .build();

            retryer.call(() -> getStatus(backend.getDatanodeRestPort()));

            Assertions.assertThat(backend.getLogs()).contains(HEAP_WARNING_LOG_MESSAGE);
        } catch (RetryException retryException) {
            LOG.error("DataNode Container logs follow:\n" + backend.getLogs());
            throw retryException;
        }
    }

    private ValidatableResponse getStatus(Integer mappedPort) {
        return RestAssured.given()
                .get("http://localhost:" + mappedPort)
                .then();
    }
}
