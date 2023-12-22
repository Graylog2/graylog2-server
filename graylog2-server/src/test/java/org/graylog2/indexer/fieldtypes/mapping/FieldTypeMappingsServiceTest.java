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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.Message.FIELD_TIMESTAMP;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    @Mock
    private IndexFieldTypeProfileService profileService;

    private IndexSetConfig existingIndexSet;

    @BeforeEach
    void setUp() {
        toTest = new FieldTypeMappingsService(indexSetService, mongoIndexSetFactory, mongoIndexSetService, profileService);
        existingIndexSet = buildSampleIndexSetConfig("existing_index_set");

        //simple storage mocking
        lenient().when(mongoIndexSetService.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void testSavesIndexSetWithNewMappingAndPreviousMappings() {
        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");
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
        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");
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
        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");
        toTest.changeFieldType(existingCustomFieldMapping,
                new LinkedHashSet<>(List.of("existing_index_set", "wrong_index_set")),
                true);

        verify(existingMongoIndexSet, never()).cycle();
        verifyNoMoreInteractions(mongoIndexSetService);
    }

    @Test
    void testMappingsRemoval() {
        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");
        final Map<String, BulkOperationResponse> response = toTest.removeCustomMappingForFields(
                List.of("existing_field", "unexisting_field"),
                Set.of("existing_index_set", "unexisting_index_set"),
                false);

        assertThat(response).isNotNull().hasSize(2);
        assertThat(response).containsOnlyKeys("existing_index_set", "unexisting_index_set");

        final BulkOperationResponse existingIndexSetResponse = response.get("existing_index_set");
        assertThat(existingIndexSetResponse.successfullyPerformed()).isEqualTo(1);
        assertThat(existingIndexSetResponse.errors()).isEmpty();
        assertThat(existingIndexSetResponse.failures()).hasSize(1);

        final BulkOperationResponse unexistingIndexSetResponse = response.get("unexisting_index_set");
        assertThat(unexistingIndexSetResponse.successfullyPerformed()).isEqualTo(0);
        assertThat(unexistingIndexSetResponse.errors()).hasSize(1);
        assertThat(unexistingIndexSetResponse.failures()).isEmpty();
    }

    @Test
    void testRemovesMappingsForIndexSetEvenIfRemovalForAnotherIndexSetThrowsException() {
        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");
        final IndexSetConfig brokenIndexSet = buildSampleIndexSetConfig("broken_index_set");
        lenient().when(mongoIndexSetService.save(argThat(indexSetConfig -> Objects.equals(brokenIndexSet.id(), indexSetConfig.id()))))
                .thenThrow(new RuntimeException("Broken!"));

        final Map<String, BulkOperationResponse> response = toTest.removeCustomMappingForFields(
                List.of("existing_field", "unexisting_field"),
                Set.of("existing_index_set", "broken_index_set"),
                false);

        assertThat(response).isNotNull().hasSize(2);
        assertThat(response).containsOnlyKeys("existing_index_set", "broken_index_set");

        final BulkOperationResponse existingIndexSetResponse = response.get("existing_index_set");
        assertThat(existingIndexSetResponse.successfullyPerformed()).isEqualTo(1);
        assertThat(existingIndexSetResponse.errors()).isEmpty();
        assertThat(existingIndexSetResponse.failures()).hasSize(1);

        final BulkOperationResponse brokenIndexSetResponse = response.get("broken_index_set");
        assertThat(brokenIndexSetResponse.successfullyPerformed()).isEqualTo(0);
        assertThat(brokenIndexSetResponse.errors()).hasSize(1);
        assertThat(brokenIndexSetResponse.failures()).isEmpty();

    }

    @Test
    void testThrowsExceptionWhenTryingToSetUnExistingProfile() {
        assertThrows(NotFoundException.class, () -> toTest.setProfile(Set.of(), "000000000000000000000042", false));
    }

    @Test
    void testThrowsExceptionWhenTryingToSetProfileWithReservedFields() {
        IndexFieldTypeProfile profile = new IndexFieldTypeProfile(
                "000000000000000000000013",
                "Wrong!",
                "Profile with reserved fields",
                new CustomFieldMappings(List.of(new CustomFieldMapping(FIELD_TIMESTAMP, "ip")))
        );
        doReturn(Optional.of(profile)).when(profileService).get("000000000000000000000013");
        assertThrows(BadRequestException.class, () -> toTest.setProfile(Set.of(), "000000000000000000000013", false));
    }

    @Test
    void testSetsProfileIfItIsCorrect() {
        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");
        final String profileId = "000000000000000000000007";
        IndexFieldTypeProfile profile = new IndexFieldTypeProfile(
                profileId,
                "Nice profile!",
                "Nice profile!",
                new CustomFieldMappings(List.of(new CustomFieldMapping("bubamara", "ip")))
        );
        doReturn(Optional.of(profile)).when(profileService).get(profileId);
        toTest.setProfile(Set.of(existingIndexSet.id()), profileId, false);

        verify(mongoIndexSetService).save(
                existingIndexSet.toBuilder()
                        .fieldTypeProfile(profileId)
                        .build());
        verifyNoInteractions(existingMongoIndexSet);
    }

    @Test
    void testDoesNothingWhenAskedToSetProfileWhichWasAlreadySet() {
        existingIndexSet = existingIndexSet.toBuilder()
                .fieldTypeProfile("000000000000000000000007")
                .build();
        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");
        final String profileId = "000000000000000000000007";
        IndexFieldTypeProfile profile = new IndexFieldTypeProfile(
                profileId,
                "Nice profile!",
                "Nice profile!",
                new CustomFieldMappings(List.of(new CustomFieldMapping("bubamara", "ip")))
        );
        doReturn(Optional.of(profile)).when(profileService).get(profileId);
        toTest.setProfile(Set.of(existingIndexSet.id()), profileId, false);

        verify(mongoIndexSetService, never()).save(any());
        verifyNoInteractions(existingMongoIndexSet);
    }

    @Test
    void testRemovesProfile() {
        existingIndexSet = existingIndexSet.toBuilder()
                .fieldTypeProfile("000000000000000000000042")
                .build();
        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");
        toTest.removeProfileFromIndexSets(Set.of(existingIndexSet.id()), false);

        verify(mongoIndexSetService).save(
                existingIndexSet.toBuilder()
                        .fieldTypeProfile(null)
                        .build());
        verifyNoInteractions(existingMongoIndexSet);
    }

    @Test
    void testDoesNothingWhenAskedToRemoveProfileFromIndexSetWithout() {
        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");
        toTest.removeProfileFromIndexSets(Set.of(existingIndexSet.id()), false);

        verify(mongoIndexSetService, never()).save(any());
        verifyNoInteractions(existingMongoIndexSet);
    }

    @Test
    void testOverridesPreviousProfile() {
        existingIndexSet = existingIndexSet.toBuilder()
                .fieldTypeProfile("000000000000000000000042")
                .build();
        doReturn(Optional.of(existingIndexSet)).when(indexSetService).get("existing_index_set");
        final String profileId = "000000000000000000000007";
        IndexFieldTypeProfile profile = new IndexFieldTypeProfile(
                profileId,
                "Nice profile!",
                "Nice profile!",
                new CustomFieldMappings(List.of(new CustomFieldMapping("bubamara", "ip")))
        );
        doReturn(Optional.of(profile)).when(profileService).get(profileId);
        toTest.setProfile(Set.of(existingIndexSet.id()), profileId, false);

        verify(mongoIndexSetService).save(
                existingIndexSet.toBuilder()
                        .fieldTypeProfile(profileId)
                        .build());
        verifyNoInteractions(existingMongoIndexSet);
    }

    private IndexSetConfig buildSampleIndexSetConfig(final String id) {
        return IndexSetConfig.builder()
                .id(id)
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
