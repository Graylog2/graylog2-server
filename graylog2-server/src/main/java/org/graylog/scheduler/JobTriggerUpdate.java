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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = JobTriggerUpdate.Builder.class)
public abstract class JobTriggerUpdate {
    public abstract Optional<DateTime> nextTime();

    public abstract Optional<JobTriggerData> data();

    public abstract Optional<JobTriggerStatus> status();

    public static JobTriggerUpdate withNextTime(DateTime nextTime) {
        return builder().nextTime(nextTime).build();
    }

    /**
     * Create job trigger update without next time. That means the trigger will not be scheduled anymore and will
     * be marked as {@link JobTriggerStatus#COMPLETE}.
     *
     * @return the job trigger update object
     */
    public static JobTriggerUpdate withoutNextTime() {
        return builder().nextTime(null).build();
    }

    public static JobTriggerUpdate withError(JobTriggerDto trigger) {
        // On error we keep the previous nextTime
        return builder().nextTime(trigger.nextTime()).status(JobTriggerStatus.ERROR).build();
    }

    public static JobTriggerUpdate withNextTimeAndData(DateTime nextTime, JobTriggerData data) {
        return builder().nextTime(nextTime).data(data).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_JobTriggerUpdate.Builder();
        }

        public abstract Builder nextTime(@Nullable DateTime nextTime);

        public abstract Builder data(@Nullable JobTriggerData data);

        public abstract Builder status(@Nullable JobTriggerStatus status);

        public abstract JobTriggerUpdate build();
    }
}
