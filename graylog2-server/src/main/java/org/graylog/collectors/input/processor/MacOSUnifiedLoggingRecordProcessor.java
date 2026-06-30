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
import org.graylog.schema.ProcessFields;
import org.graylog.schema.UserFields;
import org.graylog.schema.VendorFields;

import java.util.HashMap;
import java.util.Map;

/**
 * Processes macOS unified logging records into GIM fields.
 * <p>
 * The fixed receiver emits a plain-text body (the human-readable message), native OTel severity
 * and timestamp — all handled by {@link org.graylog.collectors.input.CollectorIngestCodec} — plus
 * structured {@code macos.*} attributes. This processor maps only those attributes: well-fitting
 * ones to GIM fields, and macOS-specific identifiers (boot session, activity/trace IDs, sender
 * image) preserved under a {@code macos_*} prefix.
 *
 * @see <a href="https://developer.apple.com/documentation/os/logging">Apple Unified Logging</a>
 */
public class MacOSUnifiedLoggingRecordProcessor implements LogRecordProcessor {

    @Override
    public Map<String, Object> process(OTelJournal.Log log) {
        final Map<String, Object> result = new HashMap<>();
        String processImagePath = null;

        for (final var attr : log.getLogRecord().getAttributesList()) {
            final var value = attr.getValue();
            switch (attr.getKey()) {
                // Mapped to GIM fields.
                case "macos.subsystem" -> putStr(result, EventFields.EVENT_SOURCE, value.getStringValue());
                case "macos.category" -> putStr(result, VendorFields.VENDOR_SUBTYPE, value.getStringValue());
                case "macos.eventType" -> putStr(result, VendorFields.VENDOR_EVENT_TYPE, value.getStringValue());
                case "macos.formatString" -> putStr(result, VendorFields.VENDOR_EVENT_DESCRIPTION, value.getStringValue());
                case "macos.processImagePath" -> {
                    final var path = emptyToNull(value.getStringValue());
                    if (path != null) {
                        processImagePath = path;
                        result.put(ProcessFields.PROCESS_PATH, path);
                    }
                }
                case "macos.processID" -> result.put(ProcessFields.PROCESS_ID, value.getIntValue());
                case "macos.userID" -> result.put(UserFields.USER_ID, value.getIntValue());
                // macOS-specific identifiers preserved under a macos_* prefix.
                case "macos.threadID" -> result.put("macos_thread_id", value.getIntValue());
                case "macos.bootUUID" -> putStr(result, "macos_boot_uuid", value.getStringValue());
                case "macos.machTimestamp" -> result.put("macos_mach_timestamp", value.getIntValue());
                case "macos.traceID" -> result.put("macos_trace_id", value.getIntValue());
                case "macos.activityIdentifier" -> result.put("macos_activity_id", value.getIntValue());
                case "macos.parentActivityIdentifier" -> result.put("macos_parent_activity_id", value.getIntValue());
                case "macos.creatorActivityID" -> result.put("macos_creator_activity_id", value.getIntValue());
                case "macos.processImageUUID" -> putStr(result, "macos_process_image_uuid", value.getStringValue());
                case "macos.senderImagePath" -> putStr(result, "macos_sender_image_path", value.getStringValue());
                case "macos.senderImageUUID" -> putStr(result, "macos_sender_image_uuid", value.getStringValue());
                case "macos.senderProgramCounter" -> result.put("macos_sender_program_counter", value.getIntValue());
                default -> {
                    // Ignore non-macos attributes.
                }
            }
        }

        // Derive the process name from the executable path.
        if (processImagePath != null) {
            final var lastSlash = processImagePath.lastIndexOf('/');
            final var name = lastSlash >= 0 ? processImagePath.substring(lastSlash + 1) : processImagePath;
            if (!name.isEmpty()) {
                result.put(ProcessFields.PROCESS_NAME, name);
            }
        }

        return result;
    }

    private static void putStr(Map<String, Object> target, String field, String value) {
        if (value != null && !value.isEmpty()) {
            target.put(field, value);
        }
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }
}
