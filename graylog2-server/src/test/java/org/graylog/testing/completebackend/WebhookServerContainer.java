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
package org.graylog.testing.completebackend;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.RestAssured;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WebhookServerContainer extends ExternalResource implements AutoCloseable, WebhookServerInstance {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookServerContainer.class);
    public static final int REQUEST_ACCEPT_PORT = 8000;
    public static final int API_PORT = 8001;

    public static final String IMAGE_WORKING_DIR = "/usr/share/graylog/webhook/";
    public static final String CONTAINER_ALIAS = "webhook-tester";

    private final GenericContainer<?> container;

    public WebhookServerContainer(GenericContainer<?> container) {
        this.container = container;
    }

    public static WebhookServerContainer createStarted(Network network) {
        final GenericContainer<?> genericContainer = new GenericContainer<>(DockerImageName.parse("node:alpine"));
        genericContainer.withNetwork(network);
        genericContainer.withNetworkAliases(CONTAINER_ALIAS);
        genericContainer.addExposedPorts(REQUEST_ACCEPT_PORT, API_PORT);
        genericContainer.withCopyFileToContainer(MountableFile.forClasspathResource("webhook.js"), IMAGE_WORKING_DIR + "webhook.js");
        genericContainer.withCommand("node " + IMAGE_WORKING_DIR + "webhook.js");
        genericContainer.withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()));
        genericContainer.start();
        genericContainer.waitingFor(new HttpWaitStrategy().forPath("/").forPort(API_PORT).withStartupTimeout(Duration.ofSeconds(10)));
        LOG.debug("Webhook tester server started");
        return new WebhookServerContainer(genericContainer);
    }


    @Override
    public URI getContainerizedCollectorURI() {
        return URI.create("http://" + CONTAINER_ALIAS + ":" + REQUEST_ACCEPT_PORT);
    }

    @Override
    public URI getMappedCollectorURI() {
        return URI.create("http://" + container.getHost() + ":" + container.getMappedPort(REQUEST_ACCEPT_PORT));
    }

    public URI getMappedApiURI() {
        return URI.create("http://" + container.getHost() + ":" + container.getMappedPort(API_PORT));
    }

    @Override
    public List<WebhookRequest> allRequests() {
        final WebhookRequest[] webhookRequests = RestAssured.given()
                .get(getMappedApiURI())
                .then()
                .statusCode(200)
                .extract().body().as(WebhookRequest[].class);
        return List.of(webhookRequests);
    }

    @Override
    public List<WebhookRequest> waitForRequests(Predicate<WebhookRequest> predicate) throws ExecutionException, RetryException {
        final Retryer<List<WebhookRequest>> retryer = RetryerBuilder.<List<WebhookRequest>>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(500, TimeUnit.MILLISECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(120))
                .retryIfResult(List::isEmpty)
                .build();
        return retryer.call(() -> allRequests().stream().filter(predicate).collect(Collectors.toList()));
    }

    @Override
    public void close() {
        if (this.container != null) {
            this.container.close();
        }
    }
}
