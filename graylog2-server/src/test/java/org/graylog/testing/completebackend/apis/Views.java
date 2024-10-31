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

import org.graylog.plugins.views.search.searchtypes.Sort;

import java.util.Locale;

import static io.restassured.RestAssured.given;

public class Views implements GraylogRestApi {
    private final GraylogApis api;

    public Views(GraylogApis api) {
        this.api = api;
    }

    /**
     * Caution, returns proper JSONPath, not the restassured/groovy version
     */
    public GraylogApiResponse getAll(int page, int size, String sort, Sort.Order order) {
        final String path = String.format(Locale.ROOT,
                "/views?page=%d&per_page=%d&sort=%s&order=%s",
                page, size, sort, order.name().toLowerCase(Locale.ROOT));
        return new GraylogApiResponse(given()
                .spec(api.requestSpecification())
                .when()
                .get(path)
                .then());
    }

    public GraylogApiResponse getOne(String id) {
        return new GraylogApiResponse(given()
                .spec(api.requestSpecification())
                .when()
                .get("/views/" + id)
                .then());
    }
}
