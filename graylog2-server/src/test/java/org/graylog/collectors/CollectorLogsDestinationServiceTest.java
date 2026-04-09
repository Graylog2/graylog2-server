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
package org.graylog.collectors;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.graylog.collectors.indexer.CollectorLogsIndexTemplateProvider;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog.collectors.input.processor.CollectorLogRecordProcessor;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetConfigFactory;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.validation.IndexSetValidator;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.joda.time.Period;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectorLogsDestinationServiceTest {

    @Mock
    private IndexSetService indexSetService;
    @Mock
    private IndexSetConfigFactory indexSetConfigFactory;
    @Mock
    private IndexSetValidator indexSetValidator;
    @Mock
    private StreamService streamService;
    @Mock
    private StreamRuleService streamRuleService;

    private CollectorLogsDestinationService service;

    @BeforeEach
    void setUp() {
        service = new CollectorLogsDestinationService(
                indexSetService, indexSetConfigFactory, indexSetValidator,
                streamService, streamRuleService
        );

        final var subject = mock(Subject.class);
        lenient().when(subject.getPrincipal()).thenReturn("admin");
        ThreadContext.bind(subject);
    }

    @AfterEach
    void tearDown() {
        ThreadContext.unbindSubject();
    }

    @Test
    void firstCallCreatesIndexSetStreamAndStreamRule() throws Exception {
        stubIndexSetDoesNotExist();
        stubStreamDoesNotExist();
        stubStreamRuleCount(0);
        when(streamRuleService.create(anyMap())).thenReturn(mock(StreamRule.class));

        service.ensureExists();

        verify(indexSetService).save(any(IndexSetConfig.class));
        verify(streamService).save(any(StreamImpl.class));
        verify(streamRuleService).save(any(StreamRule.class));
    }

    @Test
    void secondCallIsNoOp() throws Exception {
        stubIndexSetExists();
        stubStreamExists();
        stubStreamRuleCount(1);

        service.ensureExists();

        verify(indexSetService, never()).save(any(IndexSetConfig.class));
        verify(streamService, never()).save(any(StreamImpl.class));
        verify(streamRuleService, never()).save(any(StreamRule.class));
    }

    @Test
    void indexSetConfigHasCorrectProperties() throws Exception {
        stubIndexSetDoesNotExist();
        stubStreamDoesNotExist();
        stubStreamRuleCount(0);
        when(streamRuleService.create(anyMap())).thenReturn(mock(StreamRule.class));

        service.ensureExists();

        final var captor = ArgumentCaptor.forClass(IndexSetConfig.class);
        verify(indexSetService).save(captor.capture());
        final var saved = captor.getValue();

        assertThat(saved.isRegular()).hasValue(false);
        assertThat(saved.indexPrefix()).isEqualTo("gl-collector-logs");
        assertThat(saved.indexTemplateName()).isEqualTo("gl-collector-logs-template");
        assertThat(saved.indexTemplateType()).hasValue(CollectorLogsIndexTemplateProvider.COLLECTOR_LOGS_TEMPLATE_TYPE);
        assertThat(saved.dataTieringConfig()).isNull();
        assertThat(saved.isWritable()).isTrue();
        assertThat(saved.title()).isEqualTo("Collector Logs");
        assertThat(saved.description()).isEqualTo("Index set for collector self-log messages");
        assertThat(saved.rotationStrategyClass())
                .isEqualTo(TimeBasedSizeOptimizingStrategy.class.getCanonicalName());
        assertThat(saved.rotationStrategyConfig()).isInstanceOf(TimeBasedSizeOptimizingStrategyConfig.class);
        final var rotationConfig = (TimeBasedSizeOptimizingStrategyConfig) saved.rotationStrategyConfig();
        assertThat(rotationConfig.indexLifetimeMin()).isEqualTo(Period.days(14));
        assertThat(rotationConfig.indexLifetimeMax()).isEqualTo(Period.days(21));
        assertThat(saved.retentionStrategyClass())
                .isEqualTo(DeletionRetentionStrategy.class.getCanonicalName());
        assertThat(saved.retentionStrategyConfig()).isInstanceOf(DeletionRetentionStrategyConfig.class);
    }

    @Test
    void streamHasCorrectProperties() throws Exception {
        stubIndexSetDoesNotExist();
        stubStreamDoesNotExist();
        stubStreamRuleCount(0);
        when(streamRuleService.create(anyMap())).thenReturn(mock(StreamRule.class));

        service.ensureExists();

        final var captor = ArgumentCaptor.forClass(StreamImpl.class);
        verify(streamService).save(captor.capture());
        final var saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(Stream.COLLECTOR_LOGS_STREAM_ID);
        assertThat(saved.getTitle()).isEqualTo("Collector Logs");
        assertThat(saved.removeMatchesFromDefaultStream()).isTrue();
        assertThat(saved.getScope()).isEqualTo(ImmutableSystemScope.NAME);
        assertThat(saved.getDisabled()).isFalse();
        assertThat(saved.isDefault()).isFalse();
        assertThat(saved.indexSetId()).isEqualTo("saved-index-set-id");
    }

    @SuppressWarnings("unchecked")
    @Test
    void streamRuleHasCorrectProperties() throws Exception {
        stubIndexSetDoesNotExist();
        stubStreamDoesNotExist();
        stubStreamRuleCount(0);
        when(streamRuleService.create(anyMap())).thenReturn(mock(StreamRule.class));

        service.ensureExists();

        final var mapCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(streamRuleService).create(mapCaptor.capture());
        final var ruleData = (java.util.Map<String, Object>) mapCaptor.getValue();

        assertThat(ruleData.get("stream_id")).hasToString(Stream.COLLECTOR_LOGS_STREAM_ID);
        assertThat(ruleData.get("field")).isEqualTo(CollectorIngestCodec.FIELD_COLLECTOR_SOURCE_TYPE);
        assertThat(ruleData.get("type")).isEqualTo(StreamRuleType.EXACT.toInteger());
        assertThat(ruleData.get("value")).isEqualTo(CollectorLogRecordProcessor.RECEIVER_TYPE);
        assertThat(ruleData.get("inverted")).isEqualTo(false);
        verify(streamRuleService).save(any(StreamRule.class));
    }

    // --- Helpers ---

    private void stubIndexSetDoesNotExist() {
        when(indexSetService.findOne(any())).thenReturn(Optional.empty());
        final var defaultBuilder = IndexSetConfig.builder()
                .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
                .indexAnalyzer("standard")
                .shards(1)
                .replicas(0)
                .indexOptimizationDisabled(false)
                .indexOptimizationMaxNumSegments(1)
                .fieldTypeRefreshInterval(IndexSetConfig.DEFAULT_FIELD_TYPE_REFRESH_INTERVAL)
                .rotationStrategyClass("placeholder")
                .rotationStrategyConfig(TimeBasedSizeOptimizingStrategyConfig.builder()
                        .indexLifetimeMin(Period.days(30))
                        .indexLifetimeMax(Period.days(40))
                        .build())
                .retentionStrategyClass("placeholder")
                .retentionStrategyConfig(DeletionRetentionStrategyConfig.createDefault());
        when(indexSetConfigFactory.createDefault()).thenReturn(defaultBuilder);
        when(indexSetValidator.validate(any(IndexSetConfig.class))).thenReturn(Optional.empty());
        when(indexSetService.save(any(IndexSetConfig.class))).thenAnswer(invocation -> {
            final IndexSetConfig config = invocation.getArgument(0);
            return config.toBuilder().id("saved-index-set-id").build();
        });
    }

    private void stubIndexSetExists() {
        final var existingConfig = IndexSetConfig.builder()
                .id("existing-index-set-id")
                .title("Collector Logs")
                .indexPrefix("gl-collector-logs")
                .indexTemplateName("gl-collector-logs-template")
                .indexTemplateType(CollectorLogsIndexTemplateProvider.COLLECTOR_LOGS_TEMPLATE_TYPE)
                .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
                .indexAnalyzer("standard")
                .shards(1)
                .replicas(0)
                .indexOptimizationDisabled(false)
                .indexOptimizationMaxNumSegments(1)
                .fieldTypeRefreshInterval(IndexSetConfig.DEFAULT_FIELD_TYPE_REFRESH_INTERVAL)
                .rotationStrategyClass(TimeBasedSizeOptimizingStrategy.class.getCanonicalName())
                .rotationStrategyConfig(TimeBasedSizeOptimizingStrategyConfig.builder()
                        .indexLifetimeMin(Period.days(14))
                        .indexLifetimeMax(Period.days(21))
                        .build())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategyConfig(DeletionRetentionStrategyConfig.createDefault())
                .build();
        when(indexSetService.findOne(any())).thenReturn(Optional.of(existingConfig));
    }

    private void stubStreamDoesNotExist() throws NotFoundException {
        when(streamService.load(Stream.COLLECTOR_LOGS_STREAM_ID)).thenThrow(new NotFoundException("not found"));
    }

    private void stubStreamExists() throws NotFoundException {
        when(streamService.load(Stream.COLLECTOR_LOGS_STREAM_ID)).thenReturn(mock(StreamImpl.class));
    }

    private void stubStreamRuleCount(long count) {
        when(streamRuleService.streamRuleCount(Stream.COLLECTOR_LOGS_STREAM_ID)).thenReturn(count);
    }
}
