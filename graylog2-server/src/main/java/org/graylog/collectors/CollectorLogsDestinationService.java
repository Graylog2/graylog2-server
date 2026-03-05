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

import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import org.apache.shiro.SecurityUtils;
import org.bson.types.ObjectId;
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
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.graylog2.indexer.indexset.fields.IndexPrefixField.FIELD_INDEX_PREFIX;
import static org.graylog2.indexer.indexset.fields.IndexTemplateTypeField.FIELD_INDEX_TEMPLATE_TYPE;
import static org.graylog2.shared.utilities.StringUtils.f;

public class CollectorLogsDestinationService {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorLogsDestinationService.class);

    static final String COLLECTOR_LOGS_INDEX_PREFIX = "gl-collector-logs";

    private final IndexSetService indexSetService;
    private final IndexSetConfigFactory indexSetConfigFactory;
    private final IndexSetValidator indexSetValidator;
    private final StreamService streamService;
    private final StreamRuleService streamRuleService;

    @Inject
    public CollectorLogsDestinationService(IndexSetService indexSetService,
                                           IndexSetConfigFactory indexSetConfigFactory,
                                           IndexSetValidator indexSetValidator,
                                           StreamService streamService,
                                           StreamRuleService streamRuleService) {
        this.indexSetService = indexSetService;
        this.indexSetConfigFactory = indexSetConfigFactory;
        this.indexSetValidator = indexSetValidator;
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
    }

    public void ensureExists() {
        final String indexSetId = ensureIndexSet();
        ensureStream(indexSetId);
        ensureStreamRule();
    }

    private String ensureIndexSet() {
        final var query = Filters.and(
                Filters.eq(FIELD_INDEX_TEMPLATE_TYPE,
                        Optional.of(CollectorLogsIndexTemplateProvider.COLLECTOR_LOGS_TEMPLATE_TYPE)),
                Filters.eq(FIELD_INDEX_PREFIX, COLLECTOR_LOGS_INDEX_PREFIX)
        );
        final Optional<IndexSetConfig> existing = indexSetService.findOne(query);
        if (existing.isPresent()) {
            return requireNonNull(existing.get().id(), "index set ID cannot be null");
        }

        final IndexSetConfig indexSetConfig = indexSetConfigFactory.createDefault()
                .title("Collector Logs")
                .description("Index set for collector self-log messages")
                .indexTemplateType(CollectorLogsIndexTemplateProvider.COLLECTOR_LOGS_TEMPLATE_TYPE)
                .isWritable(true)
                .isRegular(false)
                .indexPrefix(COLLECTOR_LOGS_INDEX_PREFIX)
                .indexTemplateName(COLLECTOR_LOGS_INDEX_PREFIX + "-template")
                .rotationStrategyClass(TimeBasedSizeOptimizingStrategy.class.getCanonicalName())
                .rotationStrategyConfig(TimeBasedSizeOptimizingStrategyConfig.builder()
                        .indexLifetimeMin(Period.days(14))
                        .indexLifetimeMax(Period.days(21))
                        .build())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategyConfig(DeletionRetentionStrategyConfig.createDefault())
                .dataTieringConfig(null)
                .build();

        final Optional<IndexSetValidator.Violation> violation = indexSetValidator.validate(indexSetConfig);
        if (violation.isPresent()) {
            throw new InternalServerErrorException(
                    f("Collector logs index set validation failed: %s", violation.get().message()));
        }

        final IndexSetConfig saved = indexSetService.save(indexSetConfig);
        LOG.info("Created collector logs index set <{}/{}>", saved.id(), saved.title());
        return requireNonNull(saved.id(), "index set ID cannot be null");
    }

    private void ensureStream(String indexSetId) {
        try {
            streamService.load(Stream.COLLECTOR_LOGS_STREAM_ID);
            return;
        } catch (NotFoundException ignored) {
            // Stream does not exist, create it
        }

        final String creatorUserId = SecurityUtils.getSubject().getPrincipal().toString();
        final var stream = StreamImpl.builder()
                .id(Stream.COLLECTOR_LOGS_STREAM_ID)
                .title("Collector Logs")
                .description("Stream containing collector self-logs from managed collectors")
                .disabled(false)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .creatorUserId(creatorUserId)
                .matchingType(StreamImpl.MatchingType.DEFAULT)
                .removeMatchesFromDefaultStream(true)
                .indexSetId(indexSetId)
                .isDefault(false)
                .scope(ImmutableSystemScope.NAME)
                .build();

        try {
            streamService.save(stream);
            LOG.info("Created collector logs stream <{}/{}>", stream.getId(), stream.getTitle());
        } catch (ValidationException e) {
            throw new InternalServerErrorException("Failed to create collector logs stream", e);
        }
    }

    private void ensureStreamRule() {
        if (streamRuleService.streamRuleCount(Stream.COLLECTOR_LOGS_STREAM_ID) > 0) {
            return;
        }

        final var rule = streamRuleService.create(Map.of(
                StreamRuleImpl.FIELD_STREAM_ID, new ObjectId(Stream.COLLECTOR_LOGS_STREAM_ID),
                StreamRuleImpl.FIELD_FIELD, CollectorIngestCodec.FIELD_COLLECTOR_RECEIVER_TYPE,
                StreamRuleImpl.FIELD_TYPE, StreamRuleType.EXACT.toInteger(),
                StreamRuleImpl.FIELD_VALUE, CollectorLogRecordProcessor.RECEIVER_TYPE,
                StreamRuleImpl.FIELD_INVERTED, false,
                StreamRuleImpl.FIELD_DESCRIPTION, "Route collector self-logs to dedicated stream"
        ));

        try {
            streamRuleService.save(rule);
            LOG.info("Created collector logs stream rule for stream <{}>", Stream.COLLECTOR_LOGS_STREAM_ID);
        } catch (ValidationException e) {
            throw new InternalServerErrorException("Failed to create collector logs stream rule", e);
        }
    }
}
