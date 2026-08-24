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
package org.graylog.scheduler.rest;

import org.graylog.scheduler.JobTriggerData;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog.scheduler.system.SystemJobDefinitionConfig;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobResourceHandlerServiceTest {
    private static final String JOB_TYPE = "test-job-v1";

    @Mock
    private JobResourceHandler handler;

    @Test
    void resolvesTheHandlerByJobDefinitionType() {
        when(handler.getTriggerDetails(any())).thenReturn(details());

        final var summary = service().jobSummaryFromTrigger(trigger(JOB_TYPE));

        assertThat(summary.info()).isEqualTo("job info");
    }

    @Test
    void fallsBackToTheTriggerDataTypeForSystemJobs() {
        when(handler.getTriggerDetails(any())).thenReturn(details());

        // System job triggers all carry the same job definition type, so the handler is found via the data type
        final var summary = service().jobSummaryFromTrigger(trigger(SystemJobDefinitionConfig.TYPE_NAME));

        assertThat(summary.info()).isEqualTo("job info");
    }

    @Test
    void ignoresTheTriggerDataTypeForOtherJobs() {
        // Only system jobs are looked up by their data type, so this trigger gets no details
        final var summary = service().jobSummaryFromTrigger(trigger("another-job-v1"));

        assertThat(summary.info()).isEqualTo(JobTriggerDetails.EMPTY_DETAILS.info());
        verify(handler, never()).getTriggerDetails(any());
    }

    @Test
    void fallsBackToEmptyDetailsWithoutAMatchingHandler() {
        final var summary = new JobResourceHandlerService(Map.of()).jobSummaryFromTrigger(trigger(JOB_TYPE));

        assertThat(summary.info()).isEqualTo(JobTriggerDetails.EMPTY_DETAILS.info());
    }

    private JobResourceHandlerService service() {
        return new JobResourceHandlerService(Map.of(JOB_TYPE, handler));
    }

    private static JobTriggerDetails details() {
        return JobTriggerDetails.create("job info", "job description", "SomeSystemJob", true);
    }

    private static JobTriggerDto trigger(String jobDefinitionType) {
        return JobTriggerDto.builder()
                .id("54e3deadbeefdeadbeef0000")
                .jobDefinitionId("job-definition-id")
                .jobDefinitionType(jobDefinitionType)
                .schedule(OnceJobSchedule.create())
                .nextTime(DateTime.now(DateTimeZone.UTC))
                .data(new TestData())
                .build();
    }

    private static class TestData implements JobTriggerData {
        @Override
        public String type() {
            return JOB_TYPE;
        }
    }
}
