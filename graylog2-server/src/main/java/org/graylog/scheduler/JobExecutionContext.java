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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.concurrent.atomic.AtomicBoolean;

@AutoValue
@JsonDeserialize(builder = JobExecutionContext.Builder.class)
public abstract class JobExecutionContext {
    public abstract JobTriggerDto trigger();

    public abstract JobDefinitionDto definition();

    public abstract JobTriggerUpdates jobTriggerUpdates();

    public abstract AtomicBoolean isRunning();

    public static JobExecutionContext create(JobTriggerDto trigger, JobDefinitionDto definition, JobTriggerUpdates jobTriggerUpdates, AtomicBoolean isRunning) {
        return builder()
                .trigger(trigger)
                .definition(definition)
                .jobTriggerUpdates(jobTriggerUpdates)
                .isRunning(isRunning)
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_JobExecutionContext.Builder();
        }

        public abstract Builder trigger(JobTriggerDto trigger);

        public abstract Builder definition(JobDefinitionDto definition);

        public abstract Builder jobTriggerUpdates(JobTriggerUpdates jobTriggerUpdates);

        public abstract Builder isRunning(AtomicBoolean isRunning);

        public abstract JobExecutionContext build();
    }
}
