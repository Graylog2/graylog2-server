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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = EventNotificationHandlerConfigEntity.Builder.class)
public abstract class EventNotificationHandlerConfigEntity {

    private static final String FIELD_NOTIFICATION_ID = "notification_id";
    private static final String FIELD_NOTIFICATION_PARAMETERS = "notification_parameters";

    @JsonProperty(FIELD_NOTIFICATION_ID)
    public abstract ValueReference notificationId();

    @JsonProperty(FIELD_NOTIFICATION_PARAMETERS)
    public abstract Optional<ReferenceMap> notificationParameters();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        public static Builder create() {
            return new AutoValue_EventNotificationHandlerConfigEntity.Builder();
        }

        @JsonProperty(FIELD_NOTIFICATION_ID)
        public abstract Builder notificationId(ValueReference notificationId);

        @JsonProperty(FIELD_NOTIFICATION_PARAMETERS)
        public abstract Builder notificationParameters(@Nullable ReferenceMap notificationParameters);

        public abstract EventNotificationHandlerConfigEntity build();
    }
}
