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
package org.graylog2.indexer.fieldtypes.mapping;

import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class FieldTypeMappingsServiceTest {

    private final CustomFieldMapping newCustomMapping = new CustomFieldMapping("new_field", "long");
    private final CustomFieldMapping existingCustomFieldMapping = new CustomFieldMapping("existing_field", "string_fts");

    private FieldTypeMappingsService toTest;

    @Mock
    private IndexSetService indexSetService;
    @Mock
    private MongoIndexSet.Factory mongoIndexSetFactory;
    @Mock
    private MongoIndexSetService mongoIndexSetService;
    @Mock
    private MongoIndexSet existingMongoIndexSet;

    private final IndexSetConfig existingIndexSet = buildSampleIndexSetConfig();

    @BeforeEach
    void setUp() {
        toTest = new FieldTypeMappingsService(indexSetService, mongoIndexSetFactory, mongoIndexSetService);

        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");

        //simple storage mocking
        lenient().when(mongoIndexSetService.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void testSavesIndexSetWithNewMappingAndPreviousMappings() {
        toTest.changeFieldType(newCustomMapping,
                Set.of("existing_index_set"),
                false);

        verify(mongoIndexSetService).save(
                existingIndexSet.toBuilder()
                        .customFieldMappings(new CustomFieldMappings(Set.of(existingCustomFieldMapping, newCustomMapping)))
                        .build());
        verifyNoInteractions(existingMongoIndexSet);
    }

    @Test
    void testCyclesIndexSet() {
        doReturn(existingMongoIndexSet).when(mongoIndexSetFactory).create(any());

        toTest.changeFieldType(newCustomMapping,
                new LinkedHashSet<>(List.of("existing_index_set", "wrong_index_set")),
                true);

        final IndexSetConfig expectedUpdatedConfig = existingIndexSet.toBuilder()
                .customFieldMappings(new CustomFieldMappings(Set.of(existingCustomFieldMapping, newCustomMapping)))
                .build();

        verify(mongoIndexSetService).save(expectedUpdatedConfig);
        verify(existingMongoIndexSet).cycle();
        verifyNoMoreInteractions(mongoIndexSetService);
    }

    @Test
    void testDoesNotCycleIndexSetWhenMappingAlreadyExisted() {
        toTest.changeFieldType(existingCustomFieldMapping,
                new LinkedHashSet<>(List.of("existing_index_set", "wrong_index_set")),
                true);

        verify(existingMongoIndexSet, never()).cycle();
        verifyNoMoreInteractions(mongoIndexSetService);
    }

    private IndexSetConfig buildSampleIndexSetConfig() {
        return IndexSetConfig.builder()
                .id("existing_index_set")
                .title("title")
                .indexWildcard("ex*")
                .shards(1)
                .replicas(1)
                .rotationStrategy(SizeBasedRotationStrategyConfig.create(42))
                .indexPrefix("ex")
                .retentionStrategy(NoopRetentionStrategyConfig.create(13))
                .creationDate(ZonedDateTime.now())
                .indexAnalyzer("korean")
                .indexTemplateName("test_template")
                .indexOptimizationDisabled(true)
                .indexOptimizationMaxNumSegments(77)
                .customFieldMappings(new CustomFieldMappings(Set.of(existingCustomFieldMapping)))
                .build();
    }
}
