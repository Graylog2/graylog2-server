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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.rest.ValidationResult;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Configuration class for Pager Duty notifications.
 *
 * @author Edgar Molina
 */

@AutoValue
@JsonTypeName(PagerDutyNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = PagerDutyNotificationConfig.Builder.class)
public abstract class PagerDutyNotificationConfig implements EventNotificationConfig {
    public static final String TYPE_NAME = "pagerduty-notification-v2";

    static final String FIELD_ROUTING_KEY = "routing_key";
    static final String FIELD_CUSTOM_INCIDENT = "custom_incident";
    static final String FIELD_KEY_PREFIX = "key_prefix";
    static final String FIELD_CLIENT_NAME = "client_name";
    static final String FIELD_CLIENT_URL = "client_url";

    @JsonProperty(FIELD_ROUTING_KEY)
    public abstract String routingKey();

    @JsonProperty(FIELD_CUSTOM_INCIDENT)
    public abstract boolean customIncident();

    @JsonProperty(FIELD_KEY_PREFIX)
    public abstract String keyPrefix();

    @JsonProperty(FIELD_CLIENT_NAME)
    public abstract String clientName();

    @JsonProperty(FIELD_CLIENT_URL)
    public abstract String clientUrl();

    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    public static PagerDutyNotificationConfig.Builder builder() {
        return PagerDutyNotificationConfig.Builder.create();
    }

    @JsonIgnore
    @Override
    public ValidationResult validate() {
        final ValidationResult validation = new ValidationResult();

        if (routingKey().isEmpty()) {
            validation.addError(FIELD_ROUTING_KEY, "Routing Key cannot be empty.");
        }
        else if (routingKey().length() != 32) {
            validation.addError(FIELD_ROUTING_KEY, "Routing Key must be 32 characters long.");
        }
        if (customIncident() && keyPrefix().isEmpty()) {
            validation.addError(FIELD_KEY_PREFIX, "Incident Key Prefix cannot be empty when Custom Incident Key is selected.");
        }
        if (clientName().isEmpty()) {
            validation.addError(FIELD_CLIENT_NAME, "Client Name cannot be empty.");
        }
        if (clientUrl().isEmpty()) {
            validation.addError(FIELD_CLIENT_URL, "Client URL cannot be empty.");
        }
        else {
            try {
                final URI clientUri = new URI(clientUrl());
                if (!"http".equals(clientUri.getScheme()) && !"https".equals(clientUri.getScheme())) {
                    validation.addError(
                            FIELD_CLIENT_URL, "Client URL must be a valid HTTP or HTTPS URL.");
                }
            }
            catch (URISyntaxException e) {
                validation.addError(FIELD_CLIENT_URL, "Couldn't parse Client URL correctly.");
            }
        }

        return validation;
    }

    @AutoValue.Builder
    public static abstract class Builder
            implements
            EventNotificationConfig.Builder<PagerDutyNotificationConfig.Builder> {
        @JsonCreator
        public static PagerDutyNotificationConfig.Builder create() {
            return new AutoValue_PagerDutyNotificationConfig.Builder().type(TYPE_NAME);
        }

        @JsonProperty(FIELD_ROUTING_KEY)
        public abstract PagerDutyNotificationConfig.Builder routingKey(String routingKey);

        @JsonProperty(FIELD_CUSTOM_INCIDENT)
        public abstract PagerDutyNotificationConfig.Builder customIncident(boolean customIncident);

        @JsonProperty(FIELD_KEY_PREFIX)
        public abstract PagerDutyNotificationConfig.Builder keyPrefix(String keyPrefix);

        @JsonProperty(FIELD_CLIENT_NAME)
        public abstract PagerDutyNotificationConfig.Builder clientName(String clientName);

        @JsonProperty(FIELD_CLIENT_URL)
        public abstract PagerDutyNotificationConfig.Builder clientUrl(String clientUrl);

        public abstract PagerDutyNotificationConfig build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity(
            EntityDescriptorIds entityDescriptorIds) {
        return PagerDutyNotificationConfigEntity
                .builder()
                .routingKey(ValueReference.of(routingKey()))
                .customIncident(ValueReference.of(customIncident()))
                .keyPrefix(ValueReference.of(keyPrefix()))
                .clientName(ValueReference.of(clientName()))
                .clientUrl(ValueReference.of(clientUrl()))
                .build();
    }
}