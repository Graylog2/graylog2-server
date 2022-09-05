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
package org.graylog2.indexer.fieldtypes.streamfiltered.filters;

import com.google.common.collect.ImmutableSet;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.AggregationBasedFieldTypeFilterAdapter;
import org.graylog2.indexer.fieldtypes.streamfiltered.esadapters.CountExistingBasedFieldTypeFilterAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


class AdHocSearchEngineStreamBasedFieldTypeFilterTest {

    private AdHocSearchEngineStreamBasedFieldTypeFilter toTest;
    private AggregationBasedFieldTypeFilterAdapter aggregationBasedFieldTypeFilterAdapter;
    private CountExistingBasedFieldTypeFilterAdapter countExistingBasedFieldTypeFilterAdapter;

    private final Set<String> indexNames = Collections.singleton("graylog_0");
    private final Set<FieldTypeDTO> fields = ImmutableSet.of(
            FieldTypeDTO.create("message", "text"),
            FieldTypeDTO.create("count-filter-out-message", "text"),
            FieldTypeDTO.create("count", "long"),
            FieldTypeDTO.create("host", "keyword")
    );

    @BeforeEach
    void setUp() {
        countExistingBasedFieldTypeFilterAdapter = (fieldTypeDTOs, indexNames, streamIds) -> fieldTypeDTOs.stream()
                .filter(f -> !f.fieldName().startsWith("count-filter-out-"))
                .collect(Collectors.toSet());
        toTest = new AdHocSearchEngineStreamBasedFieldTypeFilter(aggregationBasedFieldTypeFilterAdapter, countExistingBasedFieldTypeFilterAdapter);

    }

    @Test
    void returnsEmptyCollectionOnNullStreams() {
        assertThat(toTest.filterFieldTypes(fields, indexNames, null))
                .isEmpty();
    }

    @Test
    void returnsEmptyCollectionOnEmptyStreams() {
        assertThat(toTest.filterFieldTypes(fields, indexNames, Collections.emptySet()))
                .isEmpty();
    }

    @Test
    void filtersFieldsCorrectly() {
        final Set<FieldTypeDTO> filteredFields = toTest.filterFieldTypes(fields, indexNames, Collections.singleton("streamId"));
        assertThat(filteredFields)
                .isNotNull()
                .hasSize(3)
                .contains(FieldTypeDTO.create("message", "text"))
                .contains(FieldTypeDTO.create("count", "long"))
                .contains(FieldTypeDTO.create("host", "keyword"));
    }
}
