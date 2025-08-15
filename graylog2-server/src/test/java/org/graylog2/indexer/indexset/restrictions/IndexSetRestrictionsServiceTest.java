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

import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.graylog2.indexer.indexset.template.IndexSetTemplateService;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetCreationRequest;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.rest.resources.system.indexer.IndexSetTestUtils.createIndexSetConfig;
import static org.graylog2.rest.resources.system.indexer.IndexSetTestUtils.toCreationRequest;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IndexSetRestrictionsServiceTest {

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
                .fieldRestrictions(Set.of(
                        restriction("shards"),
                        restriction("retention_strategy.max_number_of_indices"),
                        restriction("rotation_strategy")
                ))
                .build();
        IndexSetTemplate indexSetTemplate = createIndexSetTemplate(indexSetConfig);
        IndexSetCreationRequest request = toCreationRequest(indexSetConfig).toBuilder()
                .indexSetTemplateId(indexSetTemplate.id())
                .build();
        when(templateService.get(indexSetTemplate.id())).thenReturn(Optional.of(indexSetTemplate));

        IndexSetConfig created = underTest.createIndexSetConfig(request);

        assertThat(created).isEqualTo(indexSetConfig);

    }

    @Test
    void createIndexSetConfigWithRestrictionsAndChangedImmutableFields() {
        Set<IndexSetFieldRestriction> restrictions = Set.of(
                restriction("shards"),
                restriction("retention_strategy.max_number_of_indices"),
                restriction("rotation_strategy")
        );
        IndexSetConfig indexSetConfig = createIndexSetConfig(null, "title").toBuilder()
                .fieldRestrictions(restrictions)
                .build();
        IndexSetTemplate indexSetTemplate = createIndexSetTemplate(indexSetConfig);
        IndexSetCreationRequest request = toCreationRequest(indexSetConfig).toBuilder()
                .indexSetTemplateId(indexSetTemplate.id())
                .shards(indexSetConfig.shards()+1)
                .retentionStrategyConfig(NoopRetentionStrategyConfig.create(2))
                .rotationStrategyConfig(MessageCountRotationStrategyConfig.create(1))
                .build();
        when(templateService.get(indexSetTemplate.id())).thenReturn(Optional.of(indexSetTemplate));

        assertThatThrownBy(() -> underTest.createIndexSetConfig(request))
                .hasMessageContaining(restrictions.stream()
                        .map(IndexSetFieldRestriction::fieldName)
                        .collect(Collectors.joining(", ")));
    }

    private static ImmutableIndexSetField restriction(String field) {
        return ImmutableIndexSetField.builder()
                .fieldName(field)
                .build();
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
