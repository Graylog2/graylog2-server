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
package org.graylog2.streams.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class StreamsChangedEvent {
    private static final String FIELD_STREAM_IDS = "stream_ids";

    @JsonProperty(FIELD_STREAM_IDS)
    public abstract ImmutableSet<String> streamIds();

    @JsonCreator
    public static StreamsChangedEvent create(@JsonProperty(FIELD_STREAM_IDS) ImmutableSet<String> streamIds) {
        return new AutoValue_StreamsChangedEvent(streamIds);
    }

    public static StreamsChangedEvent create(String streamId) {
        return create(ImmutableSet.of(streamId));
    }
}