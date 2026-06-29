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
package org.graylog2.indexer.indices;

import com.mongodb.client.model.Filters;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.cluster.lock.Lock;
import org.graylog2.cluster.lock.LockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Starts and tracks {@link ReindexOutdatedIndexJob} triggers. Triggering a reindex creates a one-shot job trigger that
 * the scheduler picks up and runs asynchronously, so the REST request that started it returns immediately.
 */
@Singleton
public class ReindexOutdatedIndexJobHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ReindexOutdatedIndexJobHandler.class);
    static final String START_LOCK_PREFIX = "reindex-outdated-index-start-";

    private final DBJobDefinitionService jobDefinitionService;
    private final DBJobTriggerService jobTriggerService;
    private final JobSchedulerClock clock;
    private final LockService lockService;

    @Inject
    public ReindexOutdatedIndexJobHandler(DBJobDefinitionService jobDefinitionService,
                                          DBJobTriggerService jobTriggerService,
                                          JobSchedulerClock clock,
                                          LockService lockService) {
        this.jobDefinitionService = jobDefinitionService;
        this.jobTriggerService = jobTriggerService;
        this.clock = clock;
        this.lockService = lockService;
    }

    public JobTriggerDto start(String index, boolean withReplicas, String triggeredBy) {
        // Cluster-wide, per-index lock around the find-active + create sequence so two concurrent reindex calls
        // (different Graylog nodes or the same node) cannot both pass the active-job check and insert duplicate
        // triggers — which would run two destructive reindexes on the same index simultaneously.
        final Lock lock = lockService.lock(START_LOCK_PREFIX + index)
                .orElseThrow(() -> new IllegalStateException(
                        "A reindex for index " + index + " is currently being started — please retry shortly."));
        try {
            if (findActiveForIndex(index).isPresent()) {
                throw new IllegalStateException("A reindex job for index " + index + " is already in progress");
            }

            final JobDefinitionDto definition = ensureDefinition();

            final ReindexOutdatedIndexJob.Data data = ReindexOutdatedIndexJob.Data.builder()
                    .index(index)
                    .withReplicas(withReplicas)
                    .triggeredBy(triggeredBy)
                    .build();

            final JobTriggerDto trigger = JobTriggerDto.builder()
                    .jobDefinitionType(ReindexOutdatedIndexJob.TYPE_NAME)
                    .jobDefinitionId(definition.id())
                    .data(data)
                    .schedule(OnceJobSchedule.create())
                    .nextTime(clock.nowUTC())
                    .build();

            final JobTriggerDto created = jobTriggerService.create(trigger);
            LOG.info("Started reindex job trigger {} for index {} by {}", created.id(), index, triggeredBy);
            return created;
        } finally {
            lockService.unlock(lock);
        }
    }

    public Optional<JobTriggerDto> findActiveForIndex(String index) {
        try (final var stream = jobTriggerService.streamByQuery(
                Filters.and(
                        typeFilter(),
                        Filters.nin(JobTriggerDto.FIELD_STATUS,
                                JobTriggerStatus.COMPLETE,
                                JobTriggerStatus.ERROR,
                                JobTriggerStatus.CANCELLED)))) {
            return stream
                    .filter(trigger -> trigger.data()
                            .filter(ReindexOutdatedIndexJob.Data.class::isInstance)
                            .map(ReindexOutdatedIndexJob.Data.class::cast)
                            .map(data -> data.index().equals(index))
                            .orElse(false))
                    .findFirst();
        }
    }

    private JobDefinitionDto ensureDefinition() {
        return jobDefinitionService.get(ReindexOutdatedIndexJob.DEFINITION_INSTANCE.id())
                .orElseGet(() -> jobDefinitionService.save(ReindexOutdatedIndexJob.DEFINITION_INSTANCE));
    }

    private static Bson typeFilter() {
        return Filters.eq(JobTriggerDto.FIELD_JOB_DEFINITION_TYPE, ReindexOutdatedIndexJob.TYPE_NAME);
    }
}
