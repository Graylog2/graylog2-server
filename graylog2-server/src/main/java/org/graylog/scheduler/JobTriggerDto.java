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
package org.graylog.scheduler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog.scheduler.clock.JobSchedulerSystemClock;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = JobTriggerDto.Builder.class)
public abstract class JobTriggerDto {
    private static final String FIELD_ID = "id";
    public static final String FIELD_JOB_DEFINITION_ID = "job_definition_id";
    static final String FIELD_START_TIME = "start_time";
    static final String FIELD_END_TIME = "end_time";
    static final String FIELD_NEXT_TIME = "next_time";
    private static final String FIELD_CREATED_AT = "created_at";
    static final String FIELD_UPDATED_AT = "updated_at";
    static final String FIELD_TRIGGERED_AT = "triggered_at";
    static final String FIELD_STATUS = "status";
    static final String FIELD_LOCK = "lock";
    static final String FIELD_SCHEDULE = "schedule";
    static final String FIELD_DATA = "data";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_JOB_DEFINITION_ID)
    public abstract String jobDefinitionId();

    @JsonProperty(FIELD_START_TIME)
    public abstract DateTime startTime();

    @JsonProperty(FIELD_END_TIME)
    public abstract Optional<DateTime> endTime();

    @JsonProperty(FIELD_NEXT_TIME)
    public abstract DateTime nextTime();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract DateTime createdAt();

    @JsonProperty(FIELD_UPDATED_AT)
    public abstract DateTime updatedAt();

    @JsonProperty(FIELD_TRIGGERED_AT)
    public abstract Optional<DateTime> triggeredAt();

    @JsonProperty(FIELD_STATUS)
    public abstract JobTriggerStatus status();

    @JsonProperty(FIELD_LOCK)
    public abstract JobTriggerLock lock();

    @JsonProperty(FIELD_SCHEDULE)
    public abstract JobSchedule schedule();

    @JsonProperty(FIELD_DATA)
    public abstract Optional<JobTriggerData> data();

    public static Builder builder() {
        return Builder.create();
    }

    public static Builder builderWithClock(JobSchedulerClock clock) {
        return Builder.create(clock);
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return create(new JobSchedulerSystemClock());
        }

        public static Builder create(JobSchedulerClock clock) {
            final DateTime now = clock.nowUTC();

            return new AutoValue_JobTriggerDto.Builder()
                    .startTime(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .nextTime(now)
                    .status(JobTriggerStatus.RUNNABLE)
                    .lock(JobTriggerLock.empty());
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_JOB_DEFINITION_ID)
        public abstract Builder jobDefinitionId(String jobDefinitionId);

        @JsonProperty(FIELD_START_TIME)
        public abstract Builder startTime(DateTime startTime);

        @JsonProperty(FIELD_END_TIME)
        public abstract Builder endTime(@Nullable DateTime endTime);

        @JsonProperty(FIELD_NEXT_TIME)
        public abstract Builder nextTime(DateTime nextTime);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(DateTime createdAt);

        @JsonProperty(FIELD_UPDATED_AT)
        public abstract Builder updatedAt(DateTime updatedAt);

        @JsonProperty(FIELD_TRIGGERED_AT)
        public abstract Builder triggeredAt(@Nullable DateTime triggeredAt);

        @JsonProperty(FIELD_STATUS)
        public abstract Builder status(JobTriggerStatus status);

        @JsonProperty(FIELD_LOCK)
        public abstract Builder lock(JobTriggerLock lock);

        @JsonProperty(FIELD_SCHEDULE)
        public abstract Builder schedule(JobSchedule schedule);

        @JsonProperty(FIELD_DATA)
        public abstract Builder data(@Nullable JobTriggerData data);

        public abstract JobTriggerDto build();
    }
}
