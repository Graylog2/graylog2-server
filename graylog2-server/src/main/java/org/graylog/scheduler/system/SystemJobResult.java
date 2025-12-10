package org.graylog.scheduler.system;

import com.google.common.primitives.Ints;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.time.Duration;

public record SystemJobResult(JobTriggerStatus status, Duration delay) {
    public static SystemJobResult success() {
        return new SystemJobResult(JobTriggerStatus.COMPLETE, Duration.ZERO);
    }

    public static SystemJobResult withRetry(Duration delay) {
        return new SystemJobResult(JobTriggerStatus.RUNNABLE, delay);
    }

    public static SystemJobResult withError() {
        return new SystemJobResult(JobTriggerStatus.ERROR, Duration.ZERO);
    }

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
