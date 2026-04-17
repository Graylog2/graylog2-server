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

import com.google.common.net.InetAddresses;
import io.opentelemetry.proto.common.v1.AnyValue;
import jakarta.inject.Inject;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog.inputs.otel.codec.OTelTypeConverter;
import org.graylog.schema.DestinationFields;
import org.graylog.schema.EventFields;
import org.graylog.schema.HostFields;
import org.graylog.schema.ProcessFields;
import org.graylog.schema.SourceFields;
import org.graylog.schema.TraceFields;
import org.graylog.schema.VendorFields;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes Windows Event Log receiver messages into GIM format.
 *
 * <p>Handles all Windows Event Log channels (Security, System, Application, etc.).
 * System-level fields (event_code, host, channel, provider, etc.) are extracted for all events.
 * Unambiguous EventData fields (process, network source, destination hostname, privileges,
 * logon type, auth packages, error status) are mapped to GIM fields.
 *
 * <p>Subject/Target user-context fields ({@code SubjectUserSid}, {@code SubjectUserName},
 * {@code TargetUserSid}, {@code TargetUserName}, etc.) are passed through with their raw
 * MS XML attribute names and left for Illuminate pipelines to promote into
 * {@code user_*} / {@code source_user_*} / {@code target_user_*} per event ID. Their
 * semantics vary by event (e.g. 4624 Subject is a service requester while Target is the
 * human logging on; 4656 Subject is the acting user with no Target). Committing to a
 * single interpretation in the collector would either be wrong for some events or force
 * a breaking change once corrected — so we defer the decision to the per-event-aware layer.
 *
 * <p>All EventData/UserData entries are additionally serialized into {@code vendor_event_data}
 * as a JSON blob to preserve the complete original structure for downstream processing.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/windowseventlogreceiver">windowseventlog receiver</a>
 * @see <a href="https://learn.microsoft.com/en-us/windows/win32/wes/eventschema-systempropertiestype-complextype">SystemPropertiesType</a>
 */
public class WindowsEventLogRecordProcessor implements LogRecordProcessor {
    private static final String VENDOR_EVENT_CATEGORY = "vendor_event_category";
    private static final String VENDOR_OPCODE = "vendor_opcode";
    private static final String PRIVILEGE_ASSIGNED_NAME = "privilege_assigned_name";
    private static final String WINDOWS_LOGON_TYPE = "windows_logon_type";
    private static final String WINDOWS_AUTH_PACKAGE_NAME = "windows_authentication_package_name";
    private static final String WINDOWS_AUTH_PROCESS_NAME = "windows_authentication_process_name";
    private static final String WINDOWS_AUTH_LM_PACKAGE_NAME = "windows_authentication_lmpackage_name";
    private static final String VENDOR_EVENT_DATA = "vendor_event_data";
    private final OTelTypeConverter typeConverter;

    @Inject
    public WindowsEventLogRecordProcessor(OTelTypeConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    @Override
    public Map<String, Object> process(OTelJournal.Log log) {
        final Map<String, Object> result = new HashMap<>();
        final var logRecord = log.getLogRecord();

        if (logRecord.getBody().getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return result;
        }

        final var fields = new ExtractedFields();
        for (final var bodyField : logRecord.getBody().getKvlistValue().getValuesList()) {
            final var bodyFieldValue = bodyField.getValue();

            switch (bodyField.getKey()) {
                // Rendered message text from event rendering info.
                case "message" -> putIfPresent(result, Message.FIELD_MESSAGE, extractString(bodyFieldValue));
                // System/Computer: name of the computer on which the event occurred.
                case "computer" -> fields.computer = extractString(bodyFieldValue);
                // System/Channel: channel to which the event was logged (e.g., Security, Windows PowerShell).
                case "channel" -> putIfPresent(result, EventFields.EVENT_LOG_NAME, extractString(bodyFieldValue));
                // System/EventRecordID: channel-local record number assigned when logged.
                case "record_id" -> {
                    final var recordId = extractNumber(bodyFieldValue);
                    if (recordId != null) {
                        result.put(EventFields.EVENT_UID, recordId.toString());
                    }
                }
                // System/EventID (+ Qualifiers for legacy providers): provider-defined event identifier.
                case "event_id" -> extractEventId(bodyFieldValue, fields);
                // System/TimeCreated@SystemTime: event time in UTC.
                case "system_time" -> fields.vendorEventTimestamp = parseDateTime(extractString(bodyFieldValue));
                // System/Provider metadata: provider name, GUID, legacy source name.
                case "provider" -> extractProvider(bodyFieldValue, fields);
                // System/Task: provider-defined task/category identifier.
                case "task" -> putIfPresent(result, VENDOR_EVENT_CATEGORY, extractString(bodyFieldValue));
                // System/Opcode: provider-defined opcode describing event operation.
                case "opcode" -> putIfPresent(result, VENDOR_OPCODE, extractString(bodyFieldValue));
                // System/Version: version of the event definition.
                case "version" ->
                        putNumericAsStringIfPresent(result, VendorFields.VENDOR_VERSION, extractNumber(bodyFieldValue));
                // System/Correlation/ActivityID: activity correlation GUID for related events.
                case "correlation" -> extractCorrelation(bodyFieldValue, fields);
                // System/Execution: process/thread context that generated the event.
                case "execution" -> extractExecution(bodyFieldValue, fields);
                // EventData payload fields (provider-specific, often security auditing data names).
                case "event_data" -> extractEventData(bodyFieldValue, result, fields);
                // System/Security: user context recorded in System section (SID, name, domain, type).
                case "security" -> extractSecurity(bodyFieldValue, result);
                // System/Keywords outcome: rendered audit outcome (e.g. "success", "failure").
                case "outcome" -> putIfPresent(result, EventFields.EVENT_OUTCOME, extractString(bodyFieldValue));
                // UserData payload fields (provider-specific, alternative to EventData for some providers).
                case "user_data" -> extractUserData(bodyFieldValue, result, fields);
                // System/Level: rendered severity text (Information/Warning/Error/Critical/Verbose).
                case "level" -> putIfPresent(result, VendorFields.VENDOR_EVENT_SEVERITY, extractString(bodyFieldValue));
                // Unmapped but documented:
                // case "keywords" -> System/Keywords rendered keywords bitmask/tags.
            }
        }

        putByPrecedence(result, HostFields.HOST_HOSTNAME, fields.computer);
        putByPrecedence(result, EventFields.EVENT_SOURCE, fields.computer);

        if (fields.eventCode != null) {
            result.put(EventFields.EVENT_CODE, fields.eventCode);
        }

        if (fields.vendorEventTimestamp != null) {
            result.put(VendorFields.VENDOR_EVENT_TIMESTAMP, Tools.buildElasticSearchTimeFormat(fields.vendorEventTimestamp));
        }

        putByPrecedence(result, VendorFields.VENDOR_SUBTYPE, fields.providerName);
        putByPrecedence(result, TraceFields.TRACE_ID, fields.activityId);

        putNumericByPrecedenceAsString(result, ProcessFields.PROCESS_ID,
                fields.newProcessId, fields.processId, fields.callerProcessId, fields.executionProcessId);
        putByPrecedence(result, ProcessFields.PROCESS_PATH,
                fields.newProcessPath, fields.processPath, fields.callerProcessPath);
        putByPrecedence(result, ProcessFields.PROCESS_NAME,
                fileName(fields.newProcessPath), fileName(fields.processPath), fileName(fields.callerProcessPath));
        putByPrecedence(result, ProcessFields.PROCESS_PARENT_PATH, fields.parentProcessPath);
        putByPrecedence(result, ProcessFields.PROCESS_PARENT_NAME, fileName(fields.parentProcessPath));

        putByPrecedence(result, SourceFields.SOURCE_HOSTNAME, fields.workstationName, fields.workstation);
        putIfPresent(result, SourceFields.SOURCE_IP, validIp(fields.ipAddress));
        if (fields.ipPort != null) {
            result.put(SourceFields.SOURCE_PORT, fields.ipPort);
        }

        putByPrecedence(result, DestinationFields.DESTINATION_HOSTNAME, fields.targetServerName);

        final var privilegeValues = toMultivalue(fields.privilegeList);
        if (!privilegeValues.isEmpty()) {
            result.put(PRIVILEGE_ASSIGNED_NAME, privilegeValues);
        }

        putIfPresent(result, WINDOWS_LOGON_TYPE, fields.logonType);
        putIfPresent(result, WINDOWS_AUTH_PACKAGE_NAME, fields.authPackageName);
        putIfPresent(result, WINDOWS_AUTH_PROCESS_NAME, fields.authProcessName);
        putIfPresent(result, WINDOWS_AUTH_LM_PACKAGE_NAME, fields.authLmPackageName);
        putIfPresent(result, EventFields.EVENT_ERROR_CODE, fields.status);
        putIfPresent(result, EventFields.EVENT_ERROR_DESCRIPTION, fields.failureReason);
        putIfPresent(result, ProcessFields.PROCESS_COMMAND_LINE, fields.commandLine);

        putIfPresent(result, VENDOR_EVENT_DATA, fields.vendorEventDataJson);

        return result;
    }

    private static void extractEventId(AnyValue value, ExtractedFields fields) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                case "id" -> fields.eventCode = extractNumber(kv.getValue());
            }
        }
    }

    private static void extractProvider(AnyValue value, ExtractedFields fields) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                case "name" -> fields.providerName = extractString(kv.getValue());
            }
        }
    }

    private static void extractCorrelation(AnyValue value, ExtractedFields fields) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                case "activity_id" -> fields.activityId = extractString(kv.getValue());
            }
        }
    }

    private static void extractExecution(AnyValue value, ExtractedFields fields) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                case "process_id" -> fields.executionProcessId = extractFlexibleNumber(kv.getValue());
            }
        }
    }

    private static void extractSecurity(AnyValue value, Map<String, Object> result) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                // System/Security UserType: rendered account type (e.g. WellKnownGroup, User).
                case "user_type" -> putIfPresent(result, VendorFields.VENDOR_USER_TYPE, extractString(kv.getValue()));
            }
        }
    }

    private void extractEventData(AnyValue value, Map<String, Object> result, ExtractedFields fields) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        // Serialize ALL event_data to JSON blob (preserves full original structure)
        fields.vendorEventDataJson = typeConverter.toJson(typeConverter.toJavaMap(value.getKvlistValue()), "event_data").orElse("");

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                // Subject/Target user-context fields: pass through raw. Event-ID-conditional
                // semantics are resolved by Illuminate per-event rules.
                case "SubjectUserSid", "SubjectUserName", "SubjectDomainName", "SubjectLogonId",
                     "TargetUserSid", "TargetUserName", "TargetDomainName", "TargetLogonId",
                     "TargetAccount" ->
                        putIfPresent(result, kv.getKey(), extractString(kv.getValue()));
                // 4624/4648 logon GUID correlation value. Event-ID-conditional target
                // (user_session_uid vs source_user_session_uid), so pass through raw.
                case "LogonGuid" -> putIfPresent(result, kv.getKey(), extractString(kv.getValue()));
                // 4625 NTSTATUS sub-code for failure detail. Illuminate consumes raw to
                // override event_error_code when more specific than Status.
                case "SubStatus" -> putIfPresent(result, kv.getKey(), extractString(kv.getValue()));
                // 4624/4648 Process Information: process ID as pointer/hex or decimal.
                case "ProcessId" -> fields.processId = extractFlexibleNumber(kv.getValue());
                // 4624/4648/4798 Process Information: full process executable path.
                case "ProcessName" -> fields.processPath = extractString(kv.getValue());
                // 4798 Caller process ID that performed group-enumeration operation.
                case "CallerProcessId" -> fields.callerProcessId = extractFlexibleNumber(kv.getValue());
                // 4798 Caller process executable path.
                case "CallerProcessName" -> fields.callerProcessPath = extractString(kv.getValue());
                // 4624/4648 Network Information: source network address.
                case "IpAddress" -> fields.ipAddress = extractString(kv.getValue());
                // 4624/4648 Network Information: source port for the logon attempt.
                case "IpPort" -> fields.ipPort = extractIntInRange(kv.getValue(), 0, 65535);
                // 4624 Network Information: source workstation name.
                case "WorkstationName" -> fields.workstationName = extractString(kv.getValue());
                // 4776 Source Workstation: origin workstation for NTLM validation attempt.
                case "Workstation" -> fields.workstation = extractString(kv.getValue());
                // 4648 Target Server: server where process was run using explicit credentials.
                case "TargetServerName" -> fields.targetServerName = extractString(kv.getValue());
                // 4672 Privileges: sensitive privileges assigned to the new logon session.
                case "PrivilegeList" -> fields.privilegeList = extractString(kv.getValue());
                // 4688 Process Creation: new process executable path.
                case "NewProcessName" -> fields.newProcessPath = extractString(kv.getValue());
                // 4688 Process Creation: new process ID as hex or decimal.
                case "NewProcessId" -> fields.newProcessId = extractFlexibleNumber(kv.getValue());
                // 4688 Process Creation: parent process executable path.
                case "ParentProcessName" -> fields.parentProcessPath = extractString(kv.getValue());
                // 4624/4634 logon type code.
                case "LogonType" -> fields.logonType = extractString(kv.getValue());
                // 4624 authentication package name.
                case "AuthenticationPackageName" -> fields.authPackageName = extractString(kv.getValue());
                // 4624 trusted logon process name.
                case "LogonProcessName" -> fields.authProcessName = extractString(kv.getValue());
                // 4624 NTLM package subtype.
                case "LmPackageName" -> fields.authLmPackageName = extractString(kv.getValue());
                // 4625/4776 validation status/error code.
                case "Status" -> fields.status = extractString(kv.getValue());
                // 4625 human-readable failure description.
                case "FailureReason" -> fields.failureReason = extractString(kv.getValue());
                // 4688 command line (when audit policy enabled).
                case "CommandLine" -> fields.commandLine = extractString(kv.getValue());
            }
        }
    }

    private void extractUserData(AnyValue value, Map<String, Object> result, ExtractedFields fields) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        // Serialize user_data to vendor_event_data (same field as event_data; they're mutually exclusive)
        fields.vendorEventDataJson = typeConverter.toJson(typeConverter.toJavaMap(value.getKvlistValue()), "user_data").orElse("");

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                case "SubjectUserName", "SubjectDomainName", "SubjectUserSid", "SubjectLogonId" ->
                        putIfAbsent(result, kv.getKey(), extractString(kv.getValue()));
            }
        }
    }

    private static String extractString(AnyValue value) {
        if (value.getValueCase() != AnyValue.ValueCase.STRING_VALUE) {
            return null;
        }

        final var stringValue = value.getStringValue();
        if (stringValue.isEmpty() || "-".equals(stringValue)) {
            return null;
        }

        return stringValue;
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

    private static Long extractFlexibleNumber(AnyValue value) {
        if (value.getValueCase() == AnyValue.ValueCase.INT_VALUE) {
            return value.getIntValue();
        }

        if (value.getValueCase() != AnyValue.ValueCase.STRING_VALUE) {
            return null;
        }

        final var stringValue = value.getStringValue();
        if (stringValue.isEmpty() || "-".equals(stringValue)) {
            return null;
        }

        try {
            if (stringValue.startsWith("0x") || stringValue.startsWith("0X")) {
                return Long.parseLong(stringValue.substring(2), 16);
            }
            return Long.parseLong(stringValue);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer extractIntInRange(AnyValue value, int min, int max) {
        final var number = extractFlexibleNumber(value);
        if (number == null || number < min || number > max) {
            return null;
        }
        return number.intValue();
    }

    private static DateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        try {
            return new DateTime(Instant.parse(value).toEpochMilli(), DateTimeZone.UTC);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String validIp(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return InetAddresses.isInetAddress(value) ? value : null;
    }

    private static String fileName(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        final int slash = path.lastIndexOf('/');
        final int backslash = path.lastIndexOf('\\');
        final int idx = Math.max(slash, backslash);

        if (idx < 0 || idx == path.length() - 1) {
            return path;
        }
        return path.substring(idx + 1);
    }

    private static List<String> toMultivalue(String value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }

        final List<String> values = new ArrayList<>();
        for (final var line : value.split("\\r?\\n")) {
            final var trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }

        return values;
    }

    private static void putIfPresent(Map<String, Object> target, String fieldName, String value) {
        if (value != null && !value.isEmpty()) {
            target.put(fieldName, value);
        }
    }

    private static void putIfAbsent(Map<String, Object> target, String fieldName, String value) {
        if (value != null && !value.isEmpty() && !target.containsKey(fieldName)) {
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

    private static class ExtractedFields {
        private String computer;
        private Long eventCode;
        private DateTime vendorEventTimestamp;
        private String providerName;
        private String activityId;
        private Long executionProcessId;

        private Long processId;
        private String processPath;
        private Long newProcessId;
        private String newProcessPath;
        private String parentProcessPath;
        private Long callerProcessId;
        private String callerProcessPath;

        private String ipAddress;
        private Integer ipPort;
        private String workstationName;
        private String workstation;
        private String targetServerName;
        private String privilegeList;

        private String logonType;
        private String authPackageName;
        private String authProcessName;
        private String authLmPackageName;
        private String status;
        private String failureReason;
        private String commandLine;

        private String vendorEventDataJson;
    }
}
