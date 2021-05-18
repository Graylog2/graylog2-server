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
package org.graylog.integrations.notifications.types;

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
@JsonTypeName(SlackEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = SlackEventNotificationConfig.Builder.class)
public abstract class SlackEventNotificationConfig implements EventNotificationConfig {

    public static final String TYPE_NAME = "slack-notification-v1";

    private static final Pattern SLACK_PATTERN = Pattern.compile("https:\\/\\/hooks.slack.com\\/services\\/");
    private static final Pattern DISCORD_PATTERN = Pattern.compile("https:\\/\\/.*\\.?discord(app)?.com\\/api\\/webhooks.*\\/slack");
    private static final String DEFAULT_HEX_COLOR = "#ff0500";
    private static final String DEFAULT_CUSTOM_MESSAGE = "Graylog Slack Notification";
    private static final long DEFAULT_BACKLOG_SIZE = 0;

    static final String INVALID_BACKLOG_ERROR_MESSAGE = "Backlog size cannot be less than zero";
    static final String INVALID_CHANNEL_ERROR_MESSAGE = "Channel cannot be empty";
    static final String INVALID_WEBHOOK_ERROR_MESSAGE = "Specified Webhook URL is not a valid URL";
    static final String INVALID_SLACK_URL_ERROR_MESSAGE = "Specified Webhook URL is not a valid Slack URL";
    static final String INVALID_DISCORD_URL_ERROR_MESSAGE = "Specified Webhook URL is not a valid Discord URL";
    static final String WEB_HOOK_URL = "https://hooks.slack.com/services/xxx/xxxx/xxxxxxxxxxxxxxxxxxx";
    static final String CHANNEL = "#general";

    static final String FIELD_COLOR = "color";
    static final String FIELD_WEBHOOK_URL = "webhook_url";
    static final String FIELD_CHANNEL = "channel";
    static final String FIELD_CUSTOM_MESSAGE = "custom_message";
    static final String FIELD_USER_NAME = "user_name";
    static final String FIELD_NOTIFY_CHANNEL = "notify_channel";
    static final String FIELD_LINK_NAMES = "link_names";
    static final String FIELD_ICON_URL = "icon_url";
    static final String FIELD_ICON_EMOJI = "icon_emoji";
    static final String FIELD_BACKLOG_SIZE = "backlog_size";

    @JsonProperty(FIELD_BACKLOG_SIZE)
    public abstract long backlogSize();

    @JsonProperty(FIELD_COLOR)
    @NotBlank
    public abstract String color();

    @JsonProperty(FIELD_WEBHOOK_URL)
    @NotBlank
    public abstract String webhookUrl();

    @JsonProperty(FIELD_CHANNEL)
    @NotBlank
    public abstract String channel();

    @JsonProperty(FIELD_CUSTOM_MESSAGE)
    public abstract String customMessage();

    @JsonProperty(FIELD_USER_NAME)
    @Nullable
    public abstract String userName();

    @JsonProperty(FIELD_NOTIFY_CHANNEL)
    public abstract boolean notifyChannel();

    @JsonProperty(FIELD_LINK_NAMES)
    public abstract boolean linkNames();

    @JsonProperty(FIELD_ICON_URL)
    @Nullable
    public abstract String iconUrl();

    @JsonProperty(FIELD_ICON_EMOJI)
    @Nullable
    public abstract String iconEmoji();

    @Override
    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    public static SlackEventNotificationConfig.Builder builder() {
        return SlackEventNotificationConfig.Builder.create();
    }

    @Override
    @JsonIgnore
    public ValidationResult validate() {
        ValidationResult validation = new ValidationResult();

        URI webhookUri;
        try {
            webhookUri = new URI(webhookUrl());
            if (webhookUri.getHost().toLowerCase().contains("slack")) {
                if (!SLACK_PATTERN.matcher(webhookUrl()).find()) {
                    validation.addError(FIELD_WEBHOOK_URL, INVALID_SLACK_URL_ERROR_MESSAGE);
                }
            } else if (webhookUri.getHost().toLowerCase().contains("discord")) {
                if (!DISCORD_PATTERN.matcher(webhookUrl()).find()) {
                    validation.addError(FIELD_WEBHOOK_URL, INVALID_DISCORD_URL_ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            validation.addError(FIELD_WEBHOOK_URL, INVALID_WEBHOOK_ERROR_MESSAGE);
        }

        if (backlogSize() < 0) {
            validation.addError(FIELD_BACKLOG_SIZE, INVALID_BACKLOG_ERROR_MESSAGE);
        }

        if (channel().isEmpty()) {
            validation.addError(FIELD_CHANNEL, INVALID_CHANNEL_ERROR_MESSAGE);
        }

        return validation;
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfig.Builder<SlackEventNotificationConfig.Builder> {
        @JsonCreator
        public static SlackEventNotificationConfig.Builder create() {

            return new AutoValue_SlackEventNotificationConfig.Builder()
                    .type(TYPE_NAME)
                    .color(DEFAULT_HEX_COLOR)
                    .webhookUrl(WEB_HOOK_URL)
                    .channel(CHANNEL)
                    .customMessage(DEFAULT_CUSTOM_MESSAGE)
                    .notifyChannel(false)
                    .backlogSize(DEFAULT_BACKLOG_SIZE)
                    .linkNames(false);
        }

        @JsonProperty(FIELD_COLOR)
        public abstract SlackEventNotificationConfig.Builder color(String color);

        @JsonProperty(FIELD_WEBHOOK_URL)
        public abstract SlackEventNotificationConfig.Builder webhookUrl(String webhookUrl);

        @JsonProperty(FIELD_CHANNEL)
        public abstract SlackEventNotificationConfig.Builder channel(String channel);

        @JsonProperty(FIELD_CUSTOM_MESSAGE)
        public abstract SlackEventNotificationConfig.Builder customMessage(String customMessage);

        @JsonProperty(FIELD_USER_NAME)
        public abstract SlackEventNotificationConfig.Builder userName(String userName);

        @JsonProperty(FIELD_NOTIFY_CHANNEL)
        public abstract SlackEventNotificationConfig.Builder notifyChannel(boolean notifyChannel);

        @JsonProperty(FIELD_LINK_NAMES)
        public abstract SlackEventNotificationConfig.Builder linkNames(boolean linkNames);

        @JsonProperty(FIELD_ICON_URL)
        public abstract SlackEventNotificationConfig.Builder iconUrl(String iconUrl);

        @JsonProperty(FIELD_ICON_EMOJI)
        public abstract SlackEventNotificationConfig.Builder iconEmoji(String iconEmoji);

        @JsonProperty(FIELD_BACKLOG_SIZE)
        public abstract SlackEventNotificationConfig.Builder backlogSize(long backlogSize);

        public abstract SlackEventNotificationConfig build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return SlackEventNotificationConfigEntity.builder()
                .color(ValueReference.of(color()))
                .webhookUrl(ValueReference.of(webhookUrl()))
                .channel(ValueReference.of(channel()))
                .customMessage(ValueReference.of(customMessage()))
                .userName(ValueReference.of(userName()))
                .notifyChannel(ValueReference.of(notifyChannel()))
                .linkNames(ValueReference.of(linkNames()))
                .iconUrl(ValueReference.of(iconUrl()))
                .iconEmoji(ValueReference.of(iconEmoji()))
                .build();
    }
}
