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
package org.graylog.events.contentpack.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.notifications.NotificationDto;
import org.graylog2.contentpacks.NativeEntityConverter;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;

@AutoValue
@JsonDeserialize(builder = NotificationEntity.Builder.class)
public abstract class NotificationEntity implements NativeEntityConverter<NotificationDto> {

    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CONFIG = "config";

    public static Builder builder() {
        return Builder.create();
    }

    @JsonProperty(FIELD_TITLE)
    public abstract ValueReference title();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract ValueReference description();

    @JsonProperty(FIELD_CONFIG)
    public abstract EventNotificationConfigEntity config();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_NotificationEntity.Builder();
        }

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(ValueReference title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(ValueReference description);

        @JsonProperty(FIELD_CONFIG)
        public abstract Builder config(EventNotificationConfigEntity config);

        public abstract NotificationEntity build();
    }

    @Override
    public NotificationDto toNativeEntity(Map<String, ValueReference> parameters) {
        return NotificationDto.builder()
            .description(description().asString(parameters))
            .title(title().asString(parameters))
            .config(config().toNativeEntity(parameters))
            .build();
    }
}
