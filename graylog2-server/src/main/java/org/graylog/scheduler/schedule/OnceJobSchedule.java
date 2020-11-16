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
package org.graylog.scheduler.schedule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import org.graylog.scheduler.JobSchedule;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.Optional;

@AutoValue
@JsonTypeName(OnceJobSchedule.TYPE_NAME)
@JsonDeserialize(builder = OnceJobSchedule.Builder.class)
public abstract class OnceJobSchedule implements JobSchedule {
    public static final String TYPE_NAME = "once";

    @JsonIgnore
    @Override
    public Optional<DateTime> calculateNextTime(DateTime lastExecutionTime, DateTime lastNextTime) {
        return Optional.empty();
    }

    @Override
    public Optional<Map<String, Object>> toDBUpdate(String fieldPrefix) {
        return Optional.of(ImmutableMap.of(fieldPrefix + JobSchedule.TYPE_FIELD, type()));
    }

    public static OnceJobSchedule create() {
        return builder().build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements JobSchedule.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_OnceJobSchedule.Builder().type(TYPE_NAME);
        }

        abstract OnceJobSchedule autoBuild();

        public OnceJobSchedule build() {
            // Make sure the type name is correct!
            type(TYPE_NAME);

            return autoBuild();
        }
    }
}
