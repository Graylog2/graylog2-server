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
package org.graylog.failure;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.graylog2.indexer.messages.Indexable;
import org.joda.time.DateTime;

import java.util.Map;

import static org.graylog2.plugin.Tools.buildElasticSearchTimeFormat;
import static org.graylog2.plugin.streams.Stream.FAILURES_STREAM_ID;
import static org.joda.time.DateTimeZone.UTC;

public class ESFailureObject implements Indexable {
    private final Map<String, Object> fields;

    public ESFailureObject(Map<String, Object> fields) {
        this.fields = fields;
    }

    @Override
    public String getId() {
        return getFieldAs(String.class, "id");
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public DateTime getReceiveTime() {
        return null;
    }

    @Override
    public Map<String, Object> toElasticSearchObject(ObjectMapper objectMapper, @NonNull Meter invalidTimestampMeter) {
        final Map<String, Object> obj = Maps.newHashMapWithExpectedSize(5 + fields.size());

        obj.putAll(fields);
        obj.put("timestamp", buildElasticSearchTimeFormat(getTimestamp().withZone(UTC)));
        obj.put("streams", ImmutableList.of(FAILURES_STREAM_ID));

        return obj;
    }

    @Override
    public DateTime getTimestamp() {
        return getFieldAs(DateTime.class, "timestamp").withZone(UTC);
    }

    public <T> T getFieldAs(final Class<T> T, final String key) throws ClassCastException {
        return T.cast(getField(key));
    }

    public Object getField(final String key) {
        return fields.get(key);
    }
}
