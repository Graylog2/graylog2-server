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
package org.graylog2.indexer.results;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;

public class ResultMessage {
    private static final Logger LOG = LoggerFactory.getLogger(ResultMessage.class);

    private Message message;
    private String index;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Multimap<String, Range<Integer>> highlightRanges;

    protected ResultMessage() { /* use factory method */}

    private ResultMessage(String id, String index, Map<String, Object> message, Multimap<String, Range<Integer>> highlightRanges) {
        this.index = index;
        this.highlightRanges = highlightRanges;
        setMessage(id, message);
    }

    public static ResultMessage parseFromSource(String id, String index, Map<String, Object> message) {
        return parseFromSource(id, index, message, Collections.emptyMap());
    }

    public static ResultMessage parseFromSource(String id, String index, Map<String, Object> message, Map<String, List<String>> highlight) {
        return new ResultMessage(id, index, message, HighlightParser.extractHighlightRanges(highlight));
    }

    public static ResultMessage createFromMessage(Message message) {
        ResultMessage m = new ResultMessage();
        m.setMessage(message);
        return m;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void setMessage(String id, Map<String, Object> message) {
        Map<String, Object> tmp = Maps.newHashMap();
        tmp.putAll(message);
        tmp.put(Message.FIELD_ID, id);
        if (tmp.containsKey(Message.FIELD_TIMESTAMP)) {
            final Object tsField = tmp.get(Message.FIELD_TIMESTAMP);
            try {
                tmp.put(Message.FIELD_TIMESTAMP, ES_DATE_FORMAT_FORMATTER.parseDateTime(String.valueOf(tsField)));
            } catch (IllegalArgumentException e) {
                // could not parse date string, this is likely a bug, but we will leave the original value alone
                LOG.warn("Could not parse timestamp of message {}", message.get("id"), e);
            }
        }
        this.message = new Message(tmp);
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public Message getMessage() {
        return message;
    }

    public String getIndex() {
        return index;
    }
}
