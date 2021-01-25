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
import com.google.common.collect.Sets;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * MessageSummary is being used as a return value for AlarmCallbacks.
 */
public class MessageSummary {
    // for these fields we define individual json properties
    private static final HashSet<String> RESERVED_FIELDS = Sets.newHashSet(Message.FIELD_ID,
                                                                          Message.FIELD_MESSAGE,
                                                                          Message.FIELD_SOURCE,
                                                                          Message.FIELD_TIMESTAMP,
                                                                          Message.FIELD_STREAMS);
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
        Map<String, Object> genericFields = Maps.newHashMap();

        // strip out common "fields" that we report as individual properties
        for (Map.Entry<String, Object> entry : message.getFieldsEntries()) {
            if (!RESERVED_FIELDS.contains(entry.getKey())) {
                genericFields.put(entry.getKey(), entry.getValue());
            }

        }

        return genericFields;
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
