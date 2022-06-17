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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import java.util.Set;

@AutoValue
@JsonDeserialize(builder = TeamsMessage.Builder.class)
public abstract class TeamsMessage {
    // constant fields and values in TeamsMessage JSON object
    static final String FIELD_TYPE = "@type";
    static final String FIELD_CONTEXT = "@context";
    static final String VALUE_TYPE = "MessageCard";
    static final String VALUE_CONTEXT = "http://schema.org/extensions";

    // configurable fields
    static final String FIELD_THEME_COLOR = "themeColor";
    static final String FIELD_TEXT = "text";
    static final String FIELD_SECTIONS = "sections";
    static final String FIELD_ACTIVITY_SUBTITLE = "activitySubtitle";
    static final String FIELD_ACTIVITY_IMAGE = "activityImage";
    static final String FIELD_FACTS = "facts";

    @JsonProperty(FIELD_TYPE)
    public abstract String type();

    @JsonProperty(FIELD_CONTEXT)
    public abstract String context();

    @JsonProperty(FIELD_THEME_COLOR)
    public abstract String color();

    @JsonProperty(FIELD_TEXT)
    public abstract String text();

    @JsonProperty(FIELD_SECTIONS)
    public abstract Set<Sections> sections();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_TYPE)
        public abstract Builder type(String type);

        @JsonProperty(FIELD_CONTEXT)
        public abstract Builder context(String context);

        @JsonProperty(FIELD_THEME_COLOR)
        public abstract Builder color(String color);

        @JsonProperty(FIELD_TEXT)
        public abstract Builder text(String text);

        @JsonProperty(FIELD_SECTIONS)
        public abstract Builder sections(Set<Sections> sections);

        public abstract TeamsMessage build();

        @JsonCreator
        public static Builder create() {
            // Set the default @type and @context values on the message
            // They should never be set elsewhere
            return new AutoValue_TeamsMessage.Builder()
                    .context(VALUE_CONTEXT)
                    .type(VALUE_TYPE);
        }
    }

    @AutoValue
    @JsonDeserialize(builder = AutoValue_TeamsMessage_Sections.Builder.class)
    public static abstract class Sections {

        @JsonProperty(FIELD_ACTIVITY_SUBTITLE)
        public abstract String activitySubtitle();

        @JsonProperty(FIELD_ACTIVITY_IMAGE)
        public abstract String activityImage();

        @JsonProperty(FIELD_FACTS)
        public abstract JsonNode facts();

        public abstract Builder toBuilder();

        public static Builder builder() {
            return new AutoValue_TeamsMessage_Sections.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {

            @JsonProperty(FIELD_ACTIVITY_SUBTITLE)
            public abstract Builder activitySubtitle(String activitySubtitle);

            @JsonProperty(FIELD_ACTIVITY_IMAGE)
            public abstract Builder activityImage(String activityImage);

            @JsonProperty(FIELD_FACTS)
            public abstract Builder facts(JsonNode facts);

            public abstract Sections build();
        }
    }

}
