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
package org.graylog.events.procedures;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.apache.http.client.utils.URIBuilder;
import org.graylog.events.event.EventDto;

import java.net.URISyntaxException;

/**
 * Redirects the frontend to a link.
 */
public class Link extends Action {
    public static final String NAME = "link";
    public static final String FIELD_LINK = "link";

    @Inject
    public Link(@Assisted ActionDto dto) {
        super(dto);
    }

    public interface Factory extends Action.Factory<Link> {
        @Override
        Link create(ActionDto dto);
    }

    @AutoValue
    @JsonAutoDetect
    @JsonTypeName(NAME)
    @JsonDeserialize(builder = AutoValue_Link_Config.Builder.class)
    public static abstract class Config implements ActionConfig {
        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty(FIELD_LINK)
        public abstract String link();

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @JsonIgnore
        @Override
        public URIBuilder getLink(EventDto event) {
            try {
                return new URIBuilder(link());
            } catch (URISyntaxException e) {
                return null;
            }
        }

        @JsonIgnore
        @Override
        public String validate() {
            if (link() == null || link().isEmpty()) {
                return "Link cannot be empty";
            }
            return null;
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty(FIELD_LINK)
            public abstract Builder link(String link);

            @JsonCreator
            public static Builder create() {
                return new AutoValue_Link_Config.Builder().type(NAME);
            }

            public abstract Config build();
        }
    }
}
