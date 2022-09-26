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
package org.graylog.plugins.views;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.rest.FieldTypesForQueryRequest;
import org.graylog.plugins.views.search.rest.FieldTypesForStreamsRequest;
import org.graylog.plugins.views.search.rest.QueryDTO;
import org.graylog.plugins.views.search.searchfilters.model.InlineQueryStringSearchFilter;
import org.graylog.testing.completebackend.GraylogBackend;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog.testing.utils.SearchUtils;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;


@ContainerMatrixTestsConfiguration
public class FieldTypesResourceIT {

    private final RequestSpecification requestSpec;
    private final GraylogBackend backend;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProvider().get();

    public FieldTypesResourceIT(RequestSpecification requestSpec, GraylogBackend backend) {
        this.requestSpec = requestSpec;
        this.backend = backend;
    }

    @BeforeAll
    public void setUp() {
        backend.importElasticsearchFixture("field_type_test_messages.json", FieldTypesResourceIT.class);

        SearchUtils.waitForFieldTypeDefinition(requestSpec, "medieval_field");
        SearchUtils.waitForFieldTypeDefinition(requestSpec, "telegram_field");
        SearchUtils.waitForFieldTypeDefinition(requestSpec, "modern_field");
    }

    @ContainerMatrixTest
    void http400OnEmptyBody() {
        given()
                .spec(requestSpec)
                .when()
                .post("/views/fields/byQuery")
                .then()
                .statusCode(400);
    }

    @ContainerMatrixTest
    void returnsFieldsForAllMessages() throws Exception {
        final FieldTypesForQueryRequest request = FieldTypesForQueryRequest.Builder.builder()
                .query(createQueryDTOBuilder().build())
                .parameters(ImmutableSet.of())
                .fallback(FieldTypesForStreamsRequest.Builder.builder().build())
                .build();
        final Response response = given()
                .spec(requestSpec)
                .when()
                .accept(ContentType.JSON)
                .body(OBJECT_MAPPER.writeValueAsString(request))
                .queryParam("size", 100)
                .queryParam("useSampler", false)
                .queryParam("sampleSize", 100)
                .post("/views/fields/byQuery");

        final ValidatableResponse validatableResponse = response.then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        containsFields(validatableResponse, Set.of("message", "source", "timestamp", "streams", "medieval_field", "telegram_field", "modern_field"));
        doesNotContainFields(validatableResponse, Set.of("carramba"));

    }

    @ContainerMatrixTest
    void returnsProperFieldsForMessagesFilteredByQueryString() throws Exception {
        final FieldTypesForQueryRequest request = FieldTypesForQueryRequest.Builder.builder()
                .query(createQueryDTOBuilder()
                        .query(ElasticsearchQueryString.of("message:message"))
                        .build())
                .parameters(ImmutableSet.of())
                .fallback(FieldTypesForStreamsRequest.Builder.builder().build())
                .build();
        final Response response = given()
                .spec(requestSpec)
                .when()
                .accept(ContentType.JSON)
                .body(OBJECT_MAPPER.writeValueAsString(request))
                .queryParam("size", 100)
                .queryParam("useSampler", false)
                .queryParam("sampleSize", 100)
                .post("/views/fields/byQuery");

        final ValidatableResponse validatableResponse = response.then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        containsFields(validatableResponse, Set.of("message", "source", "timestamp", "streams", "telegram_field", "modern_field"));
        doesNotContainFields(validatableResponse, Set.of("medieval_field"));

    }

    @ContainerMatrixTest
    void ignoresSearchFiltersWithoutALicense() throws Exception {
        final FieldTypesForQueryRequest request = FieldTypesForQueryRequest.Builder.builder()
                .query(createQueryDTOBuilder()
                        .query(ElasticsearchQueryString.of("_exists_:telegram_field")) //will be used
                        .filters(List.of(
                                InlineQueryStringSearchFilter.builder().queryString("_exists_:medieval_field").build() //will be ignored
                        ))
                        .build())
                .parameters(ImmutableSet.of())
                .fallback(FieldTypesForStreamsRequest.Builder.builder().build())
                .build();
        final Response response = given()
                .spec(requestSpec)
                .when()
                .accept(ContentType.JSON)
                .body(OBJECT_MAPPER.writeValueAsString(request))
                .queryParam("size", 100)
                .queryParam("useSampler", false)
                .queryParam("sampleSize", 100)
                .post("/views/fields/byQuery");

        final ValidatableResponse validatableResponse = response.then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);

        containsFields(validatableResponse, Set.of("message", "source", "timestamp", "streams", "telegram_field"));
        doesNotContainFields(validatableResponse, Set.of("modern_field", "medieval_field"));

    }

    private void containsFields(final ValidatableResponse validatableResponse, final Collection<String> fieldNames) {
        fieldNames.forEach(
                fieldName -> validatableResponse.body("find { it.name == '" + fieldName + "' }", notNullValue())
        );
    }

    private void doesNotContainFields(final ValidatableResponse validatableResponse, final Collection<String> fieldNames) {
        fieldNames.forEach(
                fieldName -> validatableResponse.body("find { it.name == '" + fieldName + "' }", nullValue())
        );
    }

    private QueryDTO.Builder createQueryDTOBuilder() {
        return QueryDTO.builder()
                .searchTypes(Set.of())
                .filters(List.of())
                .id("query id")
                .timerange(RelativeRange.allTime())
                .query(ElasticsearchQueryString.of("*"));
    }

}
