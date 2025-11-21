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

import jakarta.inject.Inject;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.indexer.IndexSet;
import org.graylog2.periodical.IndexRangesCleanupPeriodical;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class IndexJobsService {
    private final DBJobDefinitionService jobDefinitionService;
    private final DBJobTriggerService jobTriggerService;
    private final IndexRangesCleanupPeriodical indexRangesCleanupPeriodical;

    @Inject
    public IndexJobsService(final DBJobDefinitionService jobDefinitionService,
                            final DBJobTriggerService jobTriggerService,
                            final IndexRangesCleanupPeriodical indexRangesCleanupPeriodical) {
        this.jobDefinitionService = jobDefinitionService;
        this.jobTriggerService = jobTriggerService;
        this.indexRangesCleanupPeriodical = indexRangesCleanupPeriodical;
    }

    public void submitIndexRangesCleanupJob() {
        indexRangesCleanupPeriodical.doRun();
    }

    public void submitOptimizeIndexJob(final String indexName, final int maxNumSegments) {
        final var jobDefinition = jobDefinitionService.findOrCreate(OptimizeIndexJob.DEFINITION_INSTANCE);

        final var data = OptimizeIndexJob.Data.builder().indexName(indexName).maxNumSegments(maxNumSegments).build();
        final var trigger = JobTriggerDto.builder()
                .jobDefinitionId(jobDefinition.id())
                .jobDefinitionType(OptimizeIndexJob.TYPE_NAME)
                .schedule(OnceJobSchedule.create())
                .data(data)
                .build();

        jobTriggerService.create(trigger);
    }

    public void submitCreateNewSingleIndexRangeJob(final String indexName) {
        final var jobDefinition = jobDefinitionService.findOrCreate(CreateNewSingleIndexRangeJob.DEFINITION_INSTANCE);

        final var data = CreateNewSingleIndexRangeJob.Data.builder().indexName(indexName).build();
        final var trigger = JobTriggerDto.builder()
                .jobDefinitionId(jobDefinition.id())
                .jobDefinitionType(CreateNewSingleIndexRangeJob.TYPE_NAME)
                .schedule(OnceJobSchedule.create())
                .data(data)
                .build();

        jobTriggerService.create(trigger);
    }

    public void submitRebuildIndexRangesJob(final Set<IndexSet> indexSets) {
        final var jobDefinition = jobDefinitionService.findOrCreate(RebuildIndexRangesJob.DEFINITION_INSTANCE);

        final var data = RebuildIndexRangesJob.Data.builder().indexSets(indexSets).build();
        final var trigger = JobTriggerDto.builder()
                .jobDefinitionId(jobDefinition.id())
                .jobDefinitionType(RebuildIndexRangesJob.TYPE_NAME)
                .schedule(OnceJobSchedule.create())
                .data(data)
                .build();

        jobTriggerService.create(trigger);
    }

    public void submitIndexSetCleanupJob(final IndexSet indexSet) {
        final var jobDefinition = jobDefinitionService.findOrCreate(IndexSetCleanupJob.DEFINITION_INSTANCE);

        final var data = IndexSetCleanupJob.Data.builder().indexSet(indexSet).build();
        final var trigger = JobTriggerDto.builder()
                .jobDefinitionId(jobDefinition.id())
                .jobDefinitionType(IndexSetCleanupJob.TYPE_NAME)
                .schedule(OnceJobSchedule.create())
                .data(data)
                .build();

        jobTriggerService.create(trigger);
    }

    public void submitSetIndexReadOnlyAndCalculateRangeJob(final String indexName) {
        final var jobDefinition = jobDefinitionService.findOrCreate(SetIndexReadOnlyAndCalculateRangeJob.DEFINITION_INSTANCE);

        final var data = SetIndexReadOnlyAndCalculateRangeJob.Data.builder().indexName(indexName).build();
        final var trigger = JobTriggerDto.builder()
                .jobDefinitionId(jobDefinition.id())
                .jobDefinitionType(IndexSetCleanupJob.TYPE_NAME)
                .schedule(IntervalJobSchedule.builder().interval(30).unit(TimeUnit.SECONDS).build())
                .data(data)
                .build();

        jobTriggerService.create(trigger);
    }
}
