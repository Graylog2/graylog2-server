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
package org.graylog.events.processor;

import com.google.auto.value.AutoValue;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobSchedule;

@AutoValue
public abstract class EventProcessorSchedulerConfig {
    public abstract JobDefinitionConfig jobDefinitionConfig();

    public abstract JobSchedule schedule();

    public static Builder builder() {
        return new AutoValue_EventProcessorSchedulerConfig.Builder();
    }

    public static EventProcessorSchedulerConfig create(JobDefinitionConfig jobDefinitionConfig, JobSchedule schedule) {
        return builder().jobDefinitionConfig(jobDefinitionConfig).schedule(schedule).build();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder jobDefinitionConfig(JobDefinitionConfig jobDefinitionConfig);

        public abstract Builder schedule(JobSchedule jobSchedule);

        public abstract EventProcessorSchedulerConfig build();
    }
}
