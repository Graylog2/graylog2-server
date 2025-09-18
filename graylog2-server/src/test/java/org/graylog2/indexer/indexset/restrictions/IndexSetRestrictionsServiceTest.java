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
package org.graylog2.indexer.indexset.restrictions;

import jakarta.ws.rs.BadRequestException;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.graylog2.indexer.indexset.template.IndexSetTemplateService;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetCreationRequest;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetUpdateRequest;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.rest.resources.system.indexer.IndexSetTestUtils.createIndexSetConfig;
import static org.graylog2.rest.resources.system.indexer.IndexSetTestUtils.toCreationRequest;
import static org.graylog2.rest.resources.system.indexer.IndexSetTestUtils.toUpdateRequest;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexSetRestrictionsServiceTest {

    private static final Map<String, Set<IndexSetFieldRestriction>> FIELD_RESTRICTION = Map.of(
            "shards", Set.of(ImmutableIndexSetField.builder().build()),
            "retention_strategy.max_number_of_indices", Set.of(ImmutableIndexSetField.builder().build()),
            "rotation_strategy", Set.of(ImmutableIndexSetField.builder().build(), HiddenIndexSetField.builder().build())
    );

    @Mock
    private IndexSetTemplateService templateService;
    @Mock
    private IndexSetDefaultTemplateService indexSetDefaultTemplateService;

    private IndexSetRestrictionsService underTest;

    @BeforeEach
    void setUp() {
        underTest = new IndexSetRestrictionsService(
                templateService,
                indexSetDefaultTemplateService,
                new ObjectMapperProvider().get());
    }

    @Test
    void createIndexSetConfigWithRestrictions() {
        IndexSetConfig indexSetConfig = createIndexSetConfig(null, "title").toBuilder()
                .fieldRestrictions(FIELD_RESTRICTION)
                .build();
        IndexSetTemplate indexSetTemplate = createIndexSetTemplate(indexSetConfig);
        IndexSetCreationRequest request = toCreationRequest(indexSetConfig).toBuilder()
                .indexSetTemplateId(indexSetTemplate.id())
                .build();
        when(templateService.get(indexSetTemplate.id())).thenReturn(Optional.of(indexSetTemplate));

        IndexSetConfig created = underTest.createIndexSetConfig(request, false);

        assertThat(created).isEqualTo(indexSetConfig);
    }

    @Test
    void createIndexSetConfigWithRestrictionsAndChangedImmutableFields() {
        IndexSetConfig indexSetConfig = createIndexSetConfig(null, "title").toBuilder()
                .fieldRestrictions(FIELD_RESTRICTION)
                .build();
        IndexSetTemplate indexSetTemplate = createIndexSetTemplate(indexSetConfig);
        IndexSetCreationRequest request = toCreationRequest(indexSetConfig).toBuilder()
                .indexSetTemplateId(indexSetTemplate.id())
                .shards(indexSetConfig.shards() + 1)
                .retentionStrategyConfig(NoopRetentionStrategyConfig.create(2))
                .rotationStrategyConfig(MessageCountRotationStrategyConfig.create(1))
                .build();
        when(templateService.get(indexSetTemplate.id())).thenReturn(Optional.of(indexSetTemplate));

        final BadRequestException exception = assertThrows(BadRequestException.class, () ->
                underTest.createIndexSetConfig(request, false));
        for (Map.Entry<String, Set<IndexSetFieldRestriction>> entry : FIELD_RESTRICTION.entrySet()) {
            assertThat(exception.getMessage()).contains(f("%s : %s", entry.getKey(), ImmutableIndexSetField.TYPE_NAME));
        }
    }

    @Test
    void createIndexSetConfigAndSkipRestrictionsCheck() {
        IndexSetConfig indexSetConfig = createIndexSetConfig(null, "title").toBuilder()
                .fieldRestrictions(FIELD_RESTRICTION)
                .build();
        IndexSetTemplate indexSetTemplate = createIndexSetTemplate(indexSetConfig);
        IndexSetCreationRequest request = toCreationRequest(indexSetConfig).toBuilder()
                .indexSetTemplateId(indexSetTemplate.id())
                .shards(indexSetConfig.shards() + 1)
                .build();
        when(templateService.get(indexSetTemplate.id())).thenReturn(Optional.of(indexSetTemplate));

        IndexSetConfig created = underTest.createIndexSetConfig(request, true);

        assertThat(created).isEqualTo(indexSetConfig.toBuilder()
                .shards(request.shards())
                .build());
    }

    @Test
    void updateIndexSetConfigWithRestrictions() {
        IndexSetConfig indexSetConfig = createIndexSetConfig("1", "title").toBuilder()
                .fieldRestrictions(FIELD_RESTRICTION)
                .build();
        IndexSetUpdateRequest request = toUpdateRequest(indexSetConfig).toBuilder()
                .replicas(1)
                .build();

        IndexSetConfig created = underTest.updateIndexSetConfig(request, indexSetConfig, false);

        assertThat(created).isEqualTo(indexSetConfig.toBuilder()
                .replicas(request.replicas())
                .build());
    }

    @Test
    void updateIndexSetConfigWithRestrictionsAndChangedImmutableFields() {
        IndexSetConfig indexSetConfig = createIndexSetConfig("1", "title").toBuilder()
                .fieldRestrictions(FIELD_RESTRICTION)
                .build();
        IndexSetUpdateRequest request = toUpdateRequest(indexSetConfig).toBuilder()
                .replicas(1)
                .shards(indexSetConfig.shards() + 1)
                .retentionStrategyConfig(NoopRetentionStrategyConfig.create(2))
                .rotationStrategyConfig(MessageCountRotationStrategyConfig.create(1))
                .build();

        final BadRequestException exception = assertThrows(BadRequestException.class, () ->
                underTest.updateIndexSetConfig(request, indexSetConfig, false));
        for (Map.Entry<String, Set<IndexSetFieldRestriction>> entry : FIELD_RESTRICTION.entrySet()) {
            assertThat(exception.getMessage()).contains(f("%s : %s", entry.getKey(), ImmutableIndexSetField.TYPE_NAME));
        }
    }

    @Test
    void updateIndexSetConfigWithChangedRestrictions() {
        IndexSetConfig indexSetConfig = createIndexSetConfig("1", "title").toBuilder()
                .fieldRestrictions(FIELD_RESTRICTION)
                .build();
        IndexSetUpdateRequest request = toUpdateRequest(indexSetConfig).toBuilder()
                .fieldRestrictions(null)
                .build();

        assertThatThrownBy(() -> underTest.updateIndexSetConfig(request, indexSetConfig, false))
                .hasMessageContaining(RestPermissions.INDEXSETS_FIELD_RESTRICTIONS_EDIT);
    }

    @Test
    void updateIndexSetConfigAndSkipRestrictionsCheck() {
        IndexSetConfig indexSetConfig = createIndexSetConfig("1", "title").toBuilder()
                .fieldRestrictions(FIELD_RESTRICTION)
                .build();
        IndexSetUpdateRequest request = toUpdateRequest(indexSetConfig).toBuilder()
                .shards(indexSetConfig.shards() + 1)
                .build();

        IndexSetConfig created = underTest.updateIndexSetConfig(request, indexSetConfig, true);

        assertThat(created).isEqualTo(indexSetConfig.toBuilder()
                .shards(request.shards())
                .build());
    }

    private static IndexSetTemplate createIndexSetTemplate(IndexSetConfig indexSetConfig) {
        return new IndexSetTemplate("1", "", "", false, IndexSetTemplateConfig.builder()
                .shards(indexSetConfig.shards())
                .replicas(indexSetConfig.replicas())
                .rotationStrategyClass(indexSetConfig.rotationStrategyClass())
                .rotationStrategyConfig(indexSetConfig.rotationStrategyConfig())
                .retentionStrategyClass(indexSetConfig.retentionStrategyClass())
                .retentionStrategyConfig(indexSetConfig.retentionStrategyConfig())
                .indexOptimizationMaxNumSegments(indexSetConfig.indexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(indexSetConfig.indexOptimizationDisabled())
                .indexAnalyzer(indexSetConfig.indexAnalyzer())
                .fieldTypeRefreshInterval(indexSetConfig.fieldTypeRefreshInterval())
                .useLegacyRotation(indexSetConfig.dataTieringConfig() == null)
                .fieldRestrictions(indexSetConfig.fieldRestrictions())
                .build());
    }

}
