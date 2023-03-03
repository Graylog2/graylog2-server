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
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog.datanode.testinfra.DatanodeTestExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@ExtendWith(DatanodeTestExtension.class)
public class DatanodeStartupIT {
    private static final Logger LOG = LoggerFactory.getLogger(DatanodeStartupIT.class);

    private final DatanodeContainerizedBackend backend;

    public DatanodeStartupIT(DatanodeContainerizedBackend backend) {
        this.backend = backend;
    }

    @Test
    void testDatanodeStartup() throws ExecutionException, RetryException {

        try {
            final Retryer<ValidatableResponse> retryer = RetryerBuilder.<ValidatableResponse>newBuilder()
                    .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                    .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                    .retryIfException(input -> input instanceof NoHttpResponseException)
                    .retryIfException(input -> input instanceof SocketException)
                    .retryIfResult(input -> !input.extract().body().path("process.info.status").equals("AVAILABLE"))
                    .build();

             final Integer datanodeRestApiPort = backend.getDatanodeRestPort();

                    retryer.call(() -> this.getStatus(datanodeRestApiPort))
                    .assertThat()
                    .body("process.info.node_name", Matchers.equalTo("node1"))
                    .body("process.info.pid", Matchers.notNullValue())
                    .body("process.info.user", Matchers.equalTo("opensearch"));
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
