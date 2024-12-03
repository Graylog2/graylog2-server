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
import jakarta.annotation.Nullable;
import org.graylog2.database.entities.ScopedEntity;

@AutoValue
@JsonDeserialize(builder = EventProcedureStep.Builder.class)
public abstract class EventProcedureStep extends ScopedEntity {
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_ACTION = "action";

    @Nullable
    @JsonProperty(FIELD_TITLE)
    public abstract String title();

    @Nullable
    @JsonProperty(FIELD_DESCRIPTION)
    public abstract String description();

    @Nullable
    @JsonProperty(FIELD_ACTION)
    public abstract ActionDto action();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder extends ScopedEntity.AbstractBuilder<Builder> {

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(String description);

        @JsonProperty(FIELD_ACTION)
        public abstract Builder action(ActionDto action);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventProcedureStep.Builder();
        }

        public abstract EventProcedureStep build();
    }
}
