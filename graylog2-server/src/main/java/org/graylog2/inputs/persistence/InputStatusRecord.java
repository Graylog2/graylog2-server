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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_InputStatusRecord.Builder.class)
public abstract class InputStatusRecord {
    private static final String FIELD_ID = "id";
    public static final String FIELD_INPUT_STATE_DATA = "input_state_data";

    @Id
    @ObjectId
    @JsonProperty(FIELD_ID)
    public abstract String inputId();

    @JsonProperty(FIELD_INPUT_STATE_DATA)
    public abstract InputStateData inputStateData();

    public static Builder builder() {
        return new AutoValue_InputStatusRecord.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder inputId(String inputId);

        @JsonProperty(FIELD_INPUT_STATE_DATA)
        public abstract Builder inputStateData(InputStateData inputStateData);

        public abstract InputStatusRecord build();
    }
}
