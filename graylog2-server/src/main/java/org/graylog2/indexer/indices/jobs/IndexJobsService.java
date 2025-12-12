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
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class IndexJobsService {
    private final DBJobDefinitionService jobDefinitionService;
    private final DBJobTriggerService jobTriggerService;

    @Inject
    public IndexJobsService(final DBJobDefinitionService jobDefinitionService,
                            final DBJobTriggerService jobTriggerService) {
        this.jobDefinitionService = jobDefinitionService;
        this.jobTriggerService = jobTriggerService;
    }

    public void submitSetIndexReadOnlyAndCalculateRangeJob(final String indexName) {
        final var jobDefinition = jobDefinitionService.findOrCreate(SetIndexReadOnlyAndCalculateRangeJob.DEFINITION_INSTANCE);

        final var data = SetIndexReadOnlyAndCalculateRangeJob.Data.builder().indexName(indexName).build();
        final var trigger = JobTriggerDto.builder()
                .jobDefinitionId(jobDefinition.id())
                .jobDefinitionType(SetIndexReadOnlyAndCalculateRangeJob.TYPE_NAME)
                // schedule correctly with 30 seconds delay
                .startTime(DateTime.now(DateTimeZone.UTC).plusSeconds(30))
                .schedule(OnceJobSchedule.create())
                .data(data)
                .build();

        jobTriggerService.create(trigger);
    }
}
