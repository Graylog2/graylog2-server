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
package org.graylog.collectors.input.processor;

import org.graylog.inputs.otel.OTelJournal;
import org.graylog.schema.EventFields;

import java.util.HashMap;
import java.util.Map;

public class FilelogRecordProcessor implements LogRecordProcessor {
    @Override
    public Map<String, Object> process(OTelJournal.Log log) {
        final Map<String, Object> result = new HashMap<>();

        for (final var attr : log.getLogRecord().getAttributesList()) {
            switch (attr.getKey()) {
                case "log.file.name" -> {
                    final var name = attr.getValue().getStringValue();
                    result.put(EventFields.EVENT_LOG_NAME, name);
                }
//                case "log.file.name_resolved" -> result.put(attr.getKey(), attr.getValue());
//                case "log.file.owner.group.name" -> result.put(attr.getKey(), attr.getValue());
//                case "log.file.owner.name" -> result.put(attr.getKey(), attr.getValue());
                case "log.file.path" -> {
                    final var path = attr.getValue().getStringValue();
                    result.put(EventFields.EVENT_LOG_PATH, path);
                }
//                case "log.file.path_resolved" -> result.put(attr.getKey(), attr.getValue());
//                case "log.file.record_number" -> result.put(attr.getKey(), attr.getValue());
//                case "log.file.record_offset" -> result.put(attr.getKey(), attr.getValue());
            }
        }

        return result;
    }
}
