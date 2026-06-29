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

import org.bson.conversions.Bson;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.cluster.lock.Lock;
import org.graylog2.cluster.lock.LockService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReindexOutdatedIndexJobHandlerTest {

    @Mock
    DBJobDefinitionService jobDefinitionService;
    @Mock
    DBJobTriggerService jobTriggerService;
    @Mock
    JobSchedulerClock clock;
    @Mock
    LockService lockService;
    @Mock
    Lock lock;

    private ReindexOutdatedIndexJobHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReindexOutdatedIndexJobHandler(jobDefinitionService, jobTriggerService, clock, lockService);
        // Default: lock acquisition succeeds; the contention test overrides this.
        lenient().when(lockService.lock(anyString())).thenReturn(Optional.of(lock));
        lenient().when(clock.nowUTC()).thenReturn(DateTime.now(DateTimeZone.UTC));
    }

    private static JobTriggerDto activeTriggerForIndex(String index) {
        return JobTriggerDto.builder()
                .id("000000000000000000000001")
                .jobDefinitionType(ReindexOutdatedIndexJob.TYPE_NAME)
                .jobDefinitionId(ReindexOutdatedIndexJob.DEFINITION_INSTANCE.id())
                .schedule(OnceJobSchedule.create())
                .nextTime(DateTime.now(DateTimeZone.UTC))
                .data(ReindexOutdatedIndexJob.Data.builder()
                        .index(index)
                        .withReplicas(true)
                        .triggeredBy("bob")
                        .build())
                .build();
    }

    private void mockNoActiveTrigger() {
        when(jobTriggerService.streamByQuery(any(Bson.class))).thenReturn(Stream.empty());
    }

    private void mockDefinitionPresent() {
        when(jobDefinitionService.get(ReindexOutdatedIndexJob.DEFINITION_INSTANCE.id()))
                .thenReturn(Optional.of(ReindexOutdatedIndexJob.DEFINITION_INSTANCE));
    }

    @Test
    void start_failsWhenLockNotAcquired() {
        when(lockService.lock(ReindexOutdatedIndexJobHandler.START_LOCK_PREFIX + ".idx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.start(".idx", true, "alice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("currently being started");

        verify(jobTriggerService, never()).create(any());
    }

    @Test
    void start_failsWhenActiveJobExistsForSameIndex() {
        when(jobTriggerService.streamByQuery(any(Bson.class))).thenReturn(Stream.of(activeTriggerForIndex(".idx")));

        assertThatThrownBy(() -> handler.start(".idx", true, "alice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in progress");

        verify(jobTriggerService, never()).create(any());
        verify(lockService).unlock(lock);
    }

    @Test
    void start_allowsConcurrentReindexOfDifferentIndex() {
        // An active job exists, but for a *different* index — must not block.
        when(jobTriggerService.streamByQuery(any(Bson.class))).thenReturn(Stream.of(activeTriggerForIndex(".other")));
        mockDefinitionPresent();
        when(jobTriggerService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        final var created = handler.start(".idx", true, "alice");

        assertThat(created).isNotNull();
        verify(jobTriggerService).create(any());
        verify(lockService).unlock(lock);
    }

    @Test
    void start_createsTrigger_andReleasesLock_onSuccess() {
        mockNoActiveTrigger();
        mockDefinitionPresent();
        when(jobTriggerService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        final var created = handler.start(".idx", true, "alice");

        assertThat(created).isNotNull();
        verify(jobTriggerService).create(any());
        verify(lockService).unlock(lock);
    }

    @Test
    void start_lazyCreatesJobDefinition_whenAbsent() {
        mockNoActiveTrigger();
        when(jobDefinitionService.get(ReindexOutdatedIndexJob.DEFINITION_INSTANCE.id())).thenReturn(Optional.empty());
        when(jobDefinitionService.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jobTriggerService.create(any())).thenAnswer(inv -> inv.getArgument(0));

        handler.start(".idx", true, "alice");

        verify(jobDefinitionService).save(eq(ReindexOutdatedIndexJob.DEFINITION_INSTANCE));
    }

    @Test
    void start_buildsTriggerWithCorrectTypeScheduleAndData() {
        mockNoActiveTrigger();
        mockDefinitionPresent();
        final var captor = ArgumentCaptor.forClass(JobTriggerDto.class);
        when(jobTriggerService.create(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        handler.start(".idx", false, "alice");

        final JobTriggerDto trigger = captor.getValue();
        assertThat(trigger.jobDefinitionType()).isEqualTo(ReindexOutdatedIndexJob.TYPE_NAME);
        assertThat(trigger.jobDefinitionId()).isEqualTo(ReindexOutdatedIndexJob.DEFINITION_INSTANCE.id());
        assertThat(trigger.schedule()).isInstanceOf(OnceJobSchedule.class);
        assertThat(trigger.status()).isEqualTo(JobTriggerStatus.RUNNABLE);

        final var data = (ReindexOutdatedIndexJob.Data) trigger.data().orElseThrow();
        assertThat(data.index()).isEqualTo(".idx");
        assertThat(data.withReplicas()).isFalse();
        assertThat(data.triggeredBy()).isEqualTo("alice");
    }
}
