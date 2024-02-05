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
package org.graylog.datanode.restoperations;

import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.http.NoHttpResponseException;

import javax.net.ssl.SSLHandshakeException;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public abstract class WaitingRestOperation extends RestOperation {

    private String lastRecordedResponse;

    protected WaitingRestOperation(RestOperationParameters parameters) {
        super(parameters);
    }

    @SafeVarargs
    final ValidatableResponse waitForResponse(String url, Predicate<ValidatableResponse>... predicates) throws ExecutionException, RetryException {

        if (LOG.isInfoEnabled()) {
            LOG.info("cURL request: " + formatCurlConnectionInfo(parameters.port(), url));
        }

        //noinspection UnstableApiUsage
        final RetryerBuilder<ValidatableResponse> builder = RetryerBuilder.<ValidatableResponse>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(parameters.attempts_count()))
                .retryIfException(input -> input instanceof NoHttpResponseException)
                .retryIfException(input -> input instanceof SocketException)
                .retryIfException(input -> input instanceof SSLHandshakeException) // may happen before SSL is configured properly
                .retryIfResult(r -> r.extract().statusCode() != 200)
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

                Optional.ofNullable(parameters.truststore()).ifPresent(ts -> req.trustStore(parameters.truststore()));

                parameters.addAuthorizationHeaders(req);

                return req.get(formatUrl(parameters.port(), url))
                        .then();
            });
        } catch (Exception e) {
            if (lastRecordedResponse != null) {
                LOG.warn("Last recorded opensearch response, waiting for {}: {}", formatUrl(parameters.port(), url), lastRecordedResponse);
            }
            throw e;
        }
    }

}
