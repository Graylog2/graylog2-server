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
package org.graylog2.indexer.fieldtypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.rholder.retry.RetryException;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Streams;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@GraylogBackendConfiguration()
public class FieldTypeMappingsIT {
    private static GraylogApis api;

    @BeforeAll
    static void init(GraylogApis graylogApis) {
        api = graylogApis;
    }

    record FieldTypeChangeRequest(@JsonProperty("index_sets")
                                  Set<String> indexSetsIds,
                                  @JsonProperty("field")
                                  String fieldName,
                                  @JsonProperty("type")
                                  String type,
                                  @JsonProperty("rotate")
                                  boolean rotateImmediately) {

    }

    @FullBackendTest
    void changeFieldTypeFromStringToIp() throws ExecutionException, RetryException {
        changeFieldType("custom-mappings", "field-type-mappings-test", "source_ip", "192.168.1.1", "ip");
    }

    @FullBackendTest
    void changeFieldTypeFromStringToGeoPoint() throws ExecutionException, RetryException {
        // geo_point is a multi-word physical type, which has to be reported as "geo_point", not "geopoint"
        changeFieldType("geo-mappings","field-type-mappings-geo-test", "location", "52.52,13.40", "geo-point");
    }

    private void changeFieldType(String indexPrefix, String testId, String fieldName, String fieldValue, String newType) throws ExecutionException, RetryException {
        var indexSet = api.indices().createIndexSet("Field Type Mappings Test " + testId, "Testing custom field type mapping", indexPrefix);
        var stream = api.streams().createStream("Field Type Mappings Stream " + testId, indexSet, Streams.StreamRule.exact(testId, "test_id", false));
        var gelfInput = api.gelf().createGelfHttpInput();
        waitForNewIndex(indexSet, indexPrefix + "_0");

        gelfInput.postMessage(message(testId, fieldName, fieldValue, "2019-07-23 09:53:08.175"));

        api.search().waitForMessage(testId);
        var previousType = new ArrayList<>(api.fieldTypes().waitForFieldTypeDefinitions(fieldName));
        assertThat(previousType.get(0)).isNotNull()
                .satisfies(fieldType -> assertThat(fieldType.type()).isEqualTo(FieldTypeMapper.STRING_TYPE));

        given()
                .config(api.withGraylogBackendFailureConfig())
                .spec(api.requestSpecification())
                .when()
                .body(new FieldTypeChangeRequest(Set.of(indexSet), fieldName, newType, true))
                .put("/system/indices/mappings")
                .then()
                .statusCode(200);

        waitForNewIndex(indexSet, indexPrefix + "_1");

        gelfInput.postMessage(message(testId, fieldName, fieldValue, "2023-07-23 09:53:08.175"));

        api.waitFor(() -> {
            var fieldTypes = api.fieldTypes().getFieldTypes(AbsoluteRange.create("2023-01-01T00:00:00.000Z", "2024-01-01T00:00:00.000Z"), Set.of(stream));
            var field = fieldTypes.stream().filter(type -> type.name().equals(fieldName)).findFirst();

            return field.map(type -> type.type().type().equals(newType)).orElse(false);
        }, "Waiting for field type to change to `" + newType + "` timed out.", Duration.ofSeconds(60));
    }

    private void waitForNewIndex(String indexSetId, String indexName) {
        api.waitFor(() -> api.get("/system/indexer/indices/" + indexSetId + "/list ", 200)
                .extract()
                .body()
                .asString()
                .contains(indexName), "Waiting for new index after rotation timed out.", Duration.ofMinutes(3));
    }

    private String message(String testId, String fieldName, String fieldValue, String timestamp) {
        return String.format(Locale.ROOT, """
                {
                "short_message":"%1$s",
                "test_id": "%1$s",
                "source":"example.org",
                "%2$s": "%3$s",
                "timestamp": "%4$s",
                "level":3
                }""", testId, fieldName, fieldValue, timestamp);
    }
}
