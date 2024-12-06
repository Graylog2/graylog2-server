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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = ActionDto.Builder.class)
public abstract class ActionDto {
    public static final String TITLE_FIELD = "title";
    public static final String CONFIG_FIELD = "config";

    @JsonProperty(TITLE_FIELD)
    public abstract String title();

    @JsonProperty(CONFIG_FIELD)
    public abstract ActionConfig config();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty(TITLE_FIELD)
        public abstract Builder title(String title);

        @JsonProperty(CONFIG_FIELD)
        public abstract Builder config(ActionConfig config);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_ActionDto.Builder();
        }

        public abstract ActionDto build();
    }
}
