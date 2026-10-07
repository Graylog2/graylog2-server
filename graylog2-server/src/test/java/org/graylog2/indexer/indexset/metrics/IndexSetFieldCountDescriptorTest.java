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
package org.graylog2.indexer.indexset.metrics;

import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesDTO;
import org.graylog2.indexer.fieldtypes.IndexFieldTypesService;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.metrics.entity.EntityMetric;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndexSetFieldCountDescriptorTest {
    private static final String A = "000000000000000000000001";
    private static final String B = "000000000000000000000002";
    private static final String C = "000000000000000000000003";
    private final Indices indices = mock(Indices.class);
    private final IndexFieldTypesService indexFieldTypesService = mock(IndexFieldTypesService.class);
    private final IndexSetService indexSetService = mock(IndexSetService.class);
    private final IndexSetRegistry indexSetRegistry = mock(IndexSetRegistry.class);
    private final IndexSetFieldCountDescriptor descriptor = new IndexSetFieldCountDescriptor(
            indices, indexFieldTypesService, indexSetService, indexSetRegistry, Duration.ofMinutes(5));

    @Test
    void countsTheFieldsOfEachActiveWriteIndex() {
        IndexSetMocks.stubLookup(indexSetService, indexSetRegistry,
                IndexSetMocks.indexSet(A, "graylog", true),
                IndexSetMocks.indexSet(B, "other", true),
                IndexSetMocks.indexSet(C, "fresh", true));
        when(indices.getIndexNamesAndAliases(argThat((Collection<String> patterns) -> Set.copyOf(patterns).equals(Set.of("graylog_*", "other_*", "fresh_*"))))).thenReturn(Map.of(
                "graylog_1", Set.of("graylog_1_reopened"),
                "graylog_2", Set.of("graylog_deflector"),
                "other_0", Set.of("other_deflector")));
        when(indexFieldTypesService.findByIndexNames(argThat(names -> Set.copyOf(names).equals(Set.of("graylog_2", "other_0")))))
                .thenReturn(List.of(IndexFieldTypesDTO.create(A, "graylog_2",
                        Set.of(FieldTypeDTO.create("message", "text"), FieldTypeDTO.create("source", "keyword")))));

        // B has a write index without polled field types, C has no write index yet
        assertThat(descriptor.compute(List.of(A, B, C))).containsExactlyInAnyOrder(
                new EntityMetric<>(A, 2),
                new EntityMetric<>(B, null),
                new EntityMetric<>(C, null));
    }

    @Test
    void anAliasWithSeveralTargetsStaysUnresolved() {
        IndexSetMocks.stubLookup(indexSetService, indexSetRegistry,
                IndexSetMocks.indexSet(A, "graylog", true),
                IndexSetMocks.indexSet(B, "other", true));
        when(indices.getIndexNamesAndAliases(anyCollection())).thenReturn(Map.of(
                "graylog_1", Set.of("graylog_deflector"),
                "graylog_2", Set.of("graylog_deflector"),
                "graylog_3", Set.of("graylog_deflector"),
                "other_0", Set.of("other_deflector")));
        when(indexFieldTypesService.findByIndexNames(argThat((Collection<String> names) -> Set.copyOf(names).equals(Set.of("other_0")))))
                .thenReturn(List.of(IndexFieldTypesDTO.create(B, "other_0", Set.of(FieldTypeDTO.create("message", "text")))));

        assertThat(descriptor.compute(List.of(A, B))).containsExactlyInAnyOrder(
                new EntityMetric<>(A, null),
                new EntityMetric<>(B, 1));
    }
}
