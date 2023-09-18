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

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.FailureConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.completebackend.apis.inputs.GelfInputApi;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.not;

public class GraylogApis implements GraylogRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogApis.class);

    ObjectMapperProvider OBJECT_MAPPER_PROVIDER = new ObjectMapperProvider();
    private final GraylogBackend backend;
    private final Users users;
    private final Streams streams;
    private final Sharing sharing;
    private final GelfInputApi gelf;
    private final Search search;
    private final Indices indices;
    private final FieldTypes fieldTypes;
    private final Views views;

    public GraylogApis(GraylogBackend backend) {
        this.backend = backend;
        this.users = new Users(this);
        this.streams = new Streams(this);
        this.sharing = new Sharing(this);
        this.gelf = new GelfInputApi(this);
        this.search = new Search(this);
        this.indices = new Indices(this);
        this.fieldTypes = new FieldTypes(this);
        this.views = new Views(this);
    }

    public RequestSpecification requestSpecification() {
        return new RequestSpecBuilder().build()
                .baseUri(backend.uri())
                .port(backend.apiPort())
                .basePath("/api")
                .accept(JSON)
                .contentType(JSON)
                .header("X-Requested-By", "peterchen")
                .auth().basic("admin", "admin");
    }

    public Supplier<RequestSpecification> requestSpecificationSupplier() {
        return () -> this.requestSpecification();
    }

    public GraylogBackend backend() {
        return backend;
    }

    public Users users() {
        return users;
    }

    public Streams streams() {
        return streams;
    }

    public Sharing sharing() {
        return sharing;
    }

    public GelfInputApi gelf() {
        return gelf;
    }

    public Search search() {
        return this.search;
    }

    public Indices indices() {
        return indices;
    }

    public FieldTypes fieldTypes() {
        return this.fieldTypes;
    }

    public Views views() {
        return views;
    }

    protected RequestSpecification prefix(final Users.User user) {
        return given()
                .config(withGraylogBackendFailureConfig())
                .spec(this.requestSpecification())
                .auth().basic(user.username(), user.password())
                .when();
    }

    public ValidatableResponse postWithResource(final String url, final String resource, final int expectedResult) {
        return postWithResource(url, Users.LOCAL_ADMIN, resource, expectedResult);
    }

    public ValidatableResponse postWithResource(final String url, final Users.User user, final String resource, final int expectedResult) {
        return prefix(user)
                .body(getClass().getClassLoader().getResourceAsStream(resource))
                .post(url)
                .then()
                .log().ifStatusCodeMatches(not(expectedResult))
                .statusCode(expectedResult);
    }

    public ValidatableResponse post(final String url, final String body, final int expectedResult) {
        return post(url, Users.LOCAL_ADMIN, body, expectedResult);
    }

    public ValidatableResponse post(final String url, final Users.User user, final String body, final int expectedResult) {
        var request = prefix(user);

        if(body != null) {
            request = request.body(body);
        }

        return request
                .post(url)
                .then()
                .log().ifStatusCodeMatches(not(expectedResult))
                .statusCode(expectedResult);
    }

    public ValidatableResponse put(final String url, final String body, final int expectedResult) {
        return put(url, Users.LOCAL_ADMIN, body, expectedResult);
    }

    public ValidatableResponse put(final String url, final Users.User user, final String body, final int expectedResult) {
        var request = prefix(user);

        if(body != null) {
            request = request.body(body);
        }

        return request
                .put(url)
                .then()
                .log().ifStatusCodeMatches(not(expectedResult))
                .statusCode(expectedResult);
    }

    public ValidatableResponse get(final String url, final int expectedResult) {
        return get(url, Users.LOCAL_ADMIN, Map.of(), expectedResult);
    }

    public ValidatableResponse get(final String url, final Map<String, Object> queryParms, final int expectedResult) {
        return get(url, Users.LOCAL_ADMIN, queryParms, expectedResult);
    }

    public ValidatableResponse get(final String url, final Users.User user, final Map<String, Object> queryParms, final int expectedResult) {
        var request = prefix(user);
        for (var param: queryParms.entrySet()) {
            request = request.queryParam(param.getKey(), List.of(param.getValue()));
        }
        return request.get(url)
                .then()
                .log().ifStatusCodeMatches(not(expectedResult))
                .statusCode(expectedResult);
    }

    public ValidatableResponse delete(final String url, final int expectedResult) {
        return delete(url, Users.LOCAL_ADMIN, expectedResult);
    }

    public ValidatableResponse delete(final String url, final Users.User user, final int expectedResult) {
        return prefix(user)
                .delete(url)
                .then()
                .log().ifStatusCodeMatches(not(expectedResult))
                .statusCode(expectedResult);
    }

    private boolean errorRunningIndexer(final String logs) {
        return logs.contains("Elasticsearch cluster not available")
                || logs.contains("Elasticsearch cluster is unreachable or unhealthy");
    }

    public RestAssuredConfig withGraylogBackendFailureConfig() {
        return this.withGraylogBackendFailureConfig(500);
    }

    public RestAssuredConfig withGraylogBackendFailureConfig(final int minError) {
        return RestAssured.config()
                .objectMapperConfig(new ObjectMapperConfig().jackson2ObjectMapperFactory(
                        (type, s) -> OBJECT_MAPPER_PROVIDER.get()
                ))
                .failureConfig(FailureConfig.failureConfig().with().failureListeners(
                        (reqSpec, respSpec, resp) -> {
                            if (resp.statusCode() >= minError) {
                                final var backendLogs = this.backend.getLogs();
                                LOG.error("------------------------ Output from graylog docker container start ------------------------\n"
                                        + backendLogs
                                        + "\n------------------------ Output from graylog docker container ends  ------------------------");
                                if(errorRunningIndexer(backendLogs)) {
                                    LOG.error("------------------------ Output from indexer docker container start ------------------------\n"
                                            + this.backend.getSearchLogs()
                                            + "\n------------------------ Output from indexer docker container ends  ------------------------");
                                }
                            }
                        })
                );
    }
}
