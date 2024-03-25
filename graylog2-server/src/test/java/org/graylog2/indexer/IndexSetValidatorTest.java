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
package org.graylog2.indexer;

import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.datatiering.DataTieringChecker;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.datatiering.DataTieringOrchestrator;
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.indexer.EventIndexTemplateProvider.EVENT_TEMPLATE_TYPE;
import static org.graylog2.indexer.indexset.IndexSetConfig.DEFAULT_INDEX_TEMPLATE_TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IndexSetValidatorTest {

    @Mock
    private IndexSetRegistry indexSetRegistry;

    @Mock
    private ElasticsearchConfiguration elasticsearchConfiguration;

    @Mock
    private DataTieringOrchestrator dataTieringOrchestrator;

    @Mock
    private DataTieringChecker dataTieringChecker;

    private IndexSetValidator validator;

    @BeforeEach
    public void setUp() throws Exception {
        this.validator = new IndexSetValidator(indexSetRegistry, elasticsearchConfiguration, dataTieringOrchestrator, dataTieringChecker);
    }

    @Test
    public void validate() throws Exception {
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSetRegistry.iterator()).thenReturn(Collections.singleton(indexSet).iterator());
        when(indexSet.getIndexPrefix()).thenReturn("foo");
        IndexSetConfig validConfig = testIndexSetConfig();

        final Optional<IndexSetValidator.Violation> violation = validator.validate(validConfig);

        assertThat(violation).isNotPresent();
    }

    @Test
    public void validateWhenAlreadyManaged() {
        final String prefix = "graylog_index";
        final IndexSetConfig newConfig = mock(IndexSetConfig.class);

        when(indexSetRegistry.isManagedIndex("graylog_index_0")).thenReturn(true);
        when(newConfig.indexPrefix()).thenReturn(prefix);

        final Optional<IndexSetValidator.Violation> violation = validator.validate(newConfig);

        assertThat(violation).isPresent();
    }

    @Test
    public void validateWithConflict() {
        final IndexSetConfig newConfig = mock(IndexSetConfig.class);
        final IndexSet indexSet = mock(IndexSet.class);

        when(indexSetRegistry.iterator()).thenReturn(Collections.singleton(indexSet).iterator());

        // New index prefix starts with existing index prefix
        when(indexSet.getIndexPrefix()).thenReturn("graylog");
        when(newConfig.indexPrefix()).thenReturn("graylog_index");

        final Optional<IndexSetValidator.Violation> violation = validator.validate(newConfig);

        assertThat(violation).isPresent();
    }

    @Test
    public void validateWithConflict2() {
        final IndexSetConfig newConfig = mock(IndexSetConfig.class);
        final IndexSet indexSet = mock(IndexSet.class);

        when(indexSetRegistry.iterator()).thenReturn(Collections.singleton(indexSet).iterator());

        // Existing index prefix starts with new index prefix
        when(indexSet.getIndexPrefix()).thenReturn("graylog");
        when(newConfig.indexPrefix()).thenReturn("gray");

        final Optional<IndexSetValidator.Violation> violation = validator.validate(newConfig);

        assertThat(violation).isPresent();
    }

    @Test
    public void validateWithInvalidFieldTypeRefreshInterval() {
        final Duration fieldTypeRefreshInterval = Duration.millis(999);
        final IndexSetConfig newConfig = mock(IndexSetConfig.class);
        final IndexSet indexSet = mock(IndexSet.class);

        when(indexSetRegistry.iterator()).thenReturn(Collections.singleton(indexSet).iterator());
        when(indexSet.getIndexPrefix()).thenReturn("foo");
        when(newConfig.indexPrefix()).thenReturn("graylog_index");

        when(newConfig.fieldTypeRefreshInterval()).thenReturn(fieldTypeRefreshInterval);

        final Optional<IndexSetValidator.Violation> violation = validator.validate(newConfig);

        assertThat(violation).isPresent();
    }

    @Test
    public void validateMaxRetentionPeriod() {
        when(indexSetRegistry.iterator()).thenReturn(Collections.emptyIterator());

        // no max retention period configured
        assertThat(validator.validate(testIndexSetConfig())).isNotPresent();

        // max retention period >= effective retention period
        when(elasticsearchConfiguration.getMaxIndexRetentionPeriod()).thenReturn(Period.days(10));
        assertThat(validator.validate(testIndexSetConfig())).isNotPresent();

        // max retention period < effective retention period
        when(elasticsearchConfiguration.getMaxIndexRetentionPeriod()).thenReturn(Period.days(9));
        assertThat(validator.validate(testIndexSetConfig())).hasValueSatisfying(v ->
                assertThat(v.message()).contains("effective index retention period of P1W3D")
        );

        // rotation strategy is not time-based
        final IndexSetConfig modifiedConfig = testIndexSetConfig().toBuilder()
                .rotationStrategy(MessageCountRotationStrategyConfig.create(Integer.MAX_VALUE))
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .build();
        assertThat(validator.validate(modifiedConfig)).isNotPresent();
    }

    @Test
    public void validateIndexAction() {
        final String prefix = "graylog_index";
        final Duration fieldTypeRefreshInterval = Duration.standardSeconds(1L);
        final IndexSetConfig newConfig = mock(IndexSetConfig.class);
        final IndexSet indexSet = mock(IndexSet.class);
        final RetentionStrategyConfig retentionStrategyConfig = mock(RetentionStrategyConfig.class);

        when(indexSet.getIndexPrefix()).thenReturn("foo");
        when(indexSetRegistry.iterator()).thenReturn(Collections.singleton(indexSet).iterator());
        when(newConfig.indexPrefix()).thenReturn(prefix);
        when(newConfig.fieldTypeRefreshInterval()).thenReturn(fieldTypeRefreshInterval);
        when(newConfig.retentionStrategy()).thenReturn(retentionStrategyConfig);

        final Optional<IndexSetValidator.Violation> violation = validator.validate(newConfig);

        assertThat(violation).isPresent();
    }

    @Test
    public void testStrategiesPresentIfDataTiersIsNull() {
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getIndexPrefix()).thenReturn("foo");
        when(indexSetRegistry.iterator()).thenReturn(Collections.singleton(indexSet).iterator());


        assertThat(validator.validate(testIndexSetConfig().toBuilder().retentionStrategy(null).build())).hasValueSatisfying(v ->
                assertThat(v.message()).contains("retention_strategy cannot be null")
        );
        assertThat(validator.validate(testIndexSetConfig().toBuilder().retentionStrategyClass(null).build())).hasValueSatisfying(v ->
                assertThat(v.message()).contains("retention_strategy_class cannot be null")
        );
        assertThat(validator.validate(testIndexSetConfig().toBuilder().rotationStrategy(null).build())).hasValueSatisfying(v ->
                assertThat(v.message()).contains("rotation_strategy cannot be null")
        );
        assertThat(validator.validate(testIndexSetConfig().toBuilder().rotationStrategyClass(null).build())).hasValueSatisfying(v ->
                assertThat(v.message()).contains("rotation_strategy_class cannot be null")
        );
    }

    @Test
    public void testDataTieringByDefaultDisabledInCloud() {
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSet.getIndexPrefix()).thenReturn("foo");
        when(indexSetRegistry.iterator()).thenReturn(Collections.singleton(indexSet).iterator());

        this.validator = new IndexSetValidator(indexSetRegistry, elasticsearchConfiguration, dataTieringOrchestrator, dataTieringChecker);

        IndexSetConfig config = testIndexSetConfig().toBuilder().dataTiering(mock(DataTieringConfig.class)).build();
        assertThat(validator.validate(config)).hasValueSatisfying(v ->
                assertThat(v.message()).isEqualTo("data tiering feature is disabled!"));

        when(dataTieringChecker.isEnabled()).thenReturn(true);
        assertThat(validator.validate(config)).isEmpty();
    }

    @Test
    public void testWarmTierKeywordReserved() {
        IndexSetConfig config = testIndexSetConfig().toBuilder().indexPrefix("warm_").build();

        this.validator = new IndexSetValidator(indexSetRegistry, elasticsearchConfiguration, dataTieringOrchestrator, dataTieringChecker);

        assertThat(validator.validate(config)).hasValueSatisfying(v ->
                assertThat(v.message()).contains("contains reserved keyword 'warm_'!"));
    }

    @Test
    public void testValidationOfProfilesInIndexSetConfig() {
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSetRegistry.iterator()).thenReturn(Collections.singleton(indexSet).iterator());
        when(indexSet.getIndexPrefix()).thenReturn("foo");

        this.validator = new IndexSetValidator(indexSetRegistry, elasticsearchConfiguration, dataTieringOrchestrator, dataTieringChecker);
        IndexSetConfig config = testIndexSetConfig().toBuilder().indexTemplateType(EVENT_TEMPLATE_TYPE).fieldTypeProfile("smth").build();
        assertThat(validator.validate(config)).hasValueSatisfying(v ->
                assertThat(v.message()).contains("Profiles cannot be set for events and failures index sets"));

        config = testIndexSetConfig().toBuilder().indexTemplateType("failures").fieldTypeProfile("smth").build();
        assertThat(validator.validate(config)).hasValueSatisfying(v ->
                assertThat(v.message()).contains("Profiles cannot be set for events and failures index sets"));

        config = testIndexSetConfig().toBuilder().indexTemplateType(EVENT_TEMPLATE_TYPE).fieldTypeProfile("").build();
        assertThat(validator.validate(config)).isEmpty();
        config = testIndexSetConfig().toBuilder().indexTemplateType(EVENT_TEMPLATE_TYPE).fieldTypeProfile(null).build();
        assertThat(validator.validate(config)).isEmpty();
        config = testIndexSetConfig().toBuilder().indexTemplateType("failures").fieldTypeProfile("").build();
        assertThat(validator.validate(config)).isEmpty();
        config = testIndexSetConfig().toBuilder().indexTemplateType("failures").fieldTypeProfile(null).build();
        assertThat(validator.validate(config)).isEmpty();

        config = testIndexSetConfig().toBuilder().indexTemplateType(DEFAULT_INDEX_TEMPLATE_TYPE).fieldTypeProfile("smth").build();
        assertThat(validator.validate(config)).isEmpty();
    }

    @Test
    public void testValidationOfCustomMappingsInIndexSetConfig() {
        final IndexSet indexSet = mock(IndexSet.class);
        when(indexSetRegistry.iterator()).thenReturn(Collections.singleton(indexSet).iterator());
        when(indexSet.getIndexPrefix()).thenReturn("foo");

        this.validator = new IndexSetValidator(indexSetRegistry, elasticsearchConfiguration, dataTieringOrchestrator, dataTieringChecker);
        IndexSetConfig config = testIndexSetConfig().toBuilder().indexTemplateType(EVENT_TEMPLATE_TYPE)
                .customFieldMappings(new CustomFieldMappings(List.of(new CustomFieldMapping("john", "long"))))
                .build();
        assertThat(validator.validate(config)).hasValueSatisfying(v ->
                assertThat(v.message()).contains("Custom field mappings cannot be set for events and failures index sets"));

        config = testIndexSetConfig().toBuilder().indexTemplateType("failures")
                .customFieldMappings(new CustomFieldMappings(List.of(new CustomFieldMapping("john", "long"))))
                .build();
        assertThat(validator.validate(config)).hasValueSatisfying(v ->
                assertThat(v.message()).contains("Custom field mappings cannot be set for events and failures index sets"));

        config = testIndexSetConfig().toBuilder().indexTemplateType(EVENT_TEMPLATE_TYPE)
                .customFieldMappings(new CustomFieldMappings())
                .build();
        assertThat(validator.validate(config)).isEmpty();
        config = testIndexSetConfig().toBuilder().indexTemplateType("failures")
                .customFieldMappings(new CustomFieldMappings())
                .build();
        assertThat(validator.validate(config)).isEmpty();

        config = testIndexSetConfig().toBuilder().indexTemplateType(DEFAULT_INDEX_TEMPLATE_TYPE)
                .customFieldMappings(new CustomFieldMappings(List.of(new CustomFieldMapping("john", "long"))))
                .build();
        assertThat(validator.validate(config)).isEmpty();
    }

    private IndexSetConfig testIndexSetConfig() {
        return IndexSetConfig.builder()
                .isWritable(true)
                .title("Test 1")
                .description("A test index-set.")
                .indexPrefix("graylog1")
                .indexWildcard("graylog1_*")
                .rotationStrategy(TimeBasedRotationStrategyConfig.builder().maxRotationPeriod(Period.days(1)).build())
                .rotationStrategyClass(TimeBasedRotationStrategyConfig.class.getCanonicalName())
                .retentionStrategy(NoopRetentionStrategyConfig.create(10))
                .retentionStrategyClass(NoopRetentionStrategy.class.getCanonicalName())
                .shards(4)
                .replicas(0)
                .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
                .indexTemplateName("graylog1-template")
                .indexAnalyzer("standard")
                .indexOptimizationMaxNumSegments(1)
                .indexOptimizationDisabled(false)
                .build();
    }
}
