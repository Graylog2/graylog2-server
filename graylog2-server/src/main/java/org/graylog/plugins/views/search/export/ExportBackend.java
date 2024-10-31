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
package org.graylog.plugins.views.search.export;

import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.graylog2.plugin.Tools.ES_DATE_FORMAT_FORMATTER;

public interface ExportBackend {

    Logger LOG = LoggerFactory.getLogger(ExportBackend.class);

    void run(ExportMessagesCommand request, Consumer<SimpleMessageChunk> chunkCollector);

    default Object valueFrom(Map source, String name, DateTimeZone timeZone) {
        if (name.equals(Message.FIELD_TIMESTAMP)) {
            return fixTimestampFormat(source.get(Message.FIELD_TIMESTAMP), timeZone);
        }
        return source.get(name);
    }

    default Object fixTimestampFormat(Object rawTimestamp, DateTimeZone timeZone) {
        try {
            final DateTime parsed = ES_DATE_FORMAT_FORMATTER.parseDateTime(String.valueOf(rawTimestamp));
            return parsed.withZone(timeZone).toString();
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not parse timestamp {}", rawTimestamp, e);
            return rawTimestamp;
        }
    }

    default SimpleMessage buildHitWithAllFields(Map source, String index, DateTimeZone timeZone) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

        for (Object key : source.keySet()) {
            String name = (String) key;
            Object value = valueFrom(source, name, timeZone);
            fields.put(name, value);
        }

        // _id is needed, because the old decorators implementation relies on it
        fields.put("_id", UUID.randomUUID().toString());

        return SimpleMessage.from(index, fields);
    }
}
