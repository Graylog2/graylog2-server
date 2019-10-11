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
package org.graylog.events.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
public abstract class EventNotificationContext {
    public abstract String notificationId();

    public abstract EventNotificationConfig notificationConfig();

    public abstract EventDto event();

    public abstract Optional<EventDefinitionDto> eventDefinition();

    public abstract Optional<JobTriggerDto> jobTrigger();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventNotificationContext.Builder();
        }

        public abstract Builder notificationId(String notificationId);

        public abstract Builder notificationConfig(EventNotificationConfig notificationConfig);

        public abstract Builder event(EventDto event);

        public abstract Builder eventDefinition(@Nullable EventDefinitionDto eventDefinition);

        public abstract Builder jobTrigger(JobTriggerDto jobTrigger);

        public abstract EventNotificationContext build();
    }
}
