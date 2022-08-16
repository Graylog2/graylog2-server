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

import io.restassured.path.json.JsonPath;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import static io.restassured.RestAssured.given;

public class EmailUtils {

    public static JsonPath waitForMessage(URI endpointURI, final String searchQuery, Duration timeout) {
        return WaitUtils.waitForObject(() -> {
            final JsonPath searchResponse = given()
                    .get(endpointURI.toString() + "/api/v2/search?kind=containing&query=" + searchQuery)
                    .body()
                    .jsonPath();

            if (searchResponse.getInt("count") >= 1) {
                return Optional.of(searchResponse);
            } else {
                return Optional.empty();
            }
        }, "Timed out waiting for a message", timeout);
    }
}
