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
package org.graylog.testing.utils;

import com.google.common.collect.ImmutableMap;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static io.restassured.RestAssured.given;

public final class GelfInputUtils {

    private static final Logger LOG = LoggerFactory.getLogger(GelfInputUtils.class);

    private GelfInputUtils() {
    }

    public static void createGelfHttpInput(int mappedPort, int gelfHttpPort, GraylogApis api) {

        final ArrayList<Integer> inputs = given()
                .spec(api.requestSpecification())
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
                    .spec(api.requestSpecification())
                    .body(request)
                    .expect().response().statusCode(201)
                    .when()
                    .post("/system/inputs");
        }

        waitForGelfInputOnPort(mappedPort, api);
    }

    private static void waitForGelfInputOnPort(int mappedPort, GraylogApis api) {
        WaitUtils.waitFor(
                () -> gelfInputIsListening(mappedPort, api),
                "Timed out waiting for GELF input listening on port " + mappedPort);
    }

    private static boolean gelfInputIsListening(int mappedPort, GraylogApis api) {
        try {
            gelfEndpoint(mappedPort, api)
                    .expect().response().statusCode(200)
                    .when()
                    .options();
            LOG.info("GELF input listening on port {}", mappedPort);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static RequestSpecification gelfEndpoint(int mappedPort, GraylogApis api) {
        return given()
                .spec(api.requestSpecification())
                .basePath("/gelf")
                .port(mappedPort);
    }

    public static void postMessage(int mappedPort,
                                   @SuppressWarnings("SameParameterValue") String messageJson,
                                   GraylogApis api) {
        gelfEndpoint(mappedPort, api)
                .body(messageJson)
                .expect().response().statusCode(202)
                .when()
                .post();
    }
}
