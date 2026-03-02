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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.proto.logs.v1.LogRecord;
import jakarta.inject.Inject;
import org.graylog.schema.EventFields;
import org.graylog.schema.ProcessFields;
import org.graylog.schema.SourceFields;
import org.graylog.schema.UserFields;
import org.graylog.schema.VendorFields;
import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Processes macOS unified logging messages into GIM format.
 * <p>
 * The macOS unified logging receiver sends log records where the body is a JSON string
 * containing fields such as eventMessage, eventType, processID, subsystem, category, etc.
 *
 * @see <a href="https://developer.apple.com/documentation/os/logging">Apple Unified Logging</a>
 */
public class MacOSUnifiedLoggingRecordProcessor implements LogRecordProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MacOSUnifiedLoggingRecordProcessor.class);

    // macOS log timestamps: "2026-02-26 14:30:45.123456+0000"
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendPattern("Z")
            .toFormatter(Locale.ROOT);

    private final ObjectMapper objectMapper;

    @Inject
    public MacOSUnifiedLoggingRecordProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> process(LogRecord logRecord) {
        final var bodyString = logRecord.getBody().getStringValue();
        if (bodyString == null || bodyString.isBlank()) {
            return Map.of();
        }

        final Map<String, Object> body;
        try {
            body = objectMapper.readValue(bodyString, Map.class);
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to parse macOS unified log body as JSON: {}", e.getMessage());
            return Map.of();
        }

        final Map<String, Object> result = new HashMap<>();

        String processImagePath = null;

        for (final var entry : body.entrySet()) {
            final var value = entry.getValue();
            if (value == null) {
                continue;
            }

            switch (entry.getKey()) {
                // Human-readable log message.
                case "eventMessage" -> putStringIfPresent(result, Message.FIELD_MESSAGE, value);
                // Log event type: logEvent, activityCreateEvent, stateEvent, userActionEvent, timesyncEvent.
                case "eventType" -> putStringIfPresent(result, VendorFields.VENDOR_EVENT_TYPE, value);
                // Apple log level: Default, Error, Fault.
                case "messageType" -> putStringIfPresent(result, VendorFields.VENDOR_EVENT_SEVERITY, value);
                // Reverse-DNS subsystem identifier (e.g. com.apple.xpc).
                case "subsystem" -> putStringIfPresent(result, EventFields.EVENT_SOURCE, value);
                // Category within the subsystem.
                case "category" -> putStringIfPresent(result, VendorFields.VENDOR_SUBTYPE, value);
                // Apple format template string.
                case "formatString" -> putStringIfPresent(result, VendorFields.VENDOR_EVENT_DESCRIPTION, value);
                // Process ID of the logging process.
                case "processID" -> putNumericAsString(result, ProcessFields.PROCESS_ID, value);
                // User ID of the logging process.
                case "userID" -> putNumericAsString(result, UserFields.USER_ID, value);
                // Full path to the process executable.
                case "processImagePath" -> {
                    final var path = stringValue(value);
                    if (path != null) {
                        processImagePath = path;
                        result.put(ProcessFields.PROCESS_PATH, path);
                    }
                }
                // Trace identifier for correlating related events.
                case "traceID" -> putStringIfPresent(result, EventFields.EVENT_UID, value);
                // Boot UUID identifying the boot session.
                case "bootUUID" -> putStringIfPresent(result, SourceFields.SOURCE_REFERENCE, value);
                // Event timestamp in macOS format.
                case "timestamp" -> {
                    final var ts = parseTimestamp(stringValue(value));
                    if (ts != null) {
                        result.put("vendor_event_timestamp", ts);
                    }
                }
                // Unmapped fields (preserved as commented-out cases for future mapping):
                //case "threadID" -> {
                // TODO no direct GIM mapping yet.
                //}
                //case "senderImagePath" -> {
                // TODO no direct GIM mapping yet.
                //}
                //case "senderImageUUID" -> {
                // TODO no direct GIM mapping yet.
                //}
                //case "senderProgramCounter" -> {
                // TODO no direct GIM mapping yet.
                //}
                //case "machTimestamp" -> {
                // TODO redundant with timestamp.
                //}
                //case "activityIdentifier" -> {
                // TODO no direct GIM mapping yet.
                //}
                //case "parentActivityIdentifier" -> {
                // TODO no direct GIM mapping yet.
                //}
                //case "timezoneName" -> {
                // TODO no direct GIM mapping yet.
                //}
            }
        }

        // Derive process name from path.
        if (processImagePath != null) {
            final var lastSlash = processImagePath.lastIndexOf('/');
            final var name = lastSlash >= 0 ? processImagePath.substring(lastSlash + 1) : processImagePath;
            if (!name.isEmpty()) {
                result.put(ProcessFields.PROCESS_NAME, name);
            }
        }

        return result;
    }

    private static String stringValue(Object value) {
        if (value instanceof String s && !s.isEmpty()) {
            return s;
        }
        return null;
    }

    private static void putStringIfPresent(Map<String, Object> target, String fieldName, Object value) {
        final var s = stringValue(value);
        if (s != null) {
            target.put(fieldName, s);
        }
    }

    private static void putNumericAsString(Map<String, Object> target, String fieldName, Object value) {
        if (value instanceof Number n) {
            target.put(fieldName, Long.toString(n.longValue()));
        }
    }

    private static Instant parseTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return TIMESTAMP_FORMATTER.parse(value, Instant::from);
        } catch (DateTimeParseException e) {
            LOG.debug("Failed to parse macOS timestamp: {}", value);
            return null;
        }
    }
}
