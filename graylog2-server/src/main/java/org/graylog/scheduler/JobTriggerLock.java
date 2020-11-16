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
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

import javax.annotation.Nullable;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = JobTriggerLock.Builder.class)
public abstract class JobTriggerLock {
    static final String FIELD_OWNER = "owner";
    static final String FIELD_LAST_LOCK_TIME = "last_lock_time";
    static final String FIELD_CLOCK = "clock";
    static final String FIELD_PROGRESS = "progress";

    @JsonProperty(FIELD_OWNER)
    @Nullable
    public abstract String owner();

    @JsonProperty(FIELD_LAST_LOCK_TIME)
    @Nullable
    public abstract DateTime lastLockTime();

    @JsonProperty(FIELD_CLOCK)
    public abstract long clock();

    @JsonProperty(FIELD_PROGRESS)
    public abstract int progress();

    public static JobTriggerLock empty() {
        return builder().build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_JobTriggerLock.Builder()
                    .clock(0L)
                    .progress(0);
        }

        @JsonProperty(FIELD_OWNER)
        public abstract Builder owner(@Nullable String owner);

        @JsonProperty(FIELD_LAST_LOCK_TIME)
        public abstract Builder lastLockTime(@Nullable DateTime lastLockTime);

        @JsonProperty(FIELD_CLOCK)
        public abstract Builder clock(long clock);

        @JsonProperty(FIELD_PROGRESS)
        public abstract Builder progress(int progress);

        public abstract JobTriggerLock build();
    }
}
