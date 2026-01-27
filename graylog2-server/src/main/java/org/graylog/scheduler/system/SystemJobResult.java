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
package org.graylog.scheduler.system;

import com.google.common.primitives.Ints;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.time.Duration;

/**
 * Result of a system job execution.
 */
public record SystemJobResult(JobTriggerStatus status, Duration delay, int maxRetries) {
    /**
     * Creates a successful job result indicating that the job has completed.
     *
     * @return a SystemJobResult representing a successful completion
     */
    public static SystemJobResult success() {
        return new SystemJobResult(JobTriggerStatus.COMPLETE, Duration.ZERO, 0);
    }

    /**
     * Creates a job result indicating that the job should be retried after a specified delay.
     * <p>
     * <b>CAVEAT:</b> The current scheduler implementation only supports unlimited retries for system jobs until we
     * implement retry tracking. Therefore, the {@code maxRetries} parameter must be set to {@code Integer.MAX_VALUE}.
     *
     * @param delay      the delay before retrying the job
     * @param maxRetries the maximum number of retries allowed
     * @return a SystemJobResult representing a retry instruction
     */
    public static SystemJobResult withRetry(Duration delay, int maxRetries) {
        // Our scheduler doesn't have retry tracking for system jobs yet, so using retries other than "unlimited" is not
        // safe at the moment.
        // TODO: Remove this check once we have retry tracking for the scheduler in place
        if (maxRetries != Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Scheduler currently only supports unlimited retries for system jobs.");
        }
        return new SystemJobResult(JobTriggerStatus.RUNNABLE, delay, maxRetries);
    }

    /**
     * Creates a job result indicating that the job has failed with an error.
     *
     * @return a SystemJobResult representing an error state
     */
    public static SystemJobResult withError() {
        return new SystemJobResult(JobTriggerStatus.ERROR, Duration.ZERO, 0);
    }

    // We made #toJobTriggerUpdate a static method inside Converter class to avoid exposing scheduler details
    // (JobTriggerUpdate) into the public API of SystemJobResult.
    static class Converter {
        private Converter() {
        }

        public static JobTriggerUpdate toJobTriggerUpdate(SystemJobResult result, JobTriggerDto trigger) {
            return switch (result.status()) {
                case ERROR -> JobTriggerUpdate.withError(trigger);
                case RUNNABLE -> JobTriggerUpdate.withNextTime(getNextTime(result.delay()));
                case COMPLETE, CANCELLED -> JobTriggerUpdate.withoutNextTime();
                default -> throw new IllegalStateException("Unhandled result status: " + result.status());
            };
        }

        private static DateTime getNextTime(Duration delay) {
            return DateTime.now(DateTimeZone.UTC).plusMillis(Ints.saturatedCast(delay.toMillis()));
        }
    }
}
