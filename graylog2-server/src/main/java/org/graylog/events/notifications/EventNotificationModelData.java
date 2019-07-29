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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import org.graylog.events.event.EventDto;
import org.graylog2.plugin.MessageSummary;

import java.util.List;

/**
 * Data object that can be used in notifications to provide structured data to plugins.
 */
@AutoValue
public abstract class EventNotificationModelData {
    @JsonProperty("event_definition_id")
    public abstract String eventDefinitionId();

    @JsonProperty("event_definition_type")
    public abstract String eventDefinitionType();

    @JsonProperty("event_definition_title")
    public abstract String eventDefinitionTitle();

    @JsonProperty("event_definition_description")
    public abstract String eventDefinitionDescription();

    @JsonProperty("job_definition_id")
    public abstract String jobDefinitionId();

    @JsonProperty("job_trigger_id")
    public abstract String jobTriggerId();

    @JsonProperty("event")
    public abstract EventDto event();

    @JsonProperty("backlog")
    public abstract ImmutableList<MessageSummary> backlog();

    public static Builder builder() {
        return new AutoValue_EventNotificationModelData.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder eventDefinitionId(String id);

        public abstract Builder eventDefinitionType(String type);

        public abstract Builder eventDefinitionTitle(String title);

        public abstract Builder eventDefinitionDescription(String description);

        public abstract Builder jobDefinitionId(String jobDefinitionId);

        public abstract Builder jobTriggerId(String jobTriggerId);

        public abstract Builder event(EventDto event);

        public abstract Builder backlog(List<MessageSummary> backlog);

        public abstract EventNotificationModelData build();
    }
}