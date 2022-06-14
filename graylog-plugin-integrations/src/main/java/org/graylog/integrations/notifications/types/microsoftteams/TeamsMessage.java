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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamsMessage {
    private String color;
    private String iconUrl;
    private String messageTitle;
    private JsonNode customMessage;
    private String description;


    public TeamsMessage(
            String color,
            String iconUrl,
            String messageTitle,
            JsonNode customMessage,
            String description
    ) {
        this.color = color;
        this.iconUrl = iconUrl;
        this.messageTitle = messageTitle;
        this.customMessage = customMessage;
        this.description = description;
    }

    public TeamsMessage(String messageTitle) {
        this.messageTitle = messageTitle;
    }

    public String getJsonString() {

        final Map<String, Object> params = new HashMap<>();
        params.put("@type", "MessageCard");
        params.put("@context", "http://schema.org/extensions");
        params.put("themeColor", color);
        params.put("text", messageTitle);

        final List<Sections> Sections = new ArrayList<>();
        if (!customMessage.isNull()) {
            final Sections section = new Sections(
                    description,
                    iconUrl,
                    customMessage
            );

            Sections.add(section);
        }

        if (!Sections.isEmpty()) {
            params.put("sections", Sections);
        }

        try {
            return new ObjectMapper().writeValueAsString(params);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not build payload JSON.", e);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sections {

        @JsonProperty
        public String activitySubtitle;

        @JsonProperty
        public String activityImage;

        @JsonProperty
        public JsonNode facts;

        @JsonCreator
        public Sections(String activitySubtitle, String activityImage, JsonNode facts) {
            this.activitySubtitle = activitySubtitle;
            this.activityImage = activityImage;
            this.facts = facts;

        }
    }

}
