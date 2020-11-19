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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class SlackMessage {

    private String color;
    private String iconEmoji;
    private String iconUrl;
    private String userName;
    private String channel;
    private boolean linkNames;
    private String message;
    private String customMessage;


    public SlackMessage(
            String color,
            String iconEmoji,
            String iconUrl,
            String userName,
            String channel,
            boolean linkNames,
            String message,
            String customMessage

    ) {
        this.color = color;
        this.iconEmoji = iconEmoji;
        this.iconUrl = iconUrl;
        this.userName = userName;
        this.channel = channel;
        this.linkNames = linkNames;
        this.message = message;
        this.customMessage = customMessage;

    }

    public SlackMessage(String message) {
        this.message = message;
    }

    public String getJsonString() {
        // See https://api.slack.com/methods/chat.postMessage for valid parameters
        final Map<String, Object> params = new HashMap<>();
        params.put("channel", channel);
        params.put("text", message);
        params.put("link_names", linkNames);

        if (!isNullOrEmpty(userName)) {
            params.put("username", userName);
        }

        if (!isNullOrEmpty(iconUrl)) {
            params.put("icon_url", iconUrl);
        }

        if (!isNullOrEmpty(iconEmoji)) {
            params.put("icon_emoji", ensureEmojiSyntax(iconEmoji));
        }

        final List<Attachment> attachments = new ArrayList<>();
        if (!isNullOrEmpty(customMessage)) {
            final Attachment attachment = new Attachment(
                    color,
                    customMessage,
                    "Custom Message",
                    "Custom Message:",
                    null
            );
            attachments.add(attachment);
        }

        if (!attachments.isEmpty()) {
            params.put("attachments", attachments);
        }

        try {
            return new ObjectMapper().writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not build payload JSON.", e);
        }
    }

    private String ensureEmojiSyntax(final String x) {
        String emoji = x.trim();

        if (!emoji.isEmpty() && !emoji.startsWith(":")) {
            emoji = ":" + emoji;
        }

        if (!emoji.isEmpty() && !emoji.endsWith(":")) {
            emoji = emoji + ":";
        }

        return emoji;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attachment {
        @JsonProperty
        public String fallback;
        @JsonProperty
        public String text;
        @JsonProperty
        public String pretext;
        @JsonProperty
        public String color;
        @JsonProperty
        public List<AttachmentField> fields;

        @JsonCreator
        public Attachment(String color, String text, String fallback, String pretext, List<AttachmentField> fields) {
            this.fallback = fallback;
            this.text = text;
            this.pretext = pretext;
            this.color = color;
            this.fields = fields;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AttachmentField {
        @JsonProperty
        public String title;
        @JsonProperty
        public String value;
        @JsonProperty("short")
        public boolean isShort;

        @JsonCreator
        public AttachmentField(String title, String value, boolean isShort) {
            this.title = title;
            this.value = value;
            this.isShort = isShort;
        }
    }

}
