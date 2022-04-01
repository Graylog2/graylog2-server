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
import io.restassured.specification. RequestSpecification;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.rest.models.system.inputs.responses.InputStateSummary;
import org.graylog2.rest.models.system.inputs.responses.InputStatesList;
import org.graylog2.rest.models.system.inputs.responses.InputSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static io.restassured.RestAssured.given;

public final class GelfInputUtils {

    private static final Logger LOG = LoggerFactory.getLogger(GelfInputUtils.class);

    private GelfInputUtils() {
    }

    public static void createGelfHttpInput(int mappedPort, int gelfHttpPort, RequestSpecification requestSpecification) {
        final boolean anyInputListensOnGelfPort = getInputStates(requestSpecification).states().stream()
                .anyMatch(input -> Optional.of(input)
                        .map(InputStateSummary::messageInput)
                        .map(InputSummary::attributes)
                        .map(attrs -> attrs.get("port"))
                        .filter(port -> port.equals(gelfHttpPort))
                        .isPresent()
                );
        if (!anyInputListensOnGelfPort) {
            createInput(requestSpecification, GELFHttpInput.class, ImmutableMap.of("bind_address", "0.0.0.0", "port", gelfHttpPort), "Integration test GELF input");
        }
        waitForGelfInputOnPort(mappedPort, requestSpecification);
    }

    public static InputStatesList getInputStates(RequestSpecification requestSpecification) {
        return given()
                .spec(requestSpecification)
                .expect()
                .response()
                .statusCode(200)
                .when()
                .get("/system/inputstates")
                .body().as(InputStatesList.class);
    }

    private static void waitForGelfInputOnPort(int mappedPort, RequestSpecification requestSpecification) {
        WaitUtils.waitFor(
                () -> gelfInputIsListening(mappedPort, requestSpecification),
                "Timed out waiting for GELF input listening on port " + mappedPort);
    }

    private static boolean gelfInputIsListening(int mappedPort, RequestSpecification requestSpecification) {
        try {
            gelfEndpoint(mappedPort, requestSpecification)
                    .expect().response().statusCode(200)
                    .when()
                    .options();
            LOG.info("GELF input listening on port {}", mappedPort);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static RequestSpecification gelfEndpoint(int mappedPort, RequestSpecification requestSpecification) {
        return given()
                .spec(requestSpecification)
                .basePath("/gelf")
                .port(mappedPort);
    }

    public static void postMessage(int mappedPort,
                                   @SuppressWarnings("SameParameterValue") String messageJson,
                                   RequestSpecification requestSpecification) {
        gelfEndpoint(mappedPort, requestSpecification)
                .body(messageJson)
                .expect().response().statusCode(202)
                .when()
                .post();
    }

    public static void createInput(RequestSpecification requestSpec, Class<? extends MessageInput> inputClass, ImmutableMap<String, Object> configuration, String title) {
        InputCreateRequest request = InputCreateRequest.create(
                title,
                inputClass.getName(),
                true,
                configuration,
                null);

        given()
                .spec(requestSpec)
                .body(request)
                .expect().response().statusCode(201)
                .when()
                .post("/system/inputs");
    }
}
