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
package org.graylog.testing.completebackend.apis.inputs;

import com.google.common.collect.ImmutableMap;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.completebackend.apis.GraylogRestApi;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static io.restassured.RestAssured.given;

public final class GelfInputApi implements GraylogRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(GelfInputApi.class);
    private static final int DEFAULT_GELF_HTTP_PORT = 12201;
    private final RequestSpecification requestSpecification;
    private final GraylogBackend backend;

    public GelfInputApi(RequestSpecification requestSpecification, GraylogBackend backend) {
        this.requestSpecification = requestSpecification;
        this.backend = backend;
    }


    public PortBoundGelfInputApi createGelfHttpInput() {
        return createGelfHttpInput(DEFAULT_GELF_HTTP_PORT);
    }

    public PortBoundGelfInputApi createGelfHttpInput(int gelfHttpPort) {
        return createGelfHttpInput(backend.mappedPortFor(gelfHttpPort), gelfHttpPort);
    }

    public PortBoundGelfInputApi createGelfHttpInput(int mappedPort, int gelfHttpPort) {

        final ArrayList<Integer> inputs = given()
                .spec(requestSpecification)
                .expect()
                .response()
                .statusCode(200)
                .when()
                .get("/system/inputstates")
                .body().jsonPath().get("states.message_input.attributes.port");

        if (!inputs.contains(gelfHttpPort)) {
            InputCreateRequest request = InputCreateRequest.create(
                    "Integration test GELF input",
                    GELFHttpInput.class.getName(),
                    true,
                    ImmutableMap.of("bind_address", "0.0.0.0", "port", gelfHttpPort),
                    null);

            given()
                    .spec(requestSpecification)
                    .body(request)
                    .expect().response().statusCode(201)
                    .when()
                    .post("/system/inputs");
        }

        waitForGelfInputOnPort(mappedPort);

        return new PortBoundGelfInputApi(this, mappedPort);
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

    private RequestSpecification gelfEndpoint(int mappedPort) {
        return given()
                .spec(this.requestSpecification)
                .basePath("/gelf")
                .port(mappedPort);
    }

    public void postMessage(int mappedPort,
                            @SuppressWarnings("SameParameterValue") String messageJson) {
        gelfEndpoint(mappedPort)
                .body(messageJson)
                .expect().response().statusCode(202)
                .when()
                .post();
    }
}
