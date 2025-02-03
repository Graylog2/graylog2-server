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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rholder.retry.RetryException;
import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.FailureConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.RandomStringUtils;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.completebackend.apis.inputs.GelfInputApi;
import org.graylog.testing.completebackend.apis.inputs.Inputs;
import org.graylog.testing.completebackend.apis.inputs.PortBoundGelfInputApi;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.not;

public class GraylogApis implements GraylogRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(GraylogApis.class);
    private static final ObjectMapperProvider OBJECT_MAPPER_PROVIDER = new ObjectMapperProvider();

    private final GraylogBackend backend;
    private final Users users;
    private final Streams streams;
    private final Sharing sharing;
    private final GelfInputApi gelf;
    private final Search search;
    private final Indices indices;
    private final FieldTypes fieldTypes;
    private final Views views;
    private final SystemApi system;
    private final EventNotifications eventNotifications;
    private final EventDefinitions eventDefinitions;
    private final Dashboards dashboards;
    private final Pipelines pipelines;
    private final Inputs inputs;

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
        this.system = new SystemApi(this);
        this.eventNotifications = new EventNotifications(this);
        this.eventDefinitions = new EventDefinitions(this);
        this.dashboards = new Dashboards(this);
        this.pipelines = new Pipelines(this);
        this.inputs = new Inputs(this);
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

    public SystemApi system() {
        return system;
    }

    public EventNotifications eventsNotifications() {
        return eventNotifications;
    }

    public EventDefinitions eventDefinitions() {
        return eventDefinitions;
    }

    public Dashboards dashboards() {
        return dashboards;
    }

    public Pipelines pipelines() {
        return pipelines;
    }

    public Inputs inputs() {
        return inputs;
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

    public ValidatableResponse post(final String url, final int expectedResult) {
        return post(url, Users.LOCAL_ADMIN, "", expectedResult);
    }

    public ValidatableResponse post(final String url, final Object body, final int expectedResult) throws JsonProcessingException {
        final var objectMapper = OBJECT_MAPPER_PROVIDER.get();
        return post(url, Users.LOCAL_ADMIN, objectMapper.writeValueAsString(body), expectedResult);
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

    public class SearchEnvironment implements Closeable {
        private final Map<String, Object> MANDATORY_MESSAGE_FIELDS = Map.of(
                "source", "test-environment",
                "host", "test-environment"
        );

        private final String randomId;
        private final String streamId;
        private final String indexSetId;
        private final PortBoundGelfInputApi gelfPort;
        private final ObjectMapper objectMapper;

        private SearchEnvironment(String randomId, String streamId, String indexSetId, PortBoundGelfInputApi gelfPort) {
            this.randomId = randomId;
            this.streamId = streamId;
            this.indexSetId = indexSetId;
            this.gelfPort = gelfPort;
            this.objectMapper = new ObjectMapperProvider().get();
        }

        public void ingestMessage(Map<String, Object> message) {
            final var messageWithTag = ImmutableMap.builder()
                    .putAll(MANDATORY_MESSAGE_FIELDS)
                    .putAll(message)
                    .put("test-environment", randomId)
                    .build();
            try {
                this.gelfPort.postMessage(objectMapper.writeValueAsString(messageWithTag));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error serializing message for ingestion: ", e);
            }
        }

        public void waitForFieldTypes(String... fields) {
            fieldTypes().waitForFieldTypeDefinitions(Set.of(streamId), fields);
        }

        public ValidatableResponse executePivot(Pivot pivot) {
            return search().executePivot(pivot, "", Set.of(streamId));
        }

        public void waitForMessages(Collection<String> messages) {
            search().waitForMessages(messages, RelativeRange.allTime(), Set.of(streamId));
        }

        @Override
        public void close() {
            streams().deleteStream(streamId);
            indices().deleteIndexSet(indexSetId, true);
        }
    }

    public SearchEnvironment createEnvironment(PortBoundGelfInputApi gelfPort) throws ExecutionException, RetryException {
        final var randomId = RandomStringUtils.secure().next(8, false, true);
        final var indexSetId = this.indices().createIndexSet("Test Environment " + randomId, "An index set for tests", "searchenvironment" + randomId);
        this.indices().waitForIndexNames(indexSetId);
        final var streamId = this.streams().createStream("Test Stream " + randomId, indexSetId, true, DefaultStreamMatches.REMOVE, Streams.StreamRule.exact(randomId, "test-environment", false));

        return new SearchEnvironment(randomId, streamId, indexSetId, gelfPort);
    }
}
