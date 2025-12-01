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
import com.google.common.base.Stopwatch;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
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
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.basic.BasicIndexSet;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.indexer.ranges.IndexRange;
import org.graylog2.indexer.ranges.IndexRangeService;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RebuildIndexRangesJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(RebuildIndexRangesJob.class);

    public static final String TYPE_NAME = "rebuild-index-ranges-v1";
    public static final String JOB_ID = "662a07ad68699718ec07b168";
    public static final JobDefinitionDto DEFINITION_INSTANCE = JobDefinitionDto.builder()
            .id(JOB_ID) // This is a system entity and the ID MUST NOT change!
            .title("Rebuild Index Ranges")
            .description("Runs on demand and calculates ranges for indices that should have one but are missing them.")
            .config(Config.empty())
            .build();

    private final ActivityWriter activityWriter;
    private final IndexRangeService indexRangeService;

    @Inject
    public RebuildIndexRangesJob(ActivityWriter activityWriter,
                                 IndexRangeService indexRangeService) {
        this.activityWriter = activityWriter;
        this.indexRangeService = indexRangeService;
    }

    public interface Factory extends Job.Factory<RebuildIndexRangesJob> {
        @Override
        RebuildIndexRangesJob create(JobDefinitionDto jobDefinition);
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        info("Recalculating index ranges.");

        final JobTriggerDto trigger = ctx.trigger();
        final Data jobData = trigger.data()
                .map(d -> (Data) d)
                .orElseThrow(() -> new IllegalArgumentException("RebuildIndexRangesJob job data not found"));

        // for each index set we know about
        final ListMultimap<BasicIndexSet, String> indexSets = MultimapBuilder.hashKeys().arrayListValues().build();
        for (BasicIndexSet indexSet : jobData.indexSets()) {
            final String[] managedIndicesNames = indexSet.getManagedIndices();
            for (String name : managedIndicesNames) {
                indexSets.put(indexSet, name);
            }
        }

        if (indexSets.isEmpty()) {
            info("No indices, nothing to calculate.");
            return JobTriggerUpdate.withStatusAndNoNextTime(JobTriggerStatus.COMPLETE);
        }
        final var indicesToCalculate = indexSets.values().size();
        final AtomicInteger indicesCalculated = new AtomicInteger(0);

        Stopwatch sw = Stopwatch.createStarted();
        for (BasicIndexSet indexSet : indexSets.keySet()) {
            LOG.info("Recalculating index ranges for index set {} ({}): {} indices affected.",
                    indexSet.getConfig().title(),
                    indexSet.getIndexWildcard(),
                    indexSets.get(indexSet).size());
            for (String index : indexSets.get(indexSet)) {
                try {
                    if (index.equals(indexSet.getActiveWriteIndex())) {
                        LOG.debug("{} is current write target, do not calculate index range for it", index);
                        final IndexRange emptyRange = indexRangeService.createUnknownRange(index);
                        try {
                            final IndexRange indexRange = indexRangeService.get(index);
                            if (indexRange.begin().getMillis() != 0 || indexRange.end().getMillis() != 0) {
                                LOG.info("Invalid date ranges for write index {}, resetting it.", index);
                                indexRangeService.save(emptyRange);
                            }
                        } catch (NotFoundException e) {
                            LOG.info("No index range found for write index {}, recreating it.", index);
                            indexRangeService.save(emptyRange);
                        }

                        indicesCalculated.incrementAndGet();
                        continue;
                    }
                } catch (TooManyAliasesException e) {
                    LOG.error("Multiple write alias targets found, this is a bug.");
                    indicesCalculated.incrementAndGet();
                    continue;
                }

                /* TODO cancelation handling necessary?
                if (cancelRequested) {
                    info("Stop requested. Not calculating next index range, not updating ranges.");
                    sw.stop();
                    return;
                }
                */

                try {
                    final IndexRange indexRange = indexRangeService.calculateRange(index);
                    indexRangeService.save(indexRange);
                    LOG.info("Created ranges for index {}: {}", index, indexRange);
                } catch (Exception e) {
                    LOG.info("Could not calculate range of index [" + index + "]. Skipping.", e);
                } finally {
                    indicesCalculated.incrementAndGet();
                }
            }
        }

        info("Done calculating index ranges for " + indicesToCalculate + " indices. Took " + sw.stop().elapsed(TimeUnit.MILLISECONDS) + "ms.");
        return JobTriggerUpdate.withStatusAndNoNextTime(JobTriggerStatus.COMPLETE);
    }

    protected void info(String what) {
        LOG.info(what);
        activityWriter.write(new Activity(what, RebuildIndexRangesJob.class));
    }

    @AutoValue
    @JsonTypeName(TYPE_NAME)
    public static abstract class Config implements JobDefinitionConfig {
        @JsonCreator
        public static Config create(@JsonProperty("type") String type) {
            return new AutoValue_RebuildIndexRangesJob_Config(type);
        }

        public static Config empty() {
            return create(TYPE_NAME);
        }
    }

    @AutoValue
    @JsonTypeName(TYPE_NAME)
    @JsonDeserialize(builder = Data.Builder.class)
    public static abstract class Data implements JobTriggerData {
        private static final String FIELD_INDEX_SETS = "index_sets";

        @JsonProperty(FIELD_INDEX_SETS)
        public abstract Set<BasicIndexSet> indexSets();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public static abstract class Builder implements JobTriggerData.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_RebuildIndexRangesJob_Data.Builder()
                        .type(TYPE_NAME);
            }

            @JsonProperty(FIELD_INDEX_SETS)
            public abstract Builder indexSets(Set<BasicIndexSet> indexSets);

            abstract Data autoBuild();

            public Data build() {
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }
}

