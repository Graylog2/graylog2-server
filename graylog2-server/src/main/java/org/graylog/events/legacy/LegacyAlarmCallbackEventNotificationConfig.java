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
package org.graylog.events.legacy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.LegacyAlarmCallbackEventNotificationConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.rest.ValidationResult;

import java.util.Map;

@AutoValue
@JsonTypeName(LegacyAlarmCallbackEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = LegacyAlarmCallbackEventNotificationConfig.Builder.class)
public abstract class LegacyAlarmCallbackEventNotificationConfig implements EventNotificationConfig {
    public static final String TYPE_NAME = "legacy-alarm-callback-notification-v1";

    private static final String FIELD_CALLBACK_TYPE = "callback_type";
    private static final String FIELD_CONFIGURATION = "configuration";

    @JsonProperty(FIELD_CALLBACK_TYPE)
    public abstract String callbackType();

    @JsonProperty(FIELD_CONFIGURATION)
    public abstract Map<String, Object> configuration();

    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @JsonIgnore
    public ValidationResult validate() {
        final ValidationResult validation = new ValidationResult();

        if (callbackType().isEmpty()) {
            validation.addError(FIELD_CALLBACK_TYPE, "Legacy Notification callback type cannot be empty.");
        }

        return validation;
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_LegacyAlarmCallbackEventNotificationConfig.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_CALLBACK_TYPE)
        public abstract Builder callbackType(String callbackType);

        @JsonProperty(FIELD_CONFIGURATION)
        public abstract Builder configuration(Map<String, Object> configuration);

        public abstract LegacyAlarmCallbackEventNotificationConfig build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity() {
        return LegacyAlarmCallbackEventNotificationConfigEntity.builder()
            .callbackType(ValueReference.of(callbackType()))
            .configuration(configuration())
            .build();
    }
}
