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

import static java.util.Objects.requireNonNull;

/**
 * This is thrown when a {@link Job} failed to execute correctly.
 */
public class JobExecutionException extends Exception {
    private final JobTriggerDto trigger;
    private final JobTriggerUpdate update;

    public JobExecutionException(String message, JobTriggerDto trigger, JobTriggerUpdate update) {
        this(message, trigger, update, null);
    }

    public JobExecutionException(String message, JobTriggerDto trigger, JobTriggerUpdate update, Throwable cause) {
        super(message, cause);
        this.trigger = requireNonNull(trigger, "trigger cannot be null");
        this.update = requireNonNull(update, "update cannot be null");
    }

    /**
     * Returns the trigger that triggered the job execution.
     *
     * @return the related trigger
     */
    public JobTriggerDto getTrigger() {
        return trigger;
    }

    /**
     * Returns the trigger update that should be stored in the database.
     *
     * @return the trigger update or null if not set
     */
    public JobTriggerUpdate getUpdate() {
        return update;
    }
}
