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
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Streams;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@ContainerMatrixTestsConfiguration()
public class FieldTypeMappingsIT {
    private static final String INDEX_PREFIX = "custom-mappings";
    private final GraylogApis api;

    public FieldTypeMappingsIT(GraylogApis api) {
        this.api = api;
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

    @ContainerMatrixTest
    void changeFieldTypeFromStringToIp() {
        var indexSet = api.indices().createIndexSet("Field Type Mappings Test", "Testing custom field type mapping", INDEX_PREFIX);
        var stream = api.streams().createStream("Field Type Mappings Stream", indexSet, Streams.StreamRule.exact("field-type-mappings-test", "test-id", false));
        var gelfInput = api.gelf().createGelfHttpInput();
        waitForNewIndex(indexSet, INDEX_PREFIX + "_0");

        gelfInput.postMessage(messageWithTimestamp("2019-07-23 09:53:08.175"));

        api.search().waitForMessage("field-type-mappings-test");
        var previousType = new ArrayList<>(api.fieldTypes().waitForFieldTypeDefinitions("source_ip"));
        assertThat(previousType.get(0)).isNotNull()
                .satisfies(fieldType -> assertThat(fieldType.type()).isEqualTo(FieldTypeMapper.STRING_TYPE));

        given()
                .config(api.withGraylogBackendFailureConfig())
                .spec(api.requestSpecification())
                .when()
                .body(new FieldTypeChangeRequest(Set.of(indexSet), "source_ip", "ip", true))
                .put("/system/indices/mappings")
                .then()
                .statusCode(200);

        waitForNewIndex(indexSet, INDEX_PREFIX + "_1");

        gelfInput.postMessage(messageWithTimestamp("2023-07-23 09:53:08.175"));

        api.waitFor(() -> {
            var fieldTypes = api.fieldTypes().getFieldTypes(AbsoluteRange.create("2023-01-01T00:00:00.000Z", "2024-01-01T00:00:00.000Z"), Set.of(stream));
            var sourceIpField = fieldTypes.stream().filter(type -> type.name().equals("source_ip")).findFirst();

            return sourceIpField.map(type -> type.type().type().equals("ip")).orElse(false);
        }, "Waiting for field type to change to `ip` timed out.", Duration.ofSeconds(60));
    }

    private void waitForNewIndex(String indexSetId, String indexName) {
        api.waitFor(() -> api.get("/system/indexer/indices/" + indexSetId + "/list ", 200)
                .extract()
                .body()
                .asString()
                .contains(indexName), "Waiting for new index after rotation timed out.", Duration.ofMinutes(3));
    }

    public String messageWithTimestamp(String timestamp) {
        return String.format(Locale.ROOT, """
                {
                "short_message":"field-type-mappings-test",
                "test_id": "field-type-mappings-test",
                "source":"example.org",
                "source_ip": "192.168.1.1",
                "timestamp": "%s",
                "level":3
                }""", timestamp);
    }
}
