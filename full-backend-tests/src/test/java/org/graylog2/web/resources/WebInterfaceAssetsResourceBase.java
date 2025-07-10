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
package org.graylog2.web.resources;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.http.HttpStatus;
import org.graylog.testing.completebackend.apis.GraylogApis;

import static io.restassured.RestAssured.given;

public abstract class WebInterfaceAssetsResourceBase {
    private final GraylogApis apis;

    protected WebInterfaceAssetsResourceBase(GraylogApis apis) {
        this.apis = apis;
    }

    private RequestSpecification backend() {
        return given()
                .baseUri(apis.backend().uri())
                .port(apis.backend().apiPort());
    }

    protected void testFrontend(String prefix) {
        final var scriptSrcs = backend()
                .get(prefix)
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .contentType(ContentType.HTML)
                .extract()
                .htmlPath()
                .<String>getList("html.body.script*.@src");

        scriptSrcs.forEach(src -> {
            backend()
                    .get(src)
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK)
                    .contentType(ContentType.JSON);
        });
    }
}
