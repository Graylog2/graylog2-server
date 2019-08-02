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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.plugin.rest.ValidationResult;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = EventNotificationConfig.TYPE_FIELD,
        visible = true,
        defaultImpl = EventNotificationConfig.FallbackNotificationConfig.class)
public interface EventNotificationConfig extends ContentPackable<EventNotificationConfigEntity> {
    String FIELD_NOTIFICATION_ID = "notification_id";
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    @JsonIgnore
    JobTriggerData toJobTriggerData(EventDto dto);

    @JsonIgnore
    ValidationResult validate();

    class FallbackNotificationConfig implements EventNotificationConfig {
        @Override
        public String type() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ValidationResult validate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JobTriggerData toJobTriggerData(EventDto dto) {
            return null;
        }

        @Override
        public EventNotificationConfigEntity toContentPackEntity() {
            return null;
        }
    }
}
