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
package org.graylog.integrations.pagerduty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;

/**
 * Configuration entity for PagerDuty notification events.
 *
 * @author Edgar Molina
 *
 */
@AutoValue
@JsonTypeName(PagerDutyNotificationConfigEntity.TYPE_NAME)
@JsonDeserialize(builder = PagerDutyNotificationConfigEntity.Builder.class)
public abstract class PagerDutyNotificationConfigEntity implements EventNotificationConfigEntity {
    public static final String TYPE_NAME = "pagerduty-notification-v2";

    @JsonProperty(PagerDutyNotificationConfig.FIELD_ROUTING_KEY)
    public abstract ValueReference routingKey();

    @JsonProperty(PagerDutyNotificationConfig.FIELD_CUSTOM_INCIDENT)
    public abstract ValueReference customIncident();

    @JsonProperty(PagerDutyNotificationConfig.FIELD_KEY_PREFIX)
    public abstract ValueReference keyPrefix();

    @JsonProperty(PagerDutyNotificationConfig.FIELD_CLIENT_NAME)
    public abstract ValueReference clientName();

    @JsonProperty(PagerDutyNotificationConfig.FIELD_CLIENT_URL)
    public abstract ValueReference clientUrl();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfigEntity.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_PagerDutyNotificationConfigEntity.Builder().type(TYPE_NAME);
        }

        @JsonProperty(PagerDutyNotificationConfig.FIELD_ROUTING_KEY)
        public abstract Builder routingKey(ValueReference routingKey);

        @JsonProperty(PagerDutyNotificationConfig.FIELD_CUSTOM_INCIDENT)
        public abstract Builder customIncident(ValueReference customIncident);

        @JsonProperty(PagerDutyNotificationConfig.FIELD_KEY_PREFIX)
        public abstract Builder keyPrefix(ValueReference keyPrefix);

        @JsonProperty(PagerDutyNotificationConfig.FIELD_CLIENT_NAME)
        public abstract Builder clientName(ValueReference clientName);

        @JsonProperty(PagerDutyNotificationConfig.FIELD_CLIENT_URL)
        public abstract Builder clientUrl(ValueReference clientUrl);

        public abstract PagerDutyNotificationConfigEntity build();
    }

    @Override
    public EventNotificationConfig toNativeEntity(
            Map<String, ValueReference> parameters,
            Map<EntityDescriptor, Object> nativeEntities) {
        return PagerDutyNotificationConfig.builder()
                .routingKey(routingKey().asString(parameters))
                .customIncident(customIncident().asBoolean(parameters))
                .keyPrefix(keyPrefix().asString(parameters))
                .clientName(clientName().asString(parameters))
                .clientUrl(clientUrl().asString(parameters))
                .build();
    }
}
