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

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.time.Duration;
import java.util.Locale;

import static com.cronutils.model.CronType.QUARTZ;

public class CronUtils {

    private static final CronDescriptor DESCRIPTOR = CronDescriptor.instance(Locale.ENGLISH);
    private static final CronParser PARSER = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));

    public static CronDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static CronParser getParser() {
        return PARSER;
    }

    /**
     * Validates a cron expression and throws an exception if validation fails.
     *
     * @param expression cron expression to validate
     */
    public static Cron validateExpression(String expression) {
        final Cron cron = PARSER.parse(expression);
        cron.validate();
        return cron;
    }

    /**
     * Describes a cron expression in English
     *
     * @param expression cron expression to validate
     * @return description of the expression
     */
    public static String describeExpression(String expression) {
        final Cron cron = PARSER.parse(expression);
        return DESCRIPTOR.describe(cron);
    }

    public static boolean validateCronFrequency(String expression, String timezone, Duration allowedFrequency, JobSchedulerClock clock) {
        final CronJobSchedule schedule = CronJobSchedule.builder()
                .cronExpression(expression)
                .timezone(timezone)
                .build();

        // Calculate the next 3 execution times for this job. We can't use now as an execution time because it could be
        // an expression that runs on an exact hour/minute/second. Then we need two more to avoid the case where the
        // expression only runs on certain days/months/year and nextExecution ends up being the last execution time
        // before a very long break (eg, every 30 seconds on Monday-Friday, and we are validating it at 11:59:45PM on
        // Friday night). While there are likely still really crazy crons that could defeat this validation logic, it
        // should handle all reasonable(TM) expressions.
        final DateTime now = clock.now(DateTimeZone.forID(timezone));
        final DateTime nextExecution = nextExecution(schedule, now, clock);
        final DateTime nextExecution2 = nextExecution(schedule, nextExecution, clock);
        final DateTime nextExecution3 = nextExecution(schedule, nextExecution2, clock);

        // This cron expression is very soon not going to trigger again. While it isn't necessarily useful, it
        // technically won't run afoul of happening too frequently which is what this method is checking.
        if (nextExecution == null || nextExecution2 == null || nextExecution3 == null) {
            return true;
        }

        // Figure out the time interval between the next two executions and then the interval between the 2nd and 3rd
        // next executions. Then return if the smaller of the two is larger than the smallest valid interval.
        final long interval1 = nextExecution2.getMillis() - nextExecution.getMillis();
        final long interval2 = nextExecution3.getMillis() - nextExecution2.getMillis();
        final long smallestInterval = Math.min(interval1, interval2);
        final Duration difference = allowedFrequency.minusMillis(smallestInterval);
        return difference.isZero() || difference.isNegative();
    }

    private static DateTime nextExecution(CronJobSchedule schedule, DateTime lastExecution, JobSchedulerClock clock) {
        if (lastExecution == null) {
            return null;
        }
        return schedule.calculateNextTime(lastExecution, lastExecution, clock).orElse(null);
    }
}
