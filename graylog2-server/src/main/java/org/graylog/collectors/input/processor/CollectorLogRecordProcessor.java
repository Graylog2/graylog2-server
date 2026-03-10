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

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal LogRecordProcessor for collector self-logs (supervisor and collector process).
 * <p>
 * TODO: Finalize field mapping once we can inspect real collector self-log payloads.
 *  The current mapping is preliminary.
 */
public class CollectorLogRecordProcessor implements LogRecordProcessor {

    public static final String RECEIVER_TYPE = "collector_log";

    @Override
    public Map<String, Object> process(OTelJournal.Log log) {
        final Map<String, Object> result = new HashMap<>();

        for (final var attr : log.getResource().getAttributesList()) {
            switch (attr.getKey()) {
                case "service.name" -> result.put("collector_service_name",
                        attr.getValue().getStringValue());
                case "service.version" -> result.put("collector_service_version",
                        attr.getValue().getStringValue());
            }
        }

        return result;
    }
}
