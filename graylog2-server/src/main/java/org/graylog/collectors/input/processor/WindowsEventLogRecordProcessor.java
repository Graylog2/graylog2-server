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
import io.opentelemetry.proto.logs.v1.LogRecord;
import org.graylog.schema.AssociatedFields;
import org.graylog.schema.DestinationFields;
import org.graylog.schema.EventFields;
import org.graylog.schema.HostFields;
import org.graylog.schema.ProcessFields;
import org.graylog.schema.SourceFields;
import org.graylog.schema.TraceFields;
import org.graylog.schema.UserFields;
import org.graylog.schema.VendorFields;
import org.graylog2.plugin.Message;
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
 * Field semantics are based on Windows Event Log schema and Microsoft security-auditing event documentation:
 * - System fields: SystemPropertiesType (Provider, EventID, Version, Level, Task, Opcode, Keywords,
 * TimeCreated/SystemTime, EventRecordID, Correlation/ActivityID, Execution, Channel, Computer, Security/UserID)
 * - Security event field references used here: 4624, 4634, 4648, 4672, 4688, 4717, 4718, 4740, 4776, 4798
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/windowseventlogreceiver">windowseventlog receiver</a>
 * @see <a href="https://learn.microsoft.com/en-us/windows/win32/wes/eventschema-systempropertiestype-complextype">SystemPropertiesType</a>
 */
public class WindowsEventLogRecordProcessor implements LogRecordProcessor {
    private static final String EVENT_ID = "event_id";
    private static final String VENDOR_EVENT_CATEGORY = "vendor_event_category";
    private static final String VENDOR_EVENT_TIMESTAMP = "vendor_event_timestamp";
    private static final String PRIVILEGE_ASSIGNED_NAME = "privilege_assigned_name";

    @Override
    public Map<String, Object> process(LogRecord logRecord) {
        final Map<String, Object> result = new HashMap<>();

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
                case "opcode" -> putIfPresent(result, VendorFields.VENDOR_EVENT_ACTION, extractString(bodyFieldValue));
                // System/Version: version of the event definition.
                case "version" ->
                        putNumericAsStringIfPresent(result, VendorFields.VENDOR_VERSION, extractNumber(bodyFieldValue));
                // System/Correlation/ActivityID: activity correlation GUID for related events.
                case "correlation" -> extractCorrelation(bodyFieldValue, fields);
                // System/Execution: process/thread context that generated the event.
                case "execution" -> extractExecution(bodyFieldValue, fields);
                // EventData payload fields (provider-specific, often security auditing data names).
                case "event_data" -> extractEventData(bodyFieldValue, fields);
                // Rendered Details section(s) produced by the receiver from message templates.
                case "details" -> extractDetails(bodyFieldValue);
                // System/Security: user context recorded in System section (SID, name, domain, type).
                case "security" -> extractSecurity(bodyFieldValue, fields);
                // System/Keywords outcome: rendered audit outcome (e.g. "success", "failure").
                case "outcome" -> putIfPresent(result, EventFields.EVENT_OUTCOME, extractString(bodyFieldValue));
                // UserData payload fields (provider-specific, alternative to EventData for some providers).
                case "user_data" -> extractUserData(bodyFieldValue, fields);
                // Unmapped but documented:
                // case "level" -> rendered level text (Information/Warning/Error).
                // case "keywords" -> System/Keywords rendered keywords bitmask/tags.
            }
        }

        putByPrecedence(result, HostFields.HOST_HOSTNAME, fields.computer);
        putByPrecedence(result, EventFields.EVENT_SOURCE, fields.computer);

        if (fields.eventCode != null) {
            result.put(EventFields.EVENT_CODE, fields.eventCode);
            result.put(EVENT_ID, fields.eventCode.toString());
        }

        if (fields.vendorEventTimestamp != null) {
            result.put(VENDOR_EVENT_TIMESTAMP, fields.vendorEventTimestamp);
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

        putByPrecedence(result, UserFields.USER_ID, fields.targetUserSid);
        putByPrecedence(result, UserFields.USER_NAME, fields.targetUserName);
        putByPrecedence(result, UserFields.USER_DOMAIN, fields.targetDomainName);
        putByPrecedence(result, UserFields.USER_SESSION_ID, fields.targetLogonId);

        putByPrecedence(result, AssociatedFields.ASSOCIATED_USER_ID, fields.subjectUserSid, fields.securityUserId);
        putByPrecedence(result, AssociatedFields.ASSOCIATED_USER_NAME, fields.subjectUserName, fields.securityUserName);
        // Unmapped: associated_user_domain (not in GIM) - sources: subjectDomainName, securityDomain
        putByPrecedence(result, AssociatedFields.ASSOCIATED_SESSION_ID, fields.subjectLogonId);

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

        return result;
    }

    private static void extractEventId(AnyValue value, ExtractedFields fields) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                case "id" -> fields.eventCode = extractNumber(kv.getValue());
                // Unmapped but documented:
                // case "qualifiers" -> EventID/Qualifiers high-order bits for classic providers.
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
                // Unmapped but documented:
                // case "guid" -> System/Provider@Guid.
                // case "event_source" -> System/Provider@EventSourceName.
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
                // Unmapped but documented:
                // case "thread_id" -> System/Execution@ThreadID.
            }
        }
    }

    private static void extractSecurity(AnyValue value, ExtractedFields fields) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                case "user_id" -> fields.securityUserId = extractString(kv.getValue());
                case "user_name" -> fields.securityUserName = extractString(kv.getValue());
                case "domain" -> fields.securityDomain = extractString(kv.getValue());
                // Unmapped but documented:
                // case "user_type" -> System/Security user type descriptor.
            }
        }
    }

    private static void extractEventData(AnyValue value, ExtractedFields fields) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                // 4624/4648/4672/4798 Subject: SID of account that requested/performed the action.
                case "SubjectUserSid" -> fields.subjectUserSid = extractString(kv.getValue());
                // 4624/4648/4672/4798 Subject: account name.
                case "SubjectUserName" -> fields.subjectUserName = extractString(kv.getValue());
                // 4624/4648/4672/4688/4798 Subject: account domain/computer name.
                case "SubjectDomainName" -> fields.subjectDomainName = extractString(kv.getValue());
                // 4624/4648/4672/4798 Subject: logon session ID (hex string).
                case "SubjectLogonId" -> fields.subjectLogonId = extractString(kv.getValue());
                // 4624/4634 Target/New Logon: SID of account logged on/logged off.
                case "TargetUserSid" -> fields.targetUserSid = extractString(kv.getValue());
                // 4624/4634/4648/4776/4798 Target/User/Logon Account names.
                case "TargetUserName" -> fields.targetUserName = extractString(kv.getValue());
                // 4624/4634/4648/4798 target account domain/computer context.
                case "TargetDomainName" -> fields.targetDomainName = extractString(kv.getValue());
                // 4624/4634 Target logon session ID for correlation.
                case "TargetLogonId" -> fields.targetLogonId = extractString(kv.getValue());
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
                // 4740 Account Lockout: target account name.
                case "TargetAccount" -> {
                    if (fields.targetUserName == null) {
                        fields.targetUserName = extractString(kv.getValue());
                    }
                }
                // Unmapped but documented:
                // case "process" -> OpenSSH EventData process marker.
                // case "payload" -> OpenSSH EventData payload detail text.
                // case "LogonType" -> 4624/4634 logon type code.
                // case "LogonGuid" -> 4624/4648 logon GUID correlation value.
                // case "AuthenticationPackageName" -> 4624 auth package.
                // case "LogonProcessName" -> 4624 trusted logon process name.
                // case "LmPackageName" -> 4624 NTLM package subtype.
                // case "TransmittedServices" -> 4624 Kerberos transited services.
                // case "KeyLength" -> 4624 key length.
                // case "ImpersonationLevel" -> 4624 impersonation level.
                // case "RestrictedAdminMode" -> 4624 v2 restricted admin flag.
                // case "RemoteCredentialGuard" -> 4624 v2 remote credential guard flag.
                // case "TargetOutboundUserName" -> 4624 v2 outbound username.
                // case "TargetOutboundDomainName" -> 4624 v2 outbound domain.
                // case "VirtualAccount" -> 4624 v2 virtual account flag.
                // case "TargetLinkedLogonId" -> 4624 v2 linked logon ID.
                // case "ElevatedToken" -> 4624 v2 elevated token flag.
                // case "TargetInfo" -> 4648 target server additional info.
                // case "TargetLogonGuid" -> 4648 target logon GUID.
                // case "TargetSid" -> 4717/4718/4798 target SID.
                // case "PackageName" -> 4776 authentication package.
                // case "Status" -> 4776 validation status/error code.
                // case "AccessGranted" -> 4717 granted logon right.
                // case "AccessRemoved" -> 4718 removed logon right.
                // case "MandatoryLabel" -> 4688 mandatory integrity label SID.
                // case "TokenElevationType" -> 4688 token elevation type code.
                // case "CommandLine" -> 4688 command line (when audit policy enabled).
                // case "FailureReason" -> 4625 human-readable failure description.
                // case "SubStatus" -> 4625 NTSTATUS sub-code for failure detail.
                // case "" -> unnamed positional EventData entries (common in PowerShell events).
            }
        }
    }

    private static void extractUserData(AnyValue value, ExtractedFields fields) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                // 104 EventLog-cleared: subject account name.
                case "SubjectUserName" -> {
                    if (fields.subjectUserName == null) {
                        fields.subjectUserName = extractString(kv.getValue());
                    }
                }
                // 104 EventLog-cleared: subject account domain.
                case "SubjectDomainName" -> {
                    if (fields.subjectDomainName == null) {
                        fields.subjectDomainName = extractString(kv.getValue());
                    }
                }
                // Unmapped but documented:
                // case "Channel" -> 104 cleared channel name.
                // case "ClientProcessId" -> 104 client process ID that cleared the log.
                // case "ClientProcessStartKey" -> 104 client process start key.
                // case "xml_name" -> UserData XML element name.
                // case "param1"/"param2" -> 1000/1001 positional application error parameters.
                // case "binaryDataSize" -> 1000/1001 binary data size.
                // case "binaryData" -> 1000/1001 binary crash data.
                // case "RmSessionId" -> 10000/10001 restart manager session ID.
                // case "UTCStartTime" -> 10000/10001 restart manager UTC start time.
            }
        }
    }

    private static void extractDetails(AnyValue value) {
        if (value.getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return;
        }

        for (final var kv : value.getKvlistValue().getValuesList()) {
            switch (kv.getKey()) {
                // Unmapped but documented:
                // case "Subject" -> rendered Subject account details section.
                // case "Additional Context" -> rendered explanatory/context section.
                // case "Logon Type" -> rendered logon type summary section.
                // case "Process Information" -> rendered process info section.
                // case "Network Information" -> rendered network source section.
                // case "Detailed Authentication Information" -> rendered auth details section.
                // case "New Logon" -> rendered new logon identity/session section.
                // case "Impersonation Level" -> rendered impersonation level section.
                // case "Logon Information" -> rendered logon flags section.
                // case "Account Whose Credentials Were Used" -> 4648 rendered target account section.
                // case "Target Server" -> 4648 rendered target server section.
                // case "Privileges" -> 4672 rendered privilege list section.
                // case "User" -> 4798 rendered user section.
                // case "Authentication Package" -> 4776 rendered authentication package section.
                // case "Error Code" -> 4776 rendered error/status section.
                // case "Logon Account" -> 4776 rendered logon account section.
                // case "Source Workstation" -> 4776 rendered source workstation section.
                // case "Account Modified" -> 4717/4718 rendered modified account section.
                // case "Access Granted" -> 4717 rendered granted right section.
                // case "Access Removed" -> 4718 rendered removed right section.
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
        private String securityUserId;
        private String securityUserName;
        private String securityDomain;

        private Long processId;
        private String processPath;
        private Long newProcessId;
        private String newProcessPath;
        private String parentProcessPath;
        private Long callerProcessId;
        private String callerProcessPath;

        private String targetUserSid;
        private String targetUserName;
        private String targetDomainName;
        private String targetLogonId;

        private String subjectUserSid;
        private String subjectUserName;
        private String subjectDomainName;
        private String subjectLogonId;

        private String ipAddress;
        private Integer ipPort;
        private String workstationName;
        private String workstation;
        private String targetServerName;
        private String privilegeList;
    }
}
