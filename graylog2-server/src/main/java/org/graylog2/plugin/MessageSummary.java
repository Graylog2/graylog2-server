/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.time.DateTime;

import java.util.HashSet;
import java.util.List;
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
    public List<String> getStreamIds() {
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
