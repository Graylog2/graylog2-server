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
package org.graylog.events.processor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = EventProcessorStateDto.Builder.class)
public abstract class EventProcessorStateDto {
    private static final String FIELD_ID = "id";
    static final String FIELD_EVENT_DEFINITION_ID = "event_definition_id";
    static final String FIELD_MIN_PROCESSED_TIMESTAMP = "min_processed_timestamp";
    static final String FIELD_MAX_PROCESSED_TIMESTAMP = "max_processed_timestamp";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_EVENT_DEFINITION_ID)
    public abstract String eventDefinitionId();

    @JsonProperty(FIELD_MIN_PROCESSED_TIMESTAMP)
    public abstract DateTime minProcessedTimestamp();

    @JsonProperty(FIELD_MAX_PROCESSED_TIMESTAMP)
    public abstract DateTime maxProcessedTimestamp();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventProcessorStateDto.Builder();
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_EVENT_DEFINITION_ID)
        public abstract Builder eventDefinitionId(String eventDefinitionId);

        @JsonProperty(FIELD_MIN_PROCESSED_TIMESTAMP)
        public abstract Builder minProcessedTimestamp(DateTime minProcessedTimestamp);

        @JsonProperty(FIELD_MAX_PROCESSED_TIMESTAMP)
        public abstract Builder maxProcessedTimestamp(DateTime maxProcessedTimestamp);

        public abstract EventProcessorStateDto build();
    }
}