/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.integrations.notifications.types.microsoftteams;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotBlank;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.rest.ValidationResult;
import org.joda.time.DateTimeZone;

import java.net.URI;

@AutoValue
@JsonTypeName(TeamsEventNotificationConfigV2.TYPE_NAME)
@JsonDeserialize(builder = TeamsEventNotificationConfigV2.Builder.class)
public abstract class TeamsEventNotificationConfigV2 implements EventNotificationConfig {
    public static final String TYPE_NAME = "teams-notification-v2";

    public static final long DEFAULT_BACKLOG_SIZE = 0;
    private static final String WEBHOOK_URL = "https://server.region.logic.azure.com:443/workflows/xxxxxxx";
    private static final DateTimeZone DEFAULT_TIME_ZONE = DateTimeZone.UTC;
    public static final String DEFAULT_ADAPTIVE_CARD = "{\n" +
            "  \"contentType\": \"application/vnd.microsoft.card.adaptive\",\n" +
            "  \"content\": {\n" +
            "    \"type\": \"AdaptiveCard\",\n" +
            "    \"version\": \"1.6\",\n" +
            "    \"body\": [\n" +
            "      {\n" +
            "        \"type\": \"TextBlock\",\n" +
            "        \"size\": \"Large\",\n" +
            "        \"weight\": \"Bolder\",\n" +
            "        \"text\": \"${event_definition_title} triggered\",\n" +
            "        \"style\": \"heading\",\n" +
            "        \"fontType\": \"Default\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"type\": \"TextBlock\",\n" +
            "        \"text\": \"${event_definition_description}\",\n" +
            "        \"wrap\": true\n" +
            "      }\n" +
            "    ],\n" +
            "    \"$schema\": \"http://adaptivecards.io/schemas/adaptive-card.json\",\n" +
            "    \"rtl\": false\n" +
            "  }\n" +
            "}";

    static final String INVALID_WEBHOOK_ERROR_MESSAGE = "Specified Webhook URL is not a valid URL";
    static final String INVALID_BACKLOG_ERROR_MESSAGE = "Backlog size cannot be less than zero";
    static final String FIELD_WEBHOOK_URL = "webhook_url";
    static final String FIELD_ADAPTIVE_CARD = "adaptive_card";
    static final String FIELD_BACKLOG_SIZE = "backlog_size";
    static final String FIELD_TIME_ZONE = "time_zone";

    @JsonProperty(FIELD_BACKLOG_SIZE)
    public abstract long backlogSize();

    @JsonProperty(FIELD_WEBHOOK_URL)
    @NotBlank
    public abstract String webhookUrl();

    @JsonProperty(FIELD_ADAPTIVE_CARD)
    public abstract String adaptiveCard();

    @JsonProperty(FIELD_TIME_ZONE)
    public abstract DateTimeZone timeZone();

    @Override
    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return TeamsEventNotificationConfigV2Entity.builder()
                .webhookUrl(ValueReference.of(webhookUrl()))
                .adaptiveCard(ValueReference.of(adaptiveCard()))
                .timeZone(ValueReference.of(timeZone().getID()))
                .build();
    }

    public static Builder builder() { return Builder.create(); }

    @Override
    @JsonIgnore
    public ValidationResult validate() {
        ValidationResult validation = new ValidationResult();

        try {
            new URI(webhookUrl());
        } catch (Exception ex) {
            validation.addError(FIELD_WEBHOOK_URL, INVALID_WEBHOOK_ERROR_MESSAGE);
        }

        if (backlogSize() < 0) {
            validation.addError(FIELD_BACKLOG_SIZE, INVALID_BACKLOG_ERROR_MESSAGE);
        }

        return validation;
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_TeamsEventNotificationConfigV2.Builder()
                    .type(TYPE_NAME)
                    .webhookUrl(WEBHOOK_URL)
                    .adaptiveCard(DEFAULT_ADAPTIVE_CARD)
                    .backlogSize(DEFAULT_BACKLOG_SIZE)
                    .timeZone(DEFAULT_TIME_ZONE);
        }

        @JsonProperty(FIELD_BACKLOG_SIZE)
        public abstract Builder backlogSize(long backlogSize);

        @JsonProperty(FIELD_WEBHOOK_URL)
        public abstract Builder webhookUrl(String webhookUrl);

        @JsonProperty(FIELD_ADAPTIVE_CARD)
        public abstract Builder adaptiveCard(String adaptiveCard);

        @JsonProperty(FIELD_TIME_ZONE)
        public abstract Builder timeZone(DateTimeZone timeZone);

        public abstract TeamsEventNotificationConfigV2 build();
    }
}
