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
package org.graylog.elasticsearch.e2e;

import com.google.common.collect.ImmutableMap;
import io.restassured.specification.RequestSpecification;
import org.glassfish.jersey.internal.util.Producer;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.backenddriver.SearchDriver.searchAllMessages;
import static org.junit.Assert.fail;

abstract class ElasticsearchE2E {
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchE2E.class);

    static final int GELF_HTTP_PORT = 12201;

    private final GraylogBackend sut;
    private final RequestSpecification requestSpec;

    public ElasticsearchE2E(GraylogBackend sut, RequestSpecification requestSpec) {
        this.sut = sut;
        this.requestSpec = requestSpec;
    }

    @Test
    void inputMessageCanBeSearched() {
        int mappedPort = sut.mappedPortFor(GELF_HTTP_PORT);

        createGelfHttpInput(mappedPort);

        postMessage(mappedPort, "{\"short_message\":\"Hello there\", \"host\":\"example.org\", \"facility\":\"test\"}");

        List<String> messages = searchForAllMessages();

        assertThat(messages).containsExactly("Hello there");
    }

    private void createGelfHttpInput(int mappedPort) {
        InputCreateRequest request = InputCreateRequest.create(
                "KILL ME",
                GELFHttpInput.class.getName(),
                true,
                ImmutableMap.of("bind_address", "0.0.0.0", "port", GELF_HTTP_PORT),
                null);

        given()
                .spec(requestSpec)
                .body(request)
                .expect().response().statusCode(201)
                .when()
                .post("/system/inputs");

        waitForGelfInputOnPort(mappedPort);
    }

    private void waitForGelfInputOnPort(int mappedPort) {
        waitFor(
                () -> gelfInputIsListening(mappedPort),
                "Timed out waiting for GELF input listening on port " + mappedPort);
    }

    private boolean gelfInputIsListening(int mappedPort) {
        try {
            gelfEndpoint(mappedPort)
                    .expect().response().statusCode(200)
                    .when()
                    .options();
            LOG.info("GELF input listening on port {}", mappedPort);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void postMessage(int mappedPort, @SuppressWarnings("SameParameterValue") String messageJson) {
        gelfEndpoint(mappedPort)
                .body(messageJson)
                .expect().response().statusCode(202)
                .when()
                .post();
    }

    private List<String> searchForAllMessages() {
        List<String> messages = new ArrayList<>();

        waitFor(() -> captureMessages(messages::addAll), "Timed out waiting for messages to be present");

        return messages;
    }

    private boolean captureMessages(Consumer<List<String>> messagesCaptor) {
        List<String> messages = searchAllMessages(requestSpec);
        if (!messages.isEmpty()) {
            messagesCaptor.accept(messages);
            return true;
        }
        return false;
    }

    private void waitFor(Producer<Boolean> predicate, String timeoutErrorMessage) {
        int timeOutMs = 5000;
        int msPassed = 0;
        int waitMs = 500;
        while (msPassed <= timeOutMs) {
            if (predicate.call()) {
                return;
            }
            msPassed += waitMs;
            wait(waitMs);
        }
        fail(timeoutErrorMessage);
    }

    private void wait(int waitMs) {
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private RequestSpecification gelfEndpoint(int mappedPort) {
        return given()
                .spec(requestSpec)
                .basePath("/gelf")
                .port(mappedPort);
    }
}
