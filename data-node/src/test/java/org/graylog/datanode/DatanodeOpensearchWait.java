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

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.http.NoHttpResponseException;
import org.graylog.datanode.integration.DatanodeSecuritySetupIT;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class DatanodeOpensearchWait {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeOpensearchWait.class);
    private static final int ATTEMPTS_COUNT = 160;

    private final int opensearchPort;
    private KeyStore truststore;
    private String jwtToken;
    private String lastRecordedResponse;

    private DatanodeOpensearchWait(int opensearchPort) {
        this.opensearchPort = opensearchPort;
    }

    public static DatanodeOpensearchWait onPort(int opensearchPort) {
        return new DatanodeOpensearchWait(opensearchPort);
    }

    public DatanodeOpensearchWait withTruststore(KeyStore truststore) {
        this.truststore = truststore;
        return this;
    }

    public DatanodeOpensearchWait withJwtAuth(String jwtToken) {
        this.jwtToken = jwtToken;
        return this;
    }

    public ValidatableResponse waitForGreenStatus() throws ExecutionException, RetryException {
        return waitForOpensearch(
                input -> !input.extract().body().path("status").equals("green"));
    }

    public ValidatableResponse waitForNodesCount(int countOfNodes) throws ExecutionException, RetryException {
        return waitForOpensearch(
                input -> !input.extract().body().path("discovered_cluster_manager").equals(true),
                input -> !input.extract().body().path("status").equals("green"),
                input -> !input.extract().body().path("number_of_nodes").equals(countOfNodes)
        );
    }


    @SafeVarargs
    private ValidatableResponse waitForOpensearch(Predicate<ValidatableResponse>... predicates) throws ExecutionException, RetryException {

        if(LOG.isInfoEnabled()) {
            LOG.info("cURL request: " + formatCurlConnectionInfo());
        }

        //noinspection UnstableApiUsage
        final RetryerBuilder<ValidatableResponse> builder = RetryerBuilder.<ValidatableResponse>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(ATTEMPTS_COUNT))
                .retryIfException(input -> input instanceof NoHttpResponseException)
                .retryIfException(input -> input instanceof SocketException)
                .retryIfException(input -> input instanceof SSLHandshakeException) // may happen before SSL is configured properly
                .retryIfResult(input -> !input.extract().contentType().startsWith("application/json"))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        if (attempt.hasResult() && attempt.getResult() instanceof ValidatableResponse response) {
                            lastRecordedResponse = serializeResponse(response.extract());
                        } else if (attempt.getExceptionCause() != null) {
                            lastRecordedResponse = attempt.getExceptionCause().getMessage();
                        }
                    }
                });

        for (Predicate<ValidatableResponse> predicate : predicates) {
            builder.retryIfResult(predicate::test);
        }

        final Retryer<ValidatableResponse> retryer = builder
                .build();
        try {
            return retryer.call(() -> {
                final RequestSpecification req = RestAssured.given()
                        .accept(ContentType.JSON);

                Optional.ofNullable(truststore).ifPresent(ts -> req.trustStore(truststore));

                if (jwtToken != null) {
                    req.header( "Authorization", "Bearer " + jwtToken);
                }

                return req.get(formatClusterHeathUrl())
                        .then();
            });
        } catch (ExecutionException | RetryException e) {
            if (lastRecordedResponse != null) {
                LOG.warn("Last recorded opensearch response, waiting for {}: {}", formatClusterHeathUrl(), lastRecordedResponse);
            }
            printShardsInfo();
            throw e;
        }
    }

    private String formatCurlConnectionInfo() {
        List<String> builder = new LinkedList<>();
        builder.add("curl -k"); // trust self-signed certs
        if(jwtToken != null) {
            builder.add("-H \"Authorization: Bearer " + jwtToken + "\"");
        }
        builder.add(formatClusterHeathUrl());
        return String.join(" ", builder);
    }

    private void printShardsInfo() {
        try {
            final RequestSpecification req = RestAssured.given()
                    .accept(ContentType.JSON);

            Optional.ofNullable(truststore).ifPresent(ts -> req.trustStore(truststore));

            if (jwtToken != null) {
                req.header( "Authorization", "Bearer " + jwtToken);
            }

            final String shardsResponse = req.get("https://localhost:" + opensearchPort + "/_cat/shards?v=true&h=index,shard,prirep,state,node,unassigned.reason&s=state")
                    .then().extract().body().asString();

            LOG.warn("Cluster shards status: {}", shardsResponse);
        } catch (Exception e) {
            LOG.warn("Failed to obtain shard info", e);
        }
    }

    @NotNull
    private String formatClusterHeathUrl() {
        return "https://localhost:" + opensearchPort + "/_cluster/health";
    }

    private String serializeResponse(ExtractableResponse<Response> extract) {
        final String body = extract.body().asString();
        final String contentType = extract.contentType();
        final String headers = extract.headers().toString();
        return String.format(Locale.ROOT, "\nContent-type: %s\nheaders: %s\nbody: %s", contentType, headers, body);
    }
}
