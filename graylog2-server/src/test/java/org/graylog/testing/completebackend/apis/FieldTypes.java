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

import org.graylog.plugins.views.search.rest.FieldTypesForStreamsRequest;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;

public class FieldTypes implements GraylogRestApi {

    private final GraylogApis api;

    public FieldTypes(GraylogApis api) {
        this.api = api;
    }

    public List<MappedFieldTypeDTO> getFieldTypes() {
        final MappedFieldTypeDTO[] as = given()
                .spec(api.requestSpecification())
                .get("/views/fields")
                .as(MappedFieldTypeDTO[].class);
        return Arrays.asList(as);
    }

    public List<MappedFieldTypeDTO> getFieldTypes(TimeRange timeRange, Set<String> streams) {
        final MappedFieldTypeDTO[] as = given()
                .spec(api.requestSpecification())
                .body(FieldTypesForStreamsRequest.Builder.builder().streams(streams).timerange(timeRange).build())
                .post("/views/fields")
                .as(MappedFieldTypeDTO[].class);
        return Arrays.asList(as);
    }

    public Set<MappedFieldTypeDTO> waitForFieldTypeDefinitions(Set<String> streams, String... fieldName) {
        final Set<String> expectedFields = Arrays.stream(fieldName).collect(Collectors.toSet());
        return waitForObject(() -> {
            final List<MappedFieldTypeDTO> knownTypes = getFieldTypes(RelativeRange.allTime(), streams);
            final Set<MappedFieldTypeDTO> filtered = knownTypes.stream().filter(t -> expectedFields.contains(t.name())).collect(Collectors.toSet());
            if (filtered.size() == expectedFields.size()) {
                return Optional.of(filtered);
            } else {
                return Optional.empty();
            }
        }, "Timed out waiting for field definition", Duration.ofSeconds(30));
    }

    public Set<MappedFieldTypeDTO> waitForFieldTypeDefinitions(String... fieldName) {
        final Set<String> expectedFields = Arrays.stream(fieldName).collect(Collectors.toSet());
        return waitForObject(() -> {
            final List<MappedFieldTypeDTO> knownTypes = getFieldTypes();
            final Set<MappedFieldTypeDTO> filtered = knownTypes.stream().filter(t -> expectedFields.contains(t.name())).collect(Collectors.toSet());
            if (filtered.size() == expectedFields.size()) {
                return Optional.of(filtered);
            } else {
                return Optional.empty();
            }
        }, "Timed out waiting for field definition");
    }
}
