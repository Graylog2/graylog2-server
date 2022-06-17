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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonDeserialize(builder = SlackMessage.Builder.class)
public abstract class SlackMessage {

    private static final String FIELD_ICON_EMOJI = "icon_emoji";
    private static final String FIELD_ICON_URL = "icon_url";
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_CHANNEL = "channel";
    private static final String FIELD_TEXT = "text";
    private static final String FIELD_LINK_NAMES = "link_names";
    private static final String FIELD_ATTACHMENTS = "attachments";
    private static final String FIELD_COLOR = "color";
    private static final String FIELD_ATTACHMENT_TEXT = "text";
    private static final String FIELD_FALLBACK = "fallback";
    private static final String FIELD_PRETEXT = "pretext";
    static final String VALUE_FALLBACK = "Custom Message";
    static final String VALUE_PRETEXT = "Custom Message:";

    @JsonProperty(FIELD_ICON_EMOJI)
    public abstract String iconEmoji();

    @JsonProperty(FIELD_ICON_URL)
    public abstract String iconUrl();

    @JsonProperty(FIELD_USERNAME)
    public abstract String username();

    @JsonProperty(FIELD_CHANNEL)
    public abstract String channel();

    @JsonProperty(FIELD_LINK_NAMES)
    public abstract boolean linkNames();

    @JsonProperty(FIELD_TEXT)
    public abstract String text();

    @JsonProperty(FIELD_ATTACHMENTS)
    public abstract Set<Attachment> attachments();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_ICON_EMOJI)
        public abstract Builder iconEmoji(String iconEmoji);

        @JsonProperty(FIELD_ICON_URL)
        public abstract Builder iconUrl(String iconUrl);

        @JsonProperty(FIELD_USERNAME)
        public abstract Builder username(String username);

        @JsonProperty(FIELD_CHANNEL)
        public abstract Builder channel(String channel);

        @JsonProperty(FIELD_LINK_NAMES)
        public abstract Builder linkNames(boolean linkNames);

        @JsonProperty(FIELD_TEXT)
        public abstract Builder text(String text);

        @JsonProperty(FIELD_ATTACHMENTS)
        public abstract Builder attachments(Set<Attachment> attachments);

        public abstract SlackMessage build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_SlackMessage.Builder();
        }
    }

    @AutoValue
    @JsonDeserialize(builder = AutoValue_SlackMessage_Attachment.Builder.class)
    public static abstract class Attachment {
        @JsonProperty(FIELD_FALLBACK)
        public abstract String fallback();

        @JsonProperty(FIELD_ATTACHMENT_TEXT)
        public abstract String text();

        @JsonProperty(FIELD_PRETEXT)
        public abstract String pretext();

        @JsonProperty(FIELD_COLOR)
        public abstract String color();

        public abstract Builder toBuilder();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            @JsonProperty(FIELD_FALLBACK)
            public abstract Builder fallback(String fallback);

            @JsonProperty(FIELD_ATTACHMENT_TEXT)
            public abstract Builder text(String fallback);

            @JsonProperty(FIELD_PRETEXT)
            public abstract Builder pretext(String pretext);

            @JsonProperty(FIELD_COLOR)
            public abstract Builder color(String color);

            public abstract Attachment build();

            @JsonCreator
            public static Builder create() {
                // Set the pretext and fallback values
                return new AutoValue_SlackMessage_Attachment.Builder()
                        .pretext(VALUE_PRETEXT)
                        .fallback(VALUE_FALLBACK);
            }
        }
    }
}
