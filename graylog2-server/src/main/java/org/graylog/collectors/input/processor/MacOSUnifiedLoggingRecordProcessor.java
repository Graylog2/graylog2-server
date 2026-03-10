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
import com.google.common.base.Strings;
import jakarta.inject.Inject;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog.schema.EventFields;
import org.graylog.schema.ProcessFields;
import org.graylog.schema.UserFields;
import org.graylog.schema.VendorFields;
import org.graylog2.jackson.TypeReferences;
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
    public Map<String, Object> process(OTelJournal.Log log) {
        final var bodyString = log.getLogRecord().getBody().getStringValue();
        if (bodyString.isBlank()) {
            return Map.of();
        }

        final Map<String, Object> body;
        try {
            body = objectMapper.readValue(bodyString, TypeReferences.MAP_STRING_OBJECT);
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
                case "eventMessage" -> putAsString(result, Message.FIELD_MESSAGE, value);
                // Log event type: logEvent, activityCreateEvent, stateEvent, userActionEvent, timesyncEvent.
                case "eventType" -> putAsString(result, VendorFields.VENDOR_EVENT_TYPE, value);
                // Apple log level: Default, Error, Fault.
                case "messageType" -> putAsString(result, VendorFields.VENDOR_EVENT_SEVERITY, value);
                // Reverse-DNS subsystem identifier (e.g. com.apple.xpc).
                case "subsystem" -> putAsString(result, EventFields.EVENT_SOURCE, value);
                // Category within the subsystem.
                case "category" -> putAsString(result, VendorFields.VENDOR_SUBTYPE, value);
                // Apple format template string.
                case "formatString" -> putAsString(result, VendorFields.VENDOR_EVENT_DESCRIPTION, value);
                // Process ID of the logging process.
                case "processID" -> putAsString(result, ProcessFields.PROCESS_ID, value);
                // User ID of the logging process.
                case "userID" -> putAsString(result, UserFields.USER_ID, value);
                // Full path to the process executable.
                case "processImagePath" -> {
                    final var path = Strings.emptyToNull(String.valueOf(value));
                    if (path != null) {
                        processImagePath = path;
                        result.put(ProcessFields.PROCESS_PATH, path);
                    }
                }
                // Trace identifier for correlating related events.
                case "traceID" -> putAsString(result, EventFields.EVENT_UID, value);
                // Event timestamp in macOS format.
                case "timestamp" -> {
                    final var ts = parseTimestamp(Strings.emptyToNull(String.valueOf(value)));
                    if (ts != null) {
                        result.put(VendorFields.VENDOR_EVENT_TIMESTAMP, ts);
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
                //case "bootUUID" -> {
                // TODO boot session identifier; no direct GIM mapping yet.
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

    private static void putAsString(Map<String, Object> target, String fieldName, Object value) {
        final var s = Strings.emptyToNull(String.valueOf(value));
        if (s != null) {
            target.put(fieldName, s);
        }
    }

    private static Instant parseTimestamp(String value) {
        if (value == null) {
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
