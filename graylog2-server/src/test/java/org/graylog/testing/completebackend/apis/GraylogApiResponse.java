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
package org.graylog.testing.completebackend.apis;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.ReadContext;
import io.restassured.response.ValidatableResponse;

public class GraylogApiResponse {
    private final ValidatableResponse validatableResponse;

    public GraylogApiResponse(ValidatableResponse validatableResponse) {
        this.validatableResponse = validatableResponse;
    }

    public ValidatableResponse validatableResponse() {
        return validatableResponse;
    }

    /**
     * @return the actual JSONPath implementation, not the groovy-style version that restassured provides by default
     */
    public ReadContext properJSONPath() {
        return com.jayway.jsonpath.JsonPath.parse(validatableResponse.extract().body().asString());
    }

    public String plainText() {
        return validatableResponse.extract().body().asString();
    }
}
