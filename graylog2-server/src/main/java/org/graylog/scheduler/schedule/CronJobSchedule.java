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

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.scheduler.JobSchedule;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.TimeZone;

@AutoValue
@JsonTypeName(CronJobSchedule.TYPE_NAME)
@JsonDeserialize(builder = CronJobSchedule.Builder.class)
public abstract class CronJobSchedule implements JobSchedule {
    public static final String TYPE_NAME = "cron";

    public static final String FIELD_CRON_EXPRESSION = "cron_expression";
    public static final String FIELD_TIMEZONE = "timezone";

    public static final String DEFAULT_TIMEZONE = "UTC";

    @JsonProperty(FIELD_CRON_EXPRESSION)
    public abstract String cronExpression();

    @JsonProperty(value = FIELD_TIMEZONE)
    abstract Optional<String> timezone();

    @Override
    public Optional<DateTime> calculateNextTime(DateTime previousExecutionTime, DateTime lastNextTime, JobSchedulerClock clock) {
        final Cron cron = CronUtils.getParser().parse(cronExpression());
        final ExecutionTime executionTime = ExecutionTime.forCron(cron);

        ZonedDateTime zdt = getZonedDateTime(lastNextTime == null ? clock.nowUTC() : lastNextTime);

        return executionTime
                .nextExecution(zdt)
                .map(this::toDateTime);
    }

    private ZonedDateTime getZonedDateTime(DateTime dt) {
        Instant instant = Instant.ofEpochMilli(dt.getMillis());
        ZoneId zoneId = ZoneId.of(timezone().orElse(DEFAULT_TIMEZONE), ZoneId.SHORT_IDS);
        return ZonedDateTime.ofInstant(instant, zoneId);
    }

    private DateTime toDateTime(ZonedDateTime t) {
        final DateTimeZone tz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(t.getZone()));
        return new DateTime(t.toInstant().toEpochMilli(), tz);
    }

    public static CronJobSchedule.Builder builder() {
        return CronJobSchedule.Builder.create();
    }

    public abstract CronJobSchedule.Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder implements JobSchedule.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_CronJobSchedule.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_CRON_EXPRESSION)
        public abstract Builder cronExpression(String cronExpression);

        @JsonProperty(FIELD_TIMEZONE)
        public abstract Builder timezone(@Nullable String timezone);

        abstract CronJobSchedule autoBuild();

        public CronJobSchedule build() {
            // Make sure the type name is correct!
            type(TYPE_NAME);
            final CronJobSchedule schedule = autoBuild();
            // make sure that we don't allow any invalid cron expression, as we are accepting plain string that could
            // contain anything
            CronUtils.validateExpression(schedule.cronExpression());
            return schedule;
        }
    }
}



