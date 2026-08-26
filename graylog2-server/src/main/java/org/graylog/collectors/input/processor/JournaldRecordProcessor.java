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

import io.opentelemetry.proto.common.v1.AnyValue;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog.schema.AssociatedFields;
import org.graylog.schema.EventFields;
import org.graylog.schema.HostFields;
import org.graylog.schema.ProcessFields;
import org.graylog.schema.ServiceFields;
import org.graylog.schema.UserFields;
import org.graylog.schema.VendorFields;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Processes journald messages into GIM format.
 *
 * @see <a href="https://www.freedesktop.org/software/systemd/man/latest/systemd.journal-fields.html">systemd.journal-fields</a>
 */
public class JournaldRecordProcessor implements LogRecordProcessor {
    private static final Pattern ANSI_ESCAPE_PATTERN = Pattern.compile("\\u001B\\[[\\d;]*[ -/]*[@-~]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final DateTimeFormatter SYSLOG_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM d HH:mm:ss")
            .parseDefaulting(ChronoField.YEAR, 2000)
            .toFormatter(Locale.ENGLISH);

    @Override
    public Map<String, Object> process(OTelJournal.Log log) {
        final Map<String, Object> result = new HashMap<>();

        String systemdUnit = null;
        String systemdUserUnit = null;
        String syslogIdentifier = null;

        Long pid = null;
        Long syslogPid = null;

        Long auditLoginUid = null;
        Long systemdOwnerUid = null;

        Long auditSessionId = null;
        String systemdSessionId = null;

        String bootId = null;
        String streamId = null;

        String udevSysname = null;
        String kernelDevice = null;

        Long sourceRealtimeTimestampMicros = null;
        Long realtimeTimestampMicros = null;
        DateTime syslogTimestamp = null;

        for (final var field : log.getLogRecord().getBody().getKvlistValue().getValuesList()) {
            final var fieldValue = field.getValue();

            switch (field.getKey()) {
                // Human-readable primary message text for the entry.
                case "MESSAGE" ->
                        putIfPresent(result, Message.FIELD_MESSAGE, extractMessage(fieldValue)); // example: [02/26/26, 18:13:06:433] info: [API-Q] (T024L0HBU) noversion-1772125986.431 Flannel users/info is ACTIVE
                // Syslog-compatible priority level, decimal 0 (emerg) to 7 (debug).
                case "PRIORITY" ->
                        putNumericIfPresent(result, VendorFields.VENDOR_EVENT_SEVERITY_LEVEL, extractPriority(fieldValue)); // example: 6
                // Syslog compatibility facility value, formatted as decimal.
                case "SYSLOG_FACILITY" ->
                        putNumericAsStringIfPresent(result, "vendor_event_category", extractNumber(fieldValue)); // example: 0
                // Syslog compatibility identifier string ("tag").
                case "SYSLOG_IDENTIFIER" -> syslogIdentifier = extractString(fieldValue); // example: slack.desktop
                // Syslog compatibility client PID.
                case "SYSLOG_PID" -> syslogPid = extractNumber(fieldValue); // example: 1072719
                // Syslog compatibility timestamp from the original datagram.
                case "SYSLOG_TIMESTAMP" ->
                        syslogTimestamp = formatSyslogTimestamp(extractString(fieldValue)); // example: Feb 26 18:15:01
                // Original syslog line payload as received, when MESSAGE or timestamp had to be adjusted.
                //case "SYSLOG_RAW" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_syslog_raw", fieldValue); // example: <6>Feb 26 18:15:01 h2 cron[1072719]: pam_unix(cron:session): session closed for user root
                //}
                // Documentation URL related to the log event.
                //case "DOCUMENTATION" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_documentation", fieldValue); // example: https://www.freedesktop.org/software/systemd/man/systemd.service.html
                //}
                // Source file name where the log call was issued.
                //case "CODE_FILE" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_code_file", fieldValue); // example: src/core/main.c
                //}
                // Source line number where the log call was issued.
                //case "CODE_LINE" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_code_line", fieldValue); // example: 412
                //}
                // Source function name where the log call was issued.
                //case "CODE_FUNC" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_code_func", fieldValue); // example: handle_request
                //}
                // Untrusted, user-supplied invocation ID.
                //case "INVOCATION_ID" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_invocation_id", fieldValue); // example: c5688e445fef448ea255eb3755552620
                //}
                // Untrusted, user-supplied user-manager invocation ID.
                //case "USER_INVOCATION_ID" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_user_invocation_id", fieldValue); // example: 14764bddd5b848cd95dbc7e44d44c67d
                //}
                // Numeric errno value that caused the entry.
                //case "ERRNO" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_errno", fieldValue); // example: 2
                //}
                // Numeric thread ID of the originating thread.
                //case "TID" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_tid", fieldValue); // example: 1
                //}
                // 128-bit lowercase hex message type identifier.
                //case "MESSAGE_ID" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_message_id", fieldValue); // example: 7ad2d189f7e94e70a38c781354912448
                //}
                // Unit name used by managers when logging about specific units (untrusted user field).
                //case "UNIT" -> {
                // TODO no direct GIM mapping yet.
                // Untrusted, user-supplied systemd unit field.
                // result.put("gl2_collector_journald_unit", fieldValue); // example: ssh.service
                //}
                // User-manager unit name used when logging about specific units (untrusted user field).
                //case "USER_UNIT" -> {
                // TODO no direct GIM mapping yet.
                // Untrusted, user-supplied systemd user unit field.
                // result.put("gl2_collector_journald_user_unit", fieldValue); // example: app-org.chromium.Chromium-7371.scope
                //}
                // Kernel audit login UID of the originating process.
                case "_AUDIT_LOGINUID" -> auditLoginUid = extractNumber(fieldValue); // example: 1000
                // Kernel audit session ID of the originating process.
                case "_AUDIT_SESSION" -> auditSessionId = extractNumber(fieldValue); // example: 3
                // Kernel boot ID for the boot the message was generated in (128-bit hex).
                case "_BOOT_ID" -> bootId = extractString(fieldValue); // example: dcae62f433304b8290b9372b7bdcde6c
                // Effective Linux capabilities bitmask of the originating process.
                //case "_CAP_EFFECTIVE" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_cap_effective", fieldValue); // example: 0
                //}
                // Command line of the originating process.
                case "_CMDLINE" ->
                        putIfPresent(result, ProcessFields.PROCESS_COMMAND_LINE, extractString(fieldValue)); // example: /usr/bin/slack
                // Process name of the originating process.
                case "_COMM" ->
                        putIfPresent(result, ProcessFields.PROCESS_NAME, extractString(fieldValue)); // example: slack
                // Executable path of the originating process.
                case "_EXE" ->
                        putIfPresent(result, ProcessFields.PROCESS_PATH, extractString(fieldValue)); // example: /usr/lib/slack/slack
                // Group ID of the originating process, formatted as decimal.
                //case "_GID" ->
                //        putNumericAsStringIfPresent(result, "group_id", extractNumber(fieldValue)); // example: 1000
                // Hostname of the originating host.
                case "_HOSTNAME" ->
                        putIfPresent(result, HostFields.HOST_HOSTNAME, extractString(fieldValue)); // example: h2
                // Kernel device name encoded by device type.
                case "_KERNEL_DEVICE" -> kernelDevice = extractString(fieldValue); // example: +pci:0000:00:08.1
                // Kernel subsystem name.
                //case "_KERNEL_SUBSYSTEM" ->
                //        putIfPresent(result, VendorFields.VENDOR_SUBTYPE, extractString(fieldValue)); // example: pci
                // Machine ID of the originating host.
                case "_MACHINE_ID" ->
                        putIfPresent(result, HostFields.HOST_ID, extractString(fieldValue)); // example: 3d758250c1e84341a6d2037786a25bdf
                // Process ID of the originating process, formatted as decimal.
                case "_PID" -> pid = extractNumber(fieldValue); // example: 7371
                // Runtime scope where message was logged: initrd or system.
                //case "_RUNTIME_SCOPE" ->
                //        putIfPresent(result, SourceFields.SOURCE_TYPE, extractString(fieldValue)); // example: system
                // SELinux security context (label) of the originating process.
                //case "_SELINUX_CONTEXT" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_selinux_context", fieldValue); // example: slack (unconfined)\n
                //}
                // Exported source monotonic timestamp field (not documented in latest systemd.journal-fields).
                //case "_SOURCE_MONOTONIC_TIMESTAMP" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_source_monotonic_timestamp", fieldValue); // example: 202182742521
                //}
                // Earliest trusted CLOCK_REALTIME timestamp in microseconds.
                case "_SOURCE_REALTIME_TIMESTAMP" ->
                        sourceRealtimeTimestampMicros = extractNumber(fieldValue); // example: 1772126101554715
                // Earliest trusted CLOCK_BOOTTIME timestamp in microseconds.
                //case "_SOURCE_BOOTTIME_TIMESTAMP" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_source_boottime_timestamp", fieldValue); // example: 202182742521
                //}
                // For stdout transport, indicates non-newline line termination reason.
                //case "_LINE_BREAK" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_line_break", fieldValue); // example: line-max
                //}
                // Journal namespace identifier for non-default namespaces.
                //case "_NAMESPACE" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_namespace", fieldValue); // example: customer-a
                //}
                // For stdout transport, randomized 128-bit stream connection ID.
                //case "_STREAM_ID" -> streamId = extractString(fieldValue); // example: a6f7a95da7d74ba6abe480ab239dc594
                // Systemd cgroup path of the originating process.
                //case "_SYSTEMD_CGROUP" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_systemd_cgroup", fieldValue); // example: /user.slice/user-1000.slice/user@1000.service/app.slice/app-org.chromium.Chromium-7371.scope
                //}
                // Invocation ID of the unit runtime cycle where the message was generated.
                case "_SYSTEMD_INVOCATION_ID" ->
                        putIfPresent(result, ProcessFields.PROCESS_UID, extractString(fieldValue)); // example: 14764bddd5b848cd95dbc7e44d44c67d
                // Owner UID of the systemd user unit or systemd session.
                case "_SYSTEMD_OWNER_UID" -> systemdOwnerUid = extractNumber(fieldValue); // example: 1000
                // Systemd session ID of the originating process.
                case "_SYSTEMD_SESSION" -> systemdSessionId = extractString(fieldValue); // example: 3
                // Systemd slice unit name of the originating process.
                //case "_SYSTEMD_SLICE" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_systemd_slice", fieldValue); // example: user-1000.slice
                //}
                // Systemd unit name of the originating process.
                case "_SYSTEMD_UNIT" -> systemdUnit = extractString(fieldValue); // example: user@1000.service
                // Systemd user slice unit name of the originating process.
                //case "_SYSTEMD_USER_SLICE" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_systemd_user_slice", fieldValue); // example: app.slice
                //}
                // Systemd user manager unit name of the originating process.
                case "_SYSTEMD_USER_UNIT" ->
                        systemdUserUnit = extractString(fieldValue); // example: app-org.chromium.Chromium-7371.scope
                // Transport by which journald received the entry.
                case "_TRANSPORT" -> {
                    // journald transport enum: audit, driver, syslog, journal, stdout, kernel
                    putIfPresent(result, EventFields.EVENT_SOURCE_INPUT, extractString(fieldValue)); // example: stdout
                }
                // Kernel device name as shown in /sys device tree.
                case "_UDEV_SYSNAME" -> udevSysname = extractString(fieldValue); // example: 0000:00:08.1
                // Device node path in /dev.
                //case "_UDEV_DEVNODE" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_udev_devnode", fieldValue); // example: /dev/sda
                //}
                // Additional /dev symlink names for the device node (may appear multiple times).
                //case "_UDEV_DEVLINK" -> {
                // TODO no direct GIM mapping yet.
                // Spec allows multiple values for this field per entry.
                // result.put("gl2_collector_journald_udev_devlink", fieldValue); // example: /dev/disk/by-id/wwn-0x50014ee2b58f7b03
                //}
                // User ID of the originating process, formatted as decimal.
                case "_UID" ->
                        putNumericAsStringIfPresent(result, UserFields.USER_ID, extractNumber(fieldValue)); // example: 1000
                // Annotates coredump messages with the related system unit.
                //case "COREDUMP_UNIT" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_coredump_unit", fieldValue); // example: ssh.service
                //}
                // Annotates coredump messages with the related user/session unit.
                //case "COREDUMP_USER_UNIT" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_coredump_user_unit", fieldValue); // example: app-org.chromium.Chromium-7371.scope
                //}
                // PID of the program this message pertains to when logging on behalf of another program.
                //case "OBJECT_PID" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_pid", fieldValue); // example: 7371
                //}
                // Like _UID, but for the OBJECT_PID process.
                //case "OBJECT_UID" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_uid", fieldValue); // example: 1000
                //}
                // Like _GID, but for the OBJECT_PID process.
                //case "OBJECT_GID" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_gid", fieldValue); // example: 1000
                //}
                // Like _COMM, but for the OBJECT_PID process.
                //case "OBJECT_COMM" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_comm", fieldValue); // example: sshd
                //}
                // Like _EXE, but for the OBJECT_PID process.
                //case "OBJECT_EXE" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_exe", fieldValue); // example: /usr/sbin/sshd
                //}
                // Like _CMDLINE, but for the OBJECT_PID process.
                //case "OBJECT_CMDLINE" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_cmdline", fieldValue); // example: sshd: user [priv]
                //}
                // Like _AUDIT_LOGINUID, but for the OBJECT_PID process.
                //case "OBJECT_AUDIT_LOGINUID" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_audit_loginuid", fieldValue); // example: 1000
                //}
                // Like _AUDIT_SESSION, but for the OBJECT_PID process.
                //case "OBJECT_AUDIT_SESSION" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_audit_session", fieldValue); // example: 3
                //}
                // Like _SYSTEMD_CGROUP, but for the OBJECT_PID process.
                //case "OBJECT_SYSTEMD_CGROUP" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_systemd_cgroup", fieldValue); // example: /system.slice/ssh.service
                //}
                // Like _SYSTEMD_SESSION, but for the OBJECT_PID process.
                //case "OBJECT_SYSTEMD_SESSION" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_systemd_session", fieldValue); // example: 3
                //}
                // Like _SYSTEMD_OWNER_UID, but for the OBJECT_PID process.
                //case "OBJECT_SYSTEMD_OWNER_UID" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_systemd_owner_uid", fieldValue); // example: 1000
                //}
                // Like _SYSTEMD_UNIT, but for the OBJECT_PID process.
                //case "OBJECT_SYSTEMD_UNIT" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_systemd_unit", fieldValue); // example: ssh.service
                //}
                // Like _SYSTEMD_USER_UNIT, but for the OBJECT_PID process.
                //case "OBJECT_SYSTEMD_USER_UNIT" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_systemd_user_unit", fieldValue); // example: app-org.chromium.Chromium-7371.scope
                //}
                // Like _SYSTEMD_INVOCATION_ID, but for the OBJECT_PID process.
                //case "OBJECT_SYSTEMD_INVOCATION_ID" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_object_systemd_invocation_id", fieldValue); // example: c5688e445fef448ea255eb3755552620
                //}
                // Opaque cursor uniquely describing this entry position in the journal.
                case "__CURSOR" ->
                        putIfPresent(result, EventFields.EVENT_UID, extractString(fieldValue)); // example: s=544611aab98d4df8bd045f3b0ab794bf;i=ba7357;b=dcae62f433304b8290b9372b7bdcde6c;m=2543c48021;t=64bbd3beb2870;x=27e3523fa56eaf34
                // Journal reception wallclock timestamp (CLOCK_REALTIME), in microseconds since epoch.
                case "__REALTIME_TIMESTAMP" ->
                        realtimeTimestampMicros = extractNumber(fieldValue); // example: 1772126101554715
                // Journal reception monotonic timestamp (CLOCK_MONOTONIC), in microseconds.
                //case "__MONOTONIC_TIMESTAMP" -> {
                // TODO no direct GIM mapping yet.
                // result.put("gl2_collector_journald_monotonic_timestamp", fieldValue); // example: 160050741281
                //}
                // Sequence number of this entry in the source journal file.
                //case "__SEQNUM" ->
                //        putNumericAsStringIfPresent(result, "event_id", extractNumber(fieldValue)); // example: 12219223
                // Sequence number ID associated with __SEQNUM.
                case "__SEQNUM_ID" ->
                        putIfPresent(result, VendorFields.VENDOR_TRANSACTION_ID, extractString(fieldValue)); // example: 544611aab98d4df8bd045f3b0ab794bf
            }
        }

        // Explicit precedence mappings
        putByPrecedence(result, ServiceFields.SERVICE_NAME, systemdUnit, systemdUserUnit, syslogIdentifier);
        putNumericByPrecedenceAsString(result, ProcessFields.PROCESS_ID, pid, syslogPid);
        putNumericByPrecedenceAsString(result, AssociatedFields.ASSOCIATED_USER_ID, auditLoginUid, systemdOwnerUid);
        putByPrecedence(result, UserFields.USER_SESSION_ID, toStringValue(auditSessionId), systemdSessionId);
        putByPrecedence(result, AssociatedFields.ASSOCIATED_SESSION_ID, bootId, streamId);
        putByPrecedence(result, "host_device", udevSysname, kernelDevice);
        putDateTimeByPrecedence(result, "vendor_event_timestamp",
                dateTimeFromEpochMicros(sourceRealtimeTimestampMicros),
                dateTimeFromEpochMicros(realtimeTimestampMicros),
                syslogTimestamp);

        return result;
    }

    private static String extractString(AnyValue value) {
        if (value.getValueCase() != AnyValue.ValueCase.STRING_VALUE) {
            return null;
        }

        final var stringValue = value.getStringValue();
        if (stringValue.isEmpty()) {
            return null;
        }

        return stringValue;
    }

    private static String extractMessage(AnyValue value) {
        final String rawMessage;
        switch (value.getValueCase()) {
            case STRING_VALUE -> rawMessage = extractString(value);
            case ARRAY_VALUE -> rawMessage = extractArrayMessage(value);
            default -> rawMessage = null;
        }

        if (rawMessage == null) {
            return null;
        }

        final var stripped = ANSI_ESCAPE_PATTERN.matcher(rawMessage).replaceAll("");
        if (stripped.isEmpty() || !isText(stripped)) {
            return null;
        }

        return stripped;
    }

    private static String extractArrayMessage(AnyValue value) {
        final var values = value.getArrayValue().getValuesList();
        if (values.isEmpty()) {
            return null;
        }

        final byte[] bytes = new byte[values.size()];
        for (int i = 0; i < values.size(); i++) {
            final var byteValue = extractByteValue(values.get(i));
            if (byteValue == null) {
                return null;
            }
            bytes[i] = byteValue;
        }

        return decodeUtf8(bytes);
    }

    private static Byte extractByteValue(AnyValue value) {
        final long numericValue;
        switch (value.getValueCase()) {
            case INT_VALUE -> numericValue = value.getIntValue();
            case DOUBLE_VALUE -> {
                final double d = value.getDoubleValue();
                if (!Double.isFinite(d) || d != Math.rint(d)) {
                    return null;
                }
                numericValue = (long) d;
            }
            default -> {
                return null;
            }
        }

        if (numericValue < 0 || numericValue > 255) {
            return null;
        }

        return (byte) numericValue;
    }

    private static Long extractPriority(AnyValue value) {
        final var priority = extractNumber(value);
        if (priority == null) {
            return null;
        }

        return priority >= 0 && priority <= 7 ? priority : null;
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            final var decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ignored) {
            return null;
        }
    }

    private static boolean isText(String value) {
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                return false;
            }
        }
        return true;
    }

    private static Long extractNumber(AnyValue value) {
        if (value.getValueCase() == AnyValue.ValueCase.INT_VALUE) {
            return value.getIntValue();
        }

        if (value.getValueCase() != AnyValue.ValueCase.STRING_VALUE) {
            return null;
        }

        final var stringValue = value.getStringValue();
        if (stringValue.isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(stringValue);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void putIfPresent(Map<String, Object> target, String fieldName, String value) {
        if (value != null && !value.isEmpty()) {
            target.put(fieldName, value);
        }
    }

    private static void putNumericIfPresent(Map<String, Object> target, String fieldName, Long value) {
        if (value != null) {
            target.put(fieldName, value);
        }
    }

    private static void putNumericAsStringIfPresent(Map<String, Object> target, String fieldName, Long value) {
        if (value != null) {
            target.put(fieldName, value.toString());
        }
    }

    private static void putByPrecedence(Map<String, Object> target, String fieldName, String... candidates) {
        for (final var value : candidates) {
            if (value != null && !value.isEmpty()) {
                target.put(fieldName, value);
                return;
            }
        }
    }

    private static void putNumericByPrecedenceAsString(Map<String, Object> target, String fieldName, Long... candidates) {
        for (final var value : candidates) {
            if (value != null) {
                target.put(fieldName, value.toString());
                return;
            }
        }
    }

    private static void putDateTimeByPrecedence(Map<String, Object> target, String fieldName, DateTime... candidates) {
        for (final var value : candidates) {
            if (value != null) {
                target.put(fieldName, Tools.buildElasticSearchTimeFormat(value));
                return;
            }
        }
    }

    private static DateTime dateTimeFromEpochMicros(Long micros) {
        if (micros == null) {
            return null;
        }

        return new DateTime(micros / 1_000L, DateTimeZone.UTC);
    }

    private static DateTime formatSyslogTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        // SYSLOG_TIMESTAMP omits year and timezone. Assume UTC and infer year from current UTC date.
        final var normalized = WHITESPACE_PATTERN.matcher(value.trim()).replaceAll(" ");
        final LocalDateTime parsedDateTime;
        try {
            parsedDateTime = LocalDateTime.parse(normalized, SYSLOG_TIMESTAMP_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }

        final var now = ZonedDateTime.now(ZoneOffset.UTC);
        final ZonedDateTime candidateWithCurrentYear;
        try {
            candidateWithCurrentYear = parsedDateTime.withYear(now.getYear()).atZone(ZoneOffset.UTC);
        } catch (DateTimeException ignored) {
            return null;
        }

        var candidate = candidateWithCurrentYear;
        if (candidate.isAfter(now.plusDays(1))) {
            candidate = candidate.minusYears(1);
        }
        return new DateTime(candidate.toInstant().toEpochMilli(), DateTimeZone.UTC);
    }

    private static String toStringValue(Long value) {
        return value == null ? null : value.toString();
    }
}
