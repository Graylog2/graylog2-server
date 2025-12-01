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
package org.graylog2.indexer.indices.jobs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.inject.Inject;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerData;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.indexer.ranges.MongoIndexRangeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class IndexSetCleanupJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetCleanupJob.class);

    public static final String TYPE_NAME = "index-set-cleanup-v1";
    public static final String JOB_ID = "662a07ad68699718ec07b133";
    public static final JobDefinitionDto DEFINITION_INSTANCE = JobDefinitionDto.builder()
            .id(JOB_ID) // This is a system entity and the ID MUST NOT change!
            .title("Rebuild Index Ranges")
            .description("Runs on demand and calculates ranges for indices that should have one but are missing them.")
            .config(Config.empty())
            .build();

    private final Indices indices;
    private final IndexRangeService indexRangeService;

    @Inject
    public IndexSetCleanupJob(final Indices indices,
                              final MongoIndexRangeService indexRangeService) {
        this.indices = indices;
        this.indexRangeService = indexRangeService;
    }

    public interface Factory extends Job.Factory<IndexSetCleanupJob> {
        @Override
        IndexSetCleanupJob create(JobDefinitionDto jobDefinition);
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        final JobTriggerDto trigger = ctx.trigger();
        final Data jobData = trigger.data()
                .map(d -> (Data) d)
                .orElseThrow(() -> new IllegalArgumentException("IndexSetCleanupJob job data not found"));

        final var indexSet = jobData.indexSet();
        final AtomicLong deleted = new AtomicLong(0L);

        final IndexSetConfig config = indexSet.getConfig();
        final String[] managedIndices = indexSet.getManagedIndices();

        final var total = managedIndices.length;

        try {
            LOG.info("Deleting index template <{}> from Elasticsearch", config.indexTemplateName());
            indices.deleteIndexTemplate(indexSet);
        } catch (Exception e) {
            LOG.error("Unable to delete index template <{}>", config.indexTemplateName(), e);
        }

        for (String indexName : managedIndices) {
            /* TODO: check for cancellation
            if (cancel) {
                LOG.info("Cancel requested. Deleted <{}> of <{}> indices.", deleted, total);
                break;
            }
            */
            try {
                LOG.info("Removing index range information for index: {}", indexName);
                indexRangeService.remove(indexName);

                LOG.info("Deleting index <{}> in index set <{}> ({})", indexName, config.id(), config.title());
                indices.delete(indexName);
                deleted.incrementAndGet();
            } catch (Exception e) {
                LOG.error("Unable to delete index <{}>", indexName, e);
            }
        }

        return JobTriggerUpdate.withStatusAndNoNextTime(JobTriggerStatus.COMPLETE);
    }

    @AutoValue
    @JsonTypeName(TYPE_NAME)
    public static abstract class Config implements JobDefinitionConfig {
        @JsonCreator
        public static Config create(@JsonProperty("type") String type) {
            return new AutoValue_IndexSetCleanupJob_Config(type);
        }

        public static Config empty() {
            return create(TYPE_NAME);
        }
    }

    @AutoValue
    @JsonTypeName(TYPE_NAME)
    @JsonDeserialize(builder = Data.Builder.class)
    public static abstract class Data implements JobTriggerData {
        private static final String FIELD_INDEX_SET = "index_set";

        @JsonProperty(FIELD_INDEX_SET)
        public abstract IndexSet indexSet();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public static abstract class Builder implements JobTriggerData.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_IndexSetCleanupJob_Data.Builder()
                        .type(TYPE_NAME);
            }

            @JsonProperty(FIELD_INDEX_SET)
            public abstract Builder indexSet(IndexSet indexSet);

            abstract Data autoBuild();

            public Data build() {
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }
}
