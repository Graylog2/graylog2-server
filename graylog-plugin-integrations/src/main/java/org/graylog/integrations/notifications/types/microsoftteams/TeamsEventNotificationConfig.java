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
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.rest.ValidationResult;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import java.net.URI;
import java.util.regex.Pattern;

@AutoValue
@JsonTypeName(TeamsEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = TeamsEventNotificationConfig.Builder.class)
public abstract class TeamsEventNotificationConfig implements EventNotificationConfig {

    public static final String TYPE_NAME = "teams-notification-v1";

    private static final Pattern TEAMS_PATTERN = Pattern.compile("https://.*.webhook.office.com/");
    private static final String DEFAULT_HEX_COLOR = "#ff0500";
    private static final String DEFAULT_CUSTOM_MESSAGE = "Graylog Teams Notification";
    private static final long DEFAULT_BACKLOG_SIZE = 0;

    static final String INVALID_BACKLOG_ERROR_MESSAGE = "Backlog size cannot be less than zero";
    static final String INVALID_WEBHOOK_ERROR_MESSAGE = "Specified Webhook URL is not a valid URL";
    static final String INVALID_TEAMS_URL_ERROR_MESSAGE = "Specified Webhook URL is not a valid Teams URL";
    static final String WEB_HOOK_URL = "https://teams.webhook.office.com/services/xxxx/xxxxxxxxxxxxxxxxxxx";
    static final String FIELD_WEBHOOK_URL = "webhook_url";
    static final String TEAMS_CUSTOM_MESSAGE = "custom_message";
    static final String TEAMS_ICON_URL = "icon_url";
    static final String TEAMS_BACKLOG_SIZE = "backlog_size";
    static final String TEAMS_COLOR = "color";

    @JsonProperty(TEAMS_BACKLOG_SIZE)
    public abstract long backlogSize();

    @JsonProperty(TEAMS_COLOR)
    @NotBlank
    public abstract String color();

    @JsonProperty(FIELD_WEBHOOK_URL)
    @NotBlank
    public abstract String webhookUrl();

    @JsonProperty(TEAMS_CUSTOM_MESSAGE)
    public abstract String customMessage();

    @JsonProperty(TEAMS_ICON_URL)
    @Nullable
    public abstract String iconUrl();

    @Override
    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @Override
    @JsonIgnore
    public ValidationResult validate() {
        ValidationResult validation = new ValidationResult();

        URI webhookUri;
        try {
            webhookUri = new URI(webhookUrl());
            if (webhookUri.getHost().toLowerCase().contains("office")) {
                if (!TEAMS_PATTERN.matcher(webhookUrl()).find()) {
                    validation.addError(FIELD_WEBHOOK_URL, INVALID_TEAMS_URL_ERROR_MESSAGE);
                }
            }

        } catch (Exception ex) {
            validation.addError(FIELD_WEBHOOK_URL, INVALID_WEBHOOK_ERROR_MESSAGE);
        }

        if (backlogSize() < 0) {
            validation.addError(TEAMS_BACKLOG_SIZE, INVALID_BACKLOG_ERROR_MESSAGE);
        }

        return validation;
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {

            return new AutoValue_TeamsEventNotificationConfig.Builder()
                    .type(TYPE_NAME)
                    .color(DEFAULT_HEX_COLOR)
                    .webhookUrl(WEB_HOOK_URL)
                    .customMessage(DEFAULT_CUSTOM_MESSAGE)
                    .backlogSize(DEFAULT_BACKLOG_SIZE);
        }

        @JsonProperty(TEAMS_COLOR)
        public abstract Builder color(String color);

        @JsonProperty(FIELD_WEBHOOK_URL)
        public abstract Builder webhookUrl(String webhookUrl);

        @JsonProperty(TEAMS_CUSTOM_MESSAGE)
        public abstract Builder customMessage(String customMessage);

        @JsonProperty(TEAMS_ICON_URL)
        public abstract Builder iconUrl(String iconUrl);

        @JsonProperty(TEAMS_BACKLOG_SIZE)
        public abstract Builder backlogSize(long backlogSize);

        public abstract TeamsEventNotificationConfig build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return TeamsEventNotificationConfigEntity.builder()
                .color(ValueReference.of(color()))
                .webhookUrl(ValueReference.of(webhookUrl()))
                .customMessage(ValueReference.of(customMessage()))
                .iconUrl(ValueReference.of(iconUrl()))
                .build();
    }
}
