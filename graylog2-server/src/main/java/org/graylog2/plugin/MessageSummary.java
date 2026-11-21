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
package org.graylog2.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Map;

/**
 * MessageSummary is being used as a return value for AlarmCallbacks.
 */
public class MessageSummary {
    @JsonIgnore
    private final String index;

    @JsonIgnore
    private final Message message;

    public MessageSummary(String index, Message message) {
        this.index = index;
        this.message = message;
    }

    @JsonProperty
    public String getIndex() {
        return index;
    }

    @JsonProperty
    public String getId() {
        return message.getId();
    }

    @JsonProperty
    public String getSource() {
        return message.getSource();
    }

    @JsonProperty
    public String getMessage() {
        return message.getMessage();
    }

    @JsonProperty
    public DateTime getTimestamp() {
        return message.getTimestamp();
    }

    @JsonProperty
    public Collection<String> getStreamIds() {
        return message.getStreamIds();
    }

    @JsonProperty
    public Map<String, Object> getFields() {
        // strip out common "fields" that we report as individual properties
        return Maps.newHashMap(message.getFields(key -> !isIndividualProperty(key)));
    }

    private static boolean isIndividualProperty(String key) {
        return Message.STANDARD_FIELDS.contains(key) || Message.FIELD_STREAMS.equals(key);
    }

    @JsonIgnore
    public boolean hasField(String key) {
        return message.hasField(key);
    }

    @JsonIgnore
    public Object getField(String key) {
        return message.getField(key);
    }

    @JsonIgnore
    public Message getRawMessage() { return message;}
}
