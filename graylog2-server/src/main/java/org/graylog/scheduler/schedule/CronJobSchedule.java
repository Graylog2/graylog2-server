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
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.graylog.scheduler.JobSchedule;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;

import static com.cronutils.model.CronType.QUARTZ;

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

    private static CronParser newCronParser() {
        return new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
    }

    @Override
    public Optional<DateTime> calculateNextTime(DateTime previousExecutionTime, DateTime lastNextTime, JobSchedulerClock clock) {
        final Cron cron = newCronParser().parse(cronExpression());
        final ExecutionTime executionTime = ExecutionTime.forCron(cron);

        ZonedDateTime zdt = getZonedDateTime(clock);

        return executionTime
                .nextExecution(zdt)
                .map(this::toDateTime);
    }

    private ZonedDateTime getZonedDateTime(JobSchedulerClock clock) {
        final DateTime now = clock.nowUTC();
        Instant instant = Instant.ofEpochMilli(now.getMillis());
        ZoneId zoneId = ZoneId.of(timezone().orElse(DEFAULT_TIMEZONE));
        return ZonedDateTime.ofInstant(instant, zoneId);
    }

    private DateTime toDateTime(ZonedDateTime t) {
        final DateTimeZone tz = DateTimeZone.forTimeZone(TimeZone.getTimeZone(t.getZone()));
        return new DateTime(t.toInstant().toEpochMilli(), tz);
    }

    @Override
    public Optional<Map<String, Object>> toDBUpdate(String fieldPrefix) {
        return Optional.of(ImmutableMap.of(
                fieldPrefix + JobSchedule.TYPE_FIELD, type(),
                fieldPrefix + FIELD_CRON_EXPRESSION, cronExpression(),
                fieldPrefix + FIELD_TIMEZONE, timezone().orElse(DEFAULT_TIMEZONE) // always store a TZ together with the cron expression
        ));
    }
    public static CronJobSchedule.Builder builder() {
        return CronJobSchedule.Builder.create();
    }

    public abstract CronJobSchedule.Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements JobSchedule.Builder<Builder> {

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
            validateCronExpression(schedule);
            return schedule;
        }

        /**
         * make sure that we don't allow any invalid cron expression, as we are accepting plain string
         * that could contain anything
         */
        private void validateCronExpression(CronJobSchedule schedule) {
            final Cron cron = newCronParser().parse(schedule.cronExpression());
            cron.validate();
        }
    }
}



