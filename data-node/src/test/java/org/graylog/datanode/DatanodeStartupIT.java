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

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import org.apache.http.NoHttpResponseException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.net.SocketException;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DatanodeStartupIT {

    private Network network;
    private GenericContainer mongo;
    private GenericContainer datanode;

    @BeforeEach
    void setUp() {

        network = Network.newNetwork();

        mongo = new GenericContainer("mongo:5.0")
                .withNetwork(network)
                .withNetworkAliases("mongodb")
                .withExposedPorts(27017);

        datanode = new GenericContainer(new ImageFromDockerfile()
                .withDockerfile(Path.of("./Dockerfile")))
                .withExposedPorts(8999, 9200)
                .withNetwork(network)
                .dependsOn(mongo)
                .waitingFor(new HostPortWaitStrategy());

        mongo.start();
        datanode.start();
    }

    @AfterEach
    void tearDown() {
        datanode.stop();
        mongo.stop();
        network.close();
    }

    @Test
    void testDatanodeStartup() throws ExecutionException, RetryException {

        final Retryer<ValidatableResponse> retryer = RetryerBuilder.<ValidatableResponse>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(60))
                .retryIfException(input -> input instanceof NoHttpResponseException)
                .retryIfException(input -> input instanceof SocketException)
                .retryIfResult(input -> !input.extract().body().path("process.info.status").equals("AVAILABLE"))
                .build();

        final Integer datanodeRestApiPort = datanode.getMappedPort(8999);

        retryer.call(() -> this.getStatus(datanodeRestApiPort))
                .assertThat()
                .body("process.info.node_name", Matchers.equalTo("node1"))
                .body("process.info.pid", Matchers.notNullValue())
                .body("process.info.user", Matchers.equalTo("opensearch"));
    }

    private ValidatableResponse getStatus(Integer mappedPort) {
        return RestAssured.given()
                .get("http://localhost:" + mappedPort)
                .then();
    }
}
