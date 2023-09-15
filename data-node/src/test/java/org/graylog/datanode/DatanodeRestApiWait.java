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

public class DatanodeRestApiWait {

    private static final Logger LOG = LoggerFactory.getLogger(DatanodeRestApiWait.class);
    private static final int ATTEMPTS_COUNT = 160;

    private final int datanodePort;
    private KeyStore truststore;
    private String restApiUsername;
    private String restApiPassword;
    private String lastRecordedResponse;

    private DatanodeRestApiWait(int opensearchPort) {
        this.datanodePort = opensearchPort;
    }

    public static DatanodeRestApiWait onPort(int datanodePort) {
        return new DatanodeRestApiWait(datanodePort);
    }

    public DatanodeRestApiWait withTruststore(KeyStore truststore) {
        this.truststore = truststore;
        return this;
    }

    public DatanodeRestApiWait withBasicAuth(String username, String password) {
        this.restApiUsername = username;
        this.restApiPassword = password;
        return this;
    }

    public ValidatableResponse waitForAvailableStatus() throws ExecutionException, RetryException {
        return waitForDatanode(
                input -> !input.extract().body().path("opensearch.node.state").equals("AVAILABLE")
        );
    }


    @SafeVarargs
    private ValidatableResponse waitForDatanode(Predicate<ValidatableResponse>... predicates) throws ExecutionException, RetryException {

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

                if (restApiUsername != null && restApiPassword != null) {
                    req.auth().basic(restApiUsername, restApiPassword);
                }

                return req.get(formatDatanodeRestUrl())
                        .then();
            });
        } catch (Exception e) {
            if (lastRecordedResponse != null) {
                LOG.warn("Last recorded opensearch response, waiting for {}: {}", formatDatanodeRestUrl(), lastRecordedResponse);
            }
            throw e;
        }
    }

    private String formatCurlConnectionInfo() {
        List<String> builder = new LinkedList<>();
        builder.add("curl -k"); // trust self-signed certs
        if(restApiUsername != null && restApiPassword != null) {
            builder.add("-u \"" + restApiUsername + ":" + restApiPassword + "\"");
        }
        builder.add(formatDatanodeRestUrl());
        return String.join(" ", builder);
    }

    @NotNull
    private String formatDatanodeRestUrl() {
        return "https://localhost:" + datanodePort;
    }

    private String serializeResponse(ExtractableResponse<Response> extract) {
        final String body = extract.body().asString();
        final String contentType = extract.contentType();
        final String headers = extract.headers().toString();
        return String.format(Locale.ROOT, "\nContent-type: %s\nheaders: %s\nbody: %s", contentType, headers, body);
    }
}
