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
package org.graylog2.migrations;

import com.google.common.collect.ImmutableMap;
import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetValidator;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.mongojack.DBQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.util.Locale.US;
import static java.util.Objects.requireNonNull;

public class V20210705181600_AddFailureIndexSetsMigration extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20210705181600_AddFailureIndexSetsMigration.class);

    private final ElasticsearchConfiguration elasticsearchConfiguration;
    private final MongoIndexSet.Factory mongoIndexSetFactory;
    private final IndexSetService indexSetService;
    private final IndexSetValidator indexSetValidator;
    private final StreamService streamService;

    @Inject
    public V20210705181600_AddFailureIndexSetsMigration(ElasticsearchConfiguration elasticsearchConfiguration,
                                                        MongoIndexSet.Factory mongoIndexSetFactory,
                                                        IndexSetService indexSetService,
                                                        IndexSetValidator indexSetValidator,
                                                        StreamService streamService) {
        this.elasticsearchConfiguration = elasticsearchConfiguration;
        this.mongoIndexSetFactory = mongoIndexSetFactory;
        this.indexSetService = indexSetService;
        this.indexSetValidator = indexSetValidator;
        this.streamService = streamService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2021-07-05T18:14:00Z");
    }

    @Override
    public void upgrade() {
        ensureStreamAndIndexSet(
                "Graylog Message Failures",
                "Contains messages that failed to be processed or indexed.",
                elasticsearchConfiguration.getGetDefaultFailuresIndexPrefix(),
                ElasticsearchConfiguration.DEFAULT_FAILURES_INDEX_PREFIX,
                Stream.FAILURES_STREAM_ID,
                "Processing and Indexing Failures",
                "Stream containing messages that failed to be processed or indexed"
        );
    }

    private void ensureStreamAndIndexSet(String indexSetTitle,
                                         String indexSetDescription,
                                         String indexPrefix,
                                         String indexPrefixConfigKey,
                                         String streamId,
                                         String streamTitle,
                                         String streamDescription) {
        checkIndexPrefixConflicts(indexPrefix, indexPrefixConfigKey);

        final IndexSet indexSet = setupFailureIndexSet(indexSetTitle, indexSetDescription, indexPrefix);
        try {
            streamService.load(streamId);
        } catch (NotFoundException ignored) {
            createStream(streamId, streamTitle, streamDescription, indexSet);
        }
    }

    private void checkIndexPrefixConflicts(String indexPrefix, String configKey) {
        final DBQuery.Query query = DBQuery.and(
                DBQuery.notEquals(IndexSetConfig.FIELD_INDEX_TEMPLATE_TYPE, Optional.of(IndexSetConfig.TemplateType.FAILURES)),
                DBQuery.is(IndexSetConfig.FIELD_INDEX_PREFIX, indexPrefix)
        );

        if (indexSetService.findOne(query).isPresent()) {
            final String msg = String.format(US, "Index prefix conflict: a non-failure index-set with prefix <%s> already exists. Configure a different <%s> value in the server config file.",
                    indexPrefix, configKey);
            throw new IllegalStateException(msg);
        }
    }

    private Optional<IndexSetConfig> getEventsIndexSetConfig(String indexPrefix) {
        final DBQuery.Query query = DBQuery.and(
                DBQuery.is(IndexSetConfig.FIELD_INDEX_TEMPLATE_TYPE, Optional.of(IndexSetConfig.TemplateType.FAILURES)),
                DBQuery.is(IndexSetConfig.FIELD_INDEX_PREFIX, indexPrefix)
        );
        return indexSetService.findOne(query);
    }

    private IndexSet setupFailureIndexSet(String indexSetTitle, String indexSetDescription, String indexPrefix) {
        final Optional<IndexSetConfig> optionalIndexSetConfig = getEventsIndexSetConfig(indexPrefix);
        if (optionalIndexSetConfig.isPresent()) {
            return mongoIndexSetFactory.create(optionalIndexSetConfig.get());
        }

        final IndexSetConfig indexSetConfig = IndexSetConfig.builder()
                .title(indexSetTitle)
                .description(indexSetDescription)
                .indexTemplateType(IndexSetConfig.TemplateType.FAILURES)
                .isWritable(true)
                .indexPrefix(indexPrefix)
                .shards(elasticsearchConfiguration.getShards()) // TODO do these defaults make sense here?
                .replicas(elasticsearchConfiguration.getReplicas()) // TODO do these defaults make sense here?
                // TODO do these defaults make sense here?
                .rotationStrategyClass(TimeBasedRotationStrategy.class.getCanonicalName())
                .rotationStrategy(TimeBasedRotationStrategyConfig.create(Period.months(1)))
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategy(DeletionRetentionStrategyConfig.create(12))
                .creationDate(ZonedDateTime.now(ZoneOffset.UTC))
                .indexAnalyzer(elasticsearchConfiguration.getAnalyzer())
                .indexTemplateName(indexPrefix + "-template")
                .indexOptimizationMaxNumSegments(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments())
                .indexOptimizationDisabled(elasticsearchConfiguration.isDisableIndexOptimization())
                .fieldTypeRefreshInterval(Duration.standardMinutes(1)) //TODO would the fields ever change?
                .build();

        try {
            final Optional<IndexSetValidator.Violation> violation = indexSetValidator.validate(indexSetConfig);
            if (violation.isPresent()) {
                throw new RuntimeException(violation.get().message());
            }

            final IndexSetConfig savedIndexSet = indexSetService.save(indexSetConfig);

            LOG.info("Successfully created \"failures\" index-set <{}/{}>", savedIndexSet.id(), savedIndexSet.title());

            return mongoIndexSetFactory.create(savedIndexSet);
        } catch (DuplicateKeyException e) {
            LOG.error("Couldn't create index-set <{}/{}>", indexSetTitle, indexPrefix);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void createStream(String streamId, String streamTitle, String streamDescription, IndexSet indexSet) {
        final ObjectId id = new ObjectId(streamId);
        final Map<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put(StreamImpl.FIELD_TITLE, streamTitle)
                .put(StreamImpl.FIELD_DESCRIPTION, streamDescription)
                .put(StreamImpl.FIELD_DISABLED, false)
                .put(StreamImpl.FIELD_CREATED_AT, DateTime.now(DateTimeZone.UTC))
                .put(StreamImpl.FIELD_CREATOR_USER_ID, "admin")
                .put(StreamImpl.FIELD_MATCHING_TYPE, StreamImpl.MatchingType.DEFAULT.name())
                .put(StreamImpl.FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM, true)
                .put(StreamImpl.FIELD_INDEX_SET_ID, requireNonNull(indexSet.getConfig().id(), "index set ID cannot be null"))
                .put(StreamImpl.FIELD_DEFAULT_STREAM, false)
                .build();
        final Stream stream = new StreamImpl(id, fields, Collections.emptyList(), Collections.emptySet(), indexSet);

        try {
            streamService.save(stream);
            LOG.info("Successfully created \"failures\" stream <{}/{}>", stream.getId(), stream.getTitle());
        } catch (ValidationException e) {
            LOG.error("Couldn't create failures stream <{}/{}>! This is a bug!", streamId, streamTitle, e);
        }
    }
}
