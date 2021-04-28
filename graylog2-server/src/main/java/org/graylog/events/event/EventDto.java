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
package org.graylog.events.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = EventDto.Builder.class)
public abstract class EventDto {
    public static final String FIELD_ID = "id";
    public static final String FIELD_EVENT_DEFINITION_TYPE = "event_definition_type";
    public static final String FIELD_EVENT_DEFINITION_ID = "event_definition_id";
    private static final String FIELD_ORIGIN_CONTEXT = "origin_context";
    public static final String FIELD_EVENT_TIMESTAMP = "timestamp";
    public static final String FIELD_PROCESSING_TIMESTAMP = "timestamp_processing";
    public static final String FIELD_TIMERANGE_START = "timerange_start";
    public static final String FIELD_TIMERANGE_END = "timerange_end";
    public static final String FIELD_STREAMS = "streams";
    public static final String FIELD_SOURCE_STREAMS = "source_streams";
    public static final String FIELD_ALERT = "alert";
    public static final String FIELD_MESSAGE = "message";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_KEY_TUPLE = "key_tuple";
    private static final String FIELD_KEY = "key";
    private static final String FIELD_PRIORITY = "priority";
    private static final String FIELD_FIELDS = "fields";
    private static final String FIELD_GROUP_BY_FIELDS = "group_by_fields";

    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_EVENT_DEFINITION_TYPE)
    public abstract String eventDefinitionType();

    @JsonProperty(FIELD_EVENT_DEFINITION_ID)
    public abstract String eventDefinitionId();

    @JsonProperty(FIELD_ORIGIN_CONTEXT)
    public abstract Optional<String> originContext();

    @JsonProperty(FIELD_EVENT_TIMESTAMP)
    public abstract DateTime eventTimestamp();

    @JsonProperty(FIELD_PROCESSING_TIMESTAMP)
    public abstract DateTime processingTimestamp();

    @JsonProperty(FIELD_TIMERANGE_START)
    public abstract Optional<DateTime> timerangeStart();

    @JsonProperty(FIELD_TIMERANGE_END)
    public abstract Optional<DateTime> timerangeEnd();

    @JsonProperty(FIELD_STREAMS)
    public abstract Set<String> streams();

    @JsonProperty(FIELD_SOURCE_STREAMS)
    public abstract Set<String> sourceStreams();

    @JsonProperty(FIELD_MESSAGE)
    public abstract String message();

    @JsonProperty(FIELD_SOURCE)
    public abstract String source();

    @JsonProperty(FIELD_KEY_TUPLE)
    public abstract List<String> keyTuple();

    @JsonProperty(FIELD_KEY)
    @Nullable
    public abstract String key();

    @JsonProperty(FIELD_PRIORITY)
    public abstract long priority();

    @JsonProperty(FIELD_ALERT)
    public abstract boolean alert();

    @JsonProperty(FIELD_FIELDS)
    public abstract Map<String, String> fields();

    @JsonProperty(FIELD_GROUP_BY_FIELDS)
    public abstract Map<String, String> groupByFields();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    // Our legacy search code is adding an "_id" field that we want to ignore because it's the same as the "id".
    @JsonIgnoreProperties({"_id"})
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventDto.Builder().sourceStreams(ImmutableSet.of()).groupByFields(ImmutableMap.of());
        }

        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_EVENT_DEFINITION_TYPE)
        public abstract Builder eventDefinitionType(String eventDefinitionType);

        @JsonProperty(FIELD_EVENT_DEFINITION_ID)
        public abstract Builder eventDefinitionId(String eventDefinitionId);

        @JsonProperty(FIELD_ORIGIN_CONTEXT)
        public abstract Builder originContext(@Nullable String originContext);

        @JsonProperty(FIELD_EVENT_TIMESTAMP)
        @JsonDeserialize(using = ESMongoDateTimeDeserializer.class)
        public abstract Builder eventTimestamp(DateTime eventTimestamp);

        @JsonProperty(FIELD_PROCESSING_TIMESTAMP)
        @JsonDeserialize(using = ESMongoDateTimeDeserializer.class)
        public abstract Builder processingTimestamp(DateTime processingTimestamp);

        @JsonProperty(FIELD_TIMERANGE_START)
        @JsonDeserialize(using = ESMongoDateTimeDeserializer.class)
        public abstract Builder timerangeStart(@Nullable DateTime timerangeStart);

        @JsonProperty(FIELD_TIMERANGE_END)
        @JsonDeserialize(using = ESMongoDateTimeDeserializer.class)
        public abstract Builder timerangeEnd(@Nullable DateTime timerangeEnd);

        @JsonProperty(FIELD_STREAMS)
        public abstract Builder streams(Set<String> streams);

        @JsonProperty(FIELD_SOURCE_STREAMS)
        public abstract Builder sourceStreams(Set<String> sourceStreams);

        @JsonProperty(FIELD_MESSAGE)
        public abstract Builder message(String message);

        @JsonProperty(FIELD_SOURCE)
        public abstract Builder source(String source);

        @JsonProperty(FIELD_KEY_TUPLE)
        public abstract Builder keyTuple(List<String> keyTuple);

        @JsonProperty(FIELD_KEY)
        public abstract Builder key(@Nullable String key);

        @JsonProperty(FIELD_PRIORITY)
        public abstract Builder priority(long priority);

        @JsonProperty(FIELD_ALERT)
        public abstract Builder alert(boolean alert);

        @JsonProperty(FIELD_FIELDS)
        public abstract Builder fields(Map<String, String> fields);

        @JsonProperty(FIELD_GROUP_BY_FIELDS)
        public abstract Builder groupByFields(Map<String, String> fields);

        public abstract EventDto build();
    }
}
