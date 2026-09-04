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
package org.graylog2.inputs.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = InputStateDto.Builder.class)
public abstract class InputStateDto implements MongoEntity {
    static final String FIELD_ID = "id";
    static final String FIELD_INPUT_ID = "input_id";
    static final String FIELD_NODE_ID = "node_id";
    static final String FIELD_STATE = "state";
    static final String FIELD_STARTED_AT = "started_at";
    static final String FIELD_LAST_FAILED_AT = "last_failed_at";
    static final String FIELD_DETAILED_MESSAGE = "detailed_message";
    static final String FIELD_UPDATED_AT = "updated_at";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_INPUT_ID)
    public abstract String inputId();

    @JsonProperty(FIELD_NODE_ID)
    public abstract String nodeId();

    @JsonProperty(FIELD_STATE)
    public abstract String state();

    @Nullable
    @JsonProperty(FIELD_STARTED_AT)
    public abstract DateTime startedAt();

    @Nullable
    @JsonProperty(FIELD_LAST_FAILED_AT)
    public abstract DateTime lastFailedAt();

    @Nullable
    @JsonProperty(FIELD_DETAILED_MESSAGE)
    public abstract String detailedMessage();

    @JsonProperty(FIELD_UPDATED_AT)
    public abstract DateTime updatedAt();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_InputStateDto.Builder();
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_INPUT_ID)
        public abstract Builder inputId(String inputId);

        @JsonProperty(FIELD_NODE_ID)
        public abstract Builder nodeId(String nodeId);

        @JsonProperty(FIELD_STATE)
        public abstract Builder state(String state);

        @JsonProperty(FIELD_STARTED_AT)
        public abstract Builder startedAt(DateTime startedAt);

        @JsonProperty(FIELD_LAST_FAILED_AT)
        public abstract Builder lastFailedAt(DateTime lastFailedAt);

        @JsonProperty(FIELD_DETAILED_MESSAGE)
        public abstract Builder detailedMessage(String detailedMessage);

        @JsonProperty(FIELD_UPDATED_AT)
        public abstract Builder updatedAt(DateTime updatedAt);

        public abstract InputStateDto build();
    }
}
