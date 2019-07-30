/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.scheduler;

import com.google.inject.assistedinject.Assisted;
import org.graylog.scheduler.clock.JobSchedulerClock;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Convenience factory to create {@link JobTriggerUpdate} objects.
 */
public class JobTriggerUpdates {
    public interface Factory {
        JobTriggerUpdates create(JobTriggerDto trigger);
    }

    private final JobSchedulerClock clock;
    private final JobScheduleStrategies scheduleStrategies;
    private final JobTriggerDto trigger;

    @Inject
    public JobTriggerUpdates(JobSchedulerClock clock,
                             JobScheduleStrategies scheduleStrategies,
                             @Assisted JobTriggerDto trigger) {
        this.clock = clock;
        this.scheduleStrategies = scheduleStrategies;
        this.trigger = trigger;
    }

    /**
     * Returns a job trigger update that instructs the scheduler to execute the trigger again based on its schedule
     * configuration.
     *
     * @return the job trigger update
     */
    public JobTriggerUpdate scheduleNextExecution() {
        return JobTriggerUpdate.withNextTime(scheduleStrategies.nextTime(trigger).orElse(null));
    }

    /**
     * Returns a job trigger update that instructs the scheduler to execute the trigger again based on its schedule
     * configuration. It also includes the given {@link JobTriggerData} object in the trigger update.
     *
     * @return the job trigger update
     */
    public JobTriggerUpdate scheduleNextExecution(JobTriggerData data) {
        return JobTriggerUpdate.withNextTimeAndData(scheduleStrategies.nextTime(trigger).orElse(null), data);
    }

    /**
     * Returns a job trigger update that instructs the scheduler to execute the trigger again in the future after
     * the given duration. (basically "time now" + duration)
     *
     * @param duration the duration to wait until executing the trigger again
     * @param unit     the duration unit
     * @return the job trigger update
     */
    public JobTriggerUpdate retryIn(long duration, TimeUnit unit) {
        return JobTriggerUpdate.withNextTime(clock.nowUTC().plus(unit.toMillis(duration)));
    }
}
