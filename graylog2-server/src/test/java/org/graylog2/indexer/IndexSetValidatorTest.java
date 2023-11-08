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
import org.graylog2.datatier.tier.hot.HotTier;
import org.graylog2.datatier.tier.hot.HotTierValidator;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.plugin.rest.ValidationResult;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexSetValidatorTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private IndexSetRegistry indexSetRegistry;

    @Mock
    private ElasticsearchConfiguration elasticsearchConfiguration;

    private IndexSetValidator validator;

    @Before
    public void setUp() throws Exception {
        this.validator = new IndexSetValidator(indexSetRegistry, elasticsearchConfiguration, Set.of(new HotTierValidator(elasticsearchConfiguration)));
    }

    @Test
    public void validate() throws Exception {
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
        when(retentionStrategyConfig.validate(elasticsearchConfiguration)).thenReturn(new ValidationResult());

        final Optional<IndexSetValidator.Violation> violation = validator.validate(newConfig);

        assertThat(violation).isNotPresent();
    }

    @Test
    public void validateWhenAlreadyManaged() {
        final String prefix = "graylog_index";
        final IndexSetConfig newConfig = mock(IndexSetConfig.class);
        final IndexSet indexSet = mock(IndexSet.class);

        when(indexSet.getIndexPrefix()).thenReturn("foo");
        when(indexSetRegistry.isManagedIndex("graylog_index_0")).thenReturn(true);
        when(indexSetRegistry.iterator()).thenReturn(Collections.singleton(indexSet).iterator());
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

        when(elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod()).thenReturn(Period.days(1));

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

        // TimeBasedSizeOptimizingRotation validation
        final IndexSetConfig sizeOptimizingConfig = testIndexSetConfig().toBuilder()
                .rotationStrategyClass(TimeBasedSizeOptimizingStrategy.class.getCanonicalName())
                .dataTiers(List.of(HotTier.builder()
                        .indexLifetimeMin(Period.days(2))
                        .indexLifetimeMax(Period.days(30))
                        .build())
                )
                .build();
        assertThat(validator.validate(sizeOptimizingConfig)).hasValueSatisfying(v ->
                assertThat(v.message()).contains(
                        "Lifetime setting index_lifetime_max <P30D> exceeds the configured maximum of max_index_retention_period=P9D")
        );
    }

    @Test
    public void timeBasedSizeOptimizingOnlyWithMultipleOfDays() {
        when(elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod()).thenReturn(Period.days(1));

        when(indexSetRegistry.iterator()).thenReturn(Collections.emptyIterator());
        final IndexSetConfig sizeOptimizingConfig = testIndexSetConfig().toBuilder()
                .rotationStrategyClass(TimeBasedSizeOptimizingStrategy.class.getCanonicalName())
                .dataTiers(List.of(HotTier.builder()
                        .indexLifetimeMin(Period.days(2).withHours(2))
                        .indexLifetimeMax(Period.days(30))
                        .build())
                )
                .build();

        assertThat(validator.validate(sizeOptimizingConfig)).hasValueSatisfying(v ->
                assertThat(v.message()).contains(
                        "Lifetime setting index_lifetime_min <P2DT2H> can only be a multiple of days")
        );

        assertThat(validator.periodOtherThanDays(Period.days(5))).isFalse();
        assertThat(validator.periodOtherThanDays(Period.weeks(5))).isTrue();
        assertThat(validator.periodOtherThanDays(Period.days(5).withHours(3))).isTrue();
    }

    @Test
    public void timeBasedSizeOptimizingHonorsFixedLeeWay() {
        when(elasticsearchConfiguration.getTimeSizeOptimizingRotationPeriod()).thenReturn(Period.days(1));
        when(elasticsearchConfiguration.getTimeSizeOptimizingRetentionFixedLeeway()).thenReturn(Period.days(10));

        when(indexSetRegistry.iterator()).thenReturn(Collections.emptyIterator());
        final IndexSetConfig failingConfig = testIndexSetConfig().toBuilder()
                .rotationStrategyClass(TimeBasedSizeOptimizingStrategy.class.getCanonicalName())
                .dataTiers(List.of(HotTier.builder()
                        .indexLifetimeMin(Period.days(10))
                        .indexLifetimeMax(Period.days(19))
                        .build())
                )
                .build();

        assertThat(validator.validate(failingConfig)).hasValueSatisfying(v -> assertThat(v.message())
                .contains("The duration between index_lifetime_max and index_lifetime_min <P9D> " +
                        "cannot be shorter than time_size_optimizing_retention_fixed_leeway <P10D>"));

        final IndexSetConfig successfullConfig = testIndexSetConfig().toBuilder()
                .rotationStrategyClass(TimeBasedSizeOptimizingStrategy.class.getCanonicalName())
                .dataTiers(List.of(HotTier.builder()
                        .indexLifetimeMin(Period.days(10))
                        .indexLifetimeMax(Period.days(20))
                        .build())
                )
                .build();

        assertThat(validator.validate(successfullConfig)).isEmpty();
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
        ValidationResult validationResult = new ValidationResult().addError("fieldName", "error");
        when(retentionStrategyConfig.validate(elasticsearchConfiguration)).thenReturn(validationResult);

        final Optional<IndexSetValidator.Violation> violation = validator.validate(newConfig);

        assertThat(violation).isPresent();
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
