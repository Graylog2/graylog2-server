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

import com.google.common.io.Resources;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import org.graylog.inputs.otel.OTelJournal;
import org.graylog.schema.AssociatedFields;
import org.graylog.schema.DestinationFields;
import org.graylog.schema.EventFields;
import org.graylog.schema.HostFields;
import org.graylog.schema.ProcessFields;
import org.graylog.schema.SourceFields;
import org.graylog.schema.TraceFields;
import org.graylog.schema.UserFields;
import org.graylog.schema.VendorFields;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WindowsEventLogRecordProcessorTest {
    private static final String EVENT_ID = "event_id";
    private static final String VENDOR_EVENT_CATEGORY = "vendor_event_category";
    private static final String VENDOR_EVENT_TIMESTAMP = "vendor_event_timestamp";
    private static final String PRIVILEGE_ASSIGNED_NAME = "privilege_assigned_name";

    private final WindowsEventLogRecordProcessor processor = new WindowsEventLogRecordProcessor();

    @Test
    void maps4624FromFixture() throws IOException {
        final var logRecord = fixtureRecordByRecordId(140716L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry("message", "An account was successfully logged on.")
                .containsEntry(HostFields.HOST_HOSTNAME, "winhost01")
                .containsEntry(EventFields.EVENT_SOURCE, "winhost01")
                .containsEntry(EventFields.EVENT_LOG_NAME, "Security")
                .containsEntry(EventFields.EVENT_UID, "140716")
                .containsEntry(EventFields.EVENT_CODE, 4624L)
                .containsEntry(EVENT_ID, "4624")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Security-Auditing")
                .containsEntry(VENDOR_EVENT_CATEGORY, "Logoff")
                .containsEntry(VendorFields.VENDOR_EVENT_ACTION, "Info")
                .containsEntry(VendorFields.VENDOR_VERSION, "3")
                .containsEntry(TraceFields.TRACE_ID, "{11111111-2222-3333-4444-555555555555}")
                .containsEntry(VENDOR_EVENT_TIMESTAMP, "2026-02-28 09:16:32.772")
                .containsEntry(ProcessFields.PROCESS_ID, "5860")
                .containsEntry(ProcessFields.PROCESS_PATH, "C:\\Windows\\System32\\OpenSSH\\sshd.exe")
                .containsEntry(ProcessFields.PROCESS_NAME, "sshd.exe")
                .containsEntry(UserFields.USER_ID, "S-1-5-21-1000000000-1000000000-1000000000-500")
                .containsEntry(UserFields.USER_NAME, "TestAdmin")
                .containsEntry(UserFields.USER_DOMAIN, "WINHOST01")
                .containsEntry(UserFields.USER_SESSION_ID, "0x25501f9")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_ID, "S-1-5-18")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_NAME, "WINHOST01$")
                .containsEntry(AssociatedFields.ASSOCIATED_SESSION_ID, "0x3e7");

        assertThat(result).doesNotContainKeys(
                SourceFields.SOURCE_IP,
                SourceFields.SOURCE_PORT,
                SourceFields.SOURCE_HOSTNAME,
                DestinationFields.DESTINATION_HOSTNAME
        );
    }

    @Test
    void maps4648FromFixture() throws IOException {
        final var logRecord = fixtureRecordByRecordId(140715L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry("message", "A logon was attempted using explicit credentials.")
                .containsEntry(EventFields.EVENT_CODE, 4648L)
                .containsEntry(EVENT_ID, "4648")
                .containsEntry(EventFields.EVENT_UID, "140715")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Security-Auditing")
                .containsEntry(ProcessFields.PROCESS_ID, "5860")
                .containsEntry(ProcessFields.PROCESS_PATH, "C:\\Windows\\System32\\OpenSSH\\sshd.exe")
                .containsEntry(DestinationFields.DESTINATION_HOSTNAME, "localhost")
                .containsEntry(UserFields.USER_NAME, "TestAdmin")
                .containsEntry(UserFields.USER_DOMAIN, "WINHOST01")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_ID, "S-1-5-18")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_NAME, "WINHOST01$")
                .containsEntry(AssociatedFields.ASSOCIATED_SESSION_ID, "0x3e7");

        assertThat(result).doesNotContainKeys(
                UserFields.USER_ID,
                UserFields.USER_SESSION_ID,
                SourceFields.SOURCE_IP
        );
    }

    @Test
    void maps4672PrivilegesFromFixture() throws IOException {
        final var logRecord = fixtureRecordByRecordId(140717L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry("message", "Special privileges assigned to new logon.")
                .containsEntry(EventFields.EVENT_CODE, 4672L)
                .containsEntry(EVENT_ID, "4672")
                .containsEntry(PRIVILEGE_ASSIGNED_NAME, List.of(
                        "SeSecurityPrivilege",
                        "SeTakeOwnershipPrivilege",
                        "SeLoadDriverPrivilege",
                        "SeBackupPrivilege",
                        "SeRestorePrivilege",
                        "SeDebugPrivilege",
                        "SeSystemEnvironmentPrivilege",
                        "SeImpersonatePrivilege",
                        "SeDelegateSessionUserImpersonatePrivilege"
                ))
                .containsEntry(AssociatedFields.ASSOCIATED_USER_ID, "S-1-5-21-1000000000-1000000000-1000000000-500")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_NAME, "TestAdmin")
                .containsEntry(AssociatedFields.ASSOCIATED_SESSION_ID, "0x25501f9");
    }

    @Test
    void mapsOpenSshFromFixture() throws IOException {
        final var logRecord = fixtureRecordByRecordId(2743L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry("message", "sshd: Received disconnect from 203.0.113.10 port 62064:11: disconnected by user")
                .containsEntry(HostFields.HOST_HOSTNAME, "winhost01")
                .containsEntry(EventFields.EVENT_SOURCE, "winhost01")
                .containsEntry(EventFields.EVENT_LOG_NAME, "OpenSSH/Operational")
                .containsEntry(EventFields.EVENT_UID, "2743")
                .containsEntry(EventFields.EVENT_CODE, 4L)
                .containsEntry(EVENT_ID, "4")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "OpenSSH")
                .containsEntry(ProcessFields.PROCESS_ID, "4160");

        assertThat(result).doesNotContainKeys(
                ProcessFields.PROCESS_PATH,
                SourceFields.SOURCE_IP,
                SourceFields.SOURCE_PORT
        );
    }

    @Test
    void handlesUnnamedPowershellEventDataFromFixture() throws IOException {
        final var logRecord = fixtureRecordByRecordId(8526L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry("message", "Provider \"Registry\" is Started. \r\n\r\nDetails: \r\n\tProviderName=Registry\r\n\tNewProviderState=Started\r\n\r\n\tSequenceNumber=1\r\n\r\n\tHostName=ConsoleHost\r\n\tHostVersion=5.1.26100.32370\r\n\tHostId=aaaaaaa2-bbbb-4ccc-8ddd-eeeeeeeeeee2\r\n\tHostApplication=c:\\windows\\system32\\windowspowershell\\v1.0\\powershell.exe\r\n\tEngineVersion=\r\n\tRunspaceId=\r\n\tPipelineId=\r\n\tCommandName=\r\n\tCommandType=\r\n\tScriptName=\r\n\tCommandPath=\r\n\tCommandLine=")
                .containsEntry(HostFields.HOST_HOSTNAME, "winhost01")
                .containsEntry(EventFields.EVENT_LOG_NAME, "Windows PowerShell")
                .containsEntry(EventFields.EVENT_UID, "8526")
                .containsEntry(EventFields.EVENT_CODE, 600L)
                .containsEntry(EVENT_ID, "600")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "PowerShell")
                .containsEntry(VENDOR_EVENT_CATEGORY, "Engine Lifecycle")
                .containsEntry(VendorFields.VENDOR_EVENT_ACTION, "Info");
    }

    @Test
    void maps4624WithOutcomeFromNewFormat() throws IOException {
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-2.ndjson", 227239L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(HostFields.HOST_HOSTNAME, "winserver01")
                .containsEntry(EventFields.EVENT_SOURCE, "winserver01")
                .containsEntry(EventFields.EVENT_LOG_NAME, "Security")
                .containsEntry(EventFields.EVENT_UID, "227239")
                .containsEntry(EventFields.EVENT_CODE, 4624L)
                .containsEntry(EVENT_ID, "4624")
                .containsEntry(EventFields.EVENT_OUTCOME, "success")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Security-Auditing")
                .containsEntry(VENDOR_EVENT_CATEGORY, "Logon")
                .containsEntry(TraceFields.TRACE_ID, "{11111111-aaaa-0001-bbbb-cccccccccccc}")
                .containsEntry(ProcessFields.PROCESS_ID, "592")
                .containsEntry(ProcessFields.PROCESS_PATH, "C:\\Windows\\System32\\services.exe")
                .containsEntry(ProcessFields.PROCESS_NAME, "services.exe")
                .containsEntry(UserFields.USER_ID, "S-1-5-18")
                .containsEntry(UserFields.USER_NAME, "SYSTEM")
                .containsEntry(UserFields.USER_DOMAIN, "NT AUTHORITY")
                .containsEntry(UserFields.USER_SESSION_ID, "0x3e7")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_ID, "S-1-5-18")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_NAME, "WINSERVER01$")
                .containsEntry(AssociatedFields.ASSOCIATED_SESSION_ID, "0x3e7");

        assertThat(result).doesNotContainKeys(
                SourceFields.SOURCE_IP,
                SourceFields.SOURCE_HOSTNAME
        );
    }

    @Test
    void maps4672PrivilegesFromNewFormat() throws IOException {
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-2.ndjson", 227240L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(EventFields.EVENT_CODE, 4672L)
                .containsEntry(EVENT_ID, "4672")
                .containsEntry(EventFields.EVENT_OUTCOME, "success")
                .containsEntry(PRIVILEGE_ASSIGNED_NAME, List.of(
                        "SeAssignPrimaryTokenPrivilege",
                        "SeTcbPrivilege",
                        "SeSecurityPrivilege",
                        "SeTakeOwnershipPrivilege",
                        "SeLoadDriverPrivilege",
                        "SeBackupPrivilege",
                        "SeRestorePrivilege",
                        "SeDebugPrivilege",
                        "SeAuditPrivilege",
                        "SeSystemEnvironmentPrivilege",
                        "SeImpersonatePrivilege",
                        "SeDelegateSessionUserImpersonatePrivilege"
                ))
                .containsEntry(AssociatedFields.ASSOCIATED_USER_ID, "S-1-5-18")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_NAME, "SYSTEM")
                .containsEntry(AssociatedFields.ASSOCIATED_SESSION_ID, "0x3e7");
    }

    @Test
    void maps4625FailedLogonFromNewFormat() throws IOException {
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-2.ndjson", 149486L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(HostFields.HOST_HOSTNAME, "winserver02")
                .containsEntry(EventFields.EVENT_SOURCE, "winserver02")
                .containsEntry(EventFields.EVENT_LOG_NAME, "Security")
                .containsEntry(EventFields.EVENT_CODE, 4625L)
                .containsEntry(EVENT_ID, "4625")
                .containsEntry(EventFields.EVENT_OUTCOME, "failure")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Security-Auditing")
                .containsEntry(UserFields.USER_ID, "S-1-0-0")
                .containsEntry(UserFields.USER_NAME, "USER")
                .containsEntry(SourceFields.SOURCE_IP, "198.51.100.42")
                .containsEntry(SourceFields.SOURCE_PORT, 0);

        assertThat(result).doesNotContainKeys(
                SourceFields.SOURCE_HOSTNAME,
                UserFields.USER_DOMAIN,
                UserFields.USER_SESSION_ID
        );
    }

    @Test
    void mapsSystemEventFromNewFormat() throws IOException {
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-2.ndjson", 187435L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(HostFields.HOST_HOSTNAME, "winserver01")
                .containsEntry(EventFields.EVENT_LOG_NAME, "System")
                .containsEntry(EventFields.EVENT_CODE, 7036L)
                .containsEntry(EVENT_ID, "7036")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Service Control Manager");

        assertThat(result).doesNotContainKeys(
                EventFields.EVENT_OUTCOME
        );
    }

    @Test
    void mapsSecurityUserNameAndDomain() throws IOException {
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-3.ndjson", 16521L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(HostFields.HOST_HOSTNAME, "winserver03")
                .containsEntry(EventFields.EVENT_LOG_NAME, "System")
                .containsEntry(EventFields.EVENT_CODE, 16977L)
                .containsEntry(EVENT_ID, "16977")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Directory-Services-SAM")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_ID, "S-1-5-18")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_NAME, "SYSTEM");
    }

    @Test
    void mapsUserDataSubjectFields() throws IOException {
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-3.ndjson", 16518L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(HostFields.HOST_HOSTNAME, "winserver03")
                .containsEntry(EventFields.EVENT_LOG_NAME, "System")
                .containsEntry(EventFields.EVENT_CODE, 104L)
                .containsEntry(EVENT_ID, "104")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Eventlog")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_ID, "S-1-5-21-1000000000-2000000000-3000000000-500")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_NAME, "Administrator");
    }

    @Test
    void maps4688ProcessCreation() throws IOException {
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-3.ndjson", 500001L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(HostFields.HOST_HOSTNAME, "testhost01")
                .containsEntry(EventFields.EVENT_LOG_NAME, "Security")
                .containsEntry(EventFields.EVENT_CODE, 4688L)
                .containsEntry(EVENT_ID, "4688")
                .containsEntry(EventFields.EVENT_OUTCOME, "success")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Security-Auditing")
                .containsEntry(ProcessFields.PROCESS_ID, "6700")
                .containsEntry(ProcessFields.PROCESS_PATH, "C:\\Windows\\System32\\cmd.exe")
                .containsEntry(ProcessFields.PROCESS_NAME, "cmd.exe")
                .containsEntry(ProcessFields.PROCESS_PARENT_PATH, "C:\\Windows\\System32\\services.exe")
                .containsEntry(ProcessFields.PROCESS_PARENT_NAME, "services.exe")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_ID, "S-1-5-18")
                .containsEntry(AssociatedFields.ASSOCIATED_USER_NAME, "TESTHOST01$")
                .containsEntry(AssociatedFields.ASSOCIATED_SESSION_ID, "0x3e7");
    }

    private static OTelJournal.Log wrapLogRecord(LogRecord logRecord) {
        return OTelJournal.Log.newBuilder().setLogRecord(logRecord).build();
    }

    private static LogRecord fixtureRecordByRecordId(long recordId) throws IOException {
        return fixtureRecordByRecordId("windows-2025-eventlog-1.ndjson", recordId);
    }

    private static LogRecord fixtureRecordByRecordId(String fixture, long recordId) throws IOException {
        return parseFixture(fixture).stream()
                .filter(logRecord -> topLevelLongValue(logRecord, "record_id") == recordId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No fixture record found for record_id=" + recordId));
    }

    private static long topLevelLongValue(LogRecord logRecord, String key) {
        final var value = topLevelValue(logRecord, key);
        if (value == null) {
            return -1L;
        }
        if (value.getValueCase() == AnyValue.ValueCase.INT_VALUE) {
            return value.getIntValue();
        }
        if (value.getValueCase() == AnyValue.ValueCase.STRING_VALUE) {
            return Long.parseLong(value.getStringValue());
        }
        return -1L;
    }

    private static AnyValue topLevelValue(LogRecord logRecord, String key) {
        if (logRecord.getBody().getValueCase() != AnyValue.ValueCase.KVLIST_VALUE) {
            return null;
        }

        for (final var field : logRecord.getBody().getKvlistValue().getValuesList()) {
            if (key.equals(field.getKey())) {
                return field.getValue();
            }
        }
        return null;
    }

    private static List<LogRecord> parseFixture(String filename) throws IOException {
        final var lines = Resources.readLines(Resources.getResource(WindowsEventLogRecordProcessorTest.class, filename), StandardCharsets.UTF_8);
        final List<LogRecord> records = new ArrayList<>();

        for (final var line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }

            final var request = ExportLogsServiceRequest.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(line, request);

            for (final var resourceLogs : request.getResourceLogsList()) {
                for (final var scopeLogs : resourceLogs.getScopeLogsList()) {
                    records.addAll(scopeLogs.getLogRecordsList());
                }
            }
        }

        return records;
    }
}
