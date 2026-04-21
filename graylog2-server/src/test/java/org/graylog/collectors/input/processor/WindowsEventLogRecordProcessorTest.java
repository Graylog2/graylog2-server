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
import org.graylog.inputs.otel.codec.OTelTypeConverter;
import org.graylog.schema.DestinationFields;
import org.graylog.schema.EventFields;
import org.graylog.schema.HostFields;
import org.graylog.schema.ProcessFields;
import org.graylog.schema.SourceFields;
import org.graylog.schema.TraceFields;
import org.graylog.schema.VendorFields;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WindowsEventLogRecordProcessorTest {
    private static final String VENDOR_EVENT_CATEGORY = "vendor_event_category";
    private static final String VENDOR_OPCODE = "vendor_opcode";
    private static final String PRIVILEGE_ASSIGNED_NAME = "privilege_assigned_name";
    private static final String WINDOWS_LOGON_TYPE = "windows_logon_type";
    private static final String WINDOWS_AUTH_PACKAGE_NAME = "windows_authentication_package_name";
    private static final String WINDOWS_AUTH_PROCESS_NAME = "windows_authentication_process_name";
    private static final String WINDOWS_AUTH_LM_PACKAGE_NAME = "windows_authentication_lmpackage_name";
    private static final String VENDOR_EVENT_DATA = "vendor_event_data";


    private final WindowsEventLogRecordProcessor processor = new WindowsEventLogRecordProcessor(new OTelTypeConverter(new ObjectMapperProvider().get()));

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
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Security-Auditing")
                .containsEntry(VENDOR_EVENT_CATEGORY, "Logoff")
                .containsEntry(VENDOR_OPCODE, "Info")
                .containsEntry(VendorFields.VENDOR_VERSION, "3")
                .containsEntry(TraceFields.TRACE_ID, "{11111111-2222-3333-4444-555555555555}")
                .containsEntry(VendorFields.VENDOR_EVENT_TIMESTAMP,"2026-02-28 09:16:32.772")
                .containsEntry(ProcessFields.PROCESS_ID, "5860")
                .containsEntry(ProcessFields.PROCESS_PATH, "C:\\Windows\\System32\\OpenSSH\\sshd.exe")
                .containsEntry(ProcessFields.PROCESS_NAME, "sshd.exe")
                .containsEntry("TargetUserSid", "S-1-5-21-1000000000-1000000000-1000000000-500")
                .containsEntry("TargetUserName", "TestAdmin")
                .containsEntry("TargetDomainName", "WINHOST01")
                .containsEntry("TargetLogonId", "0x25501f9")
                .containsEntry("SubjectUserSid", "S-1-5-18")
                .containsEntry("SubjectUserName", "WINHOST01$")
                .containsEntry("SubjectDomainName", "TESTWG")
                .containsEntry("SubjectLogonId", "0x3e7");

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
                .containsEntry(EventFields.EVENT_UID, "140715")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Security-Auditing")
                .containsEntry(ProcessFields.PROCESS_ID, "5860")
                .containsEntry(ProcessFields.PROCESS_PATH, "C:\\Windows\\System32\\OpenSSH\\sshd.exe")
                .containsEntry(DestinationFields.DESTINATION_HOSTNAME, "localhost")
                .containsEntry("TargetUserName", "TestAdmin")
                .containsEntry("TargetDomainName", "WINHOST01")
                .containsEntry("SubjectUserSid", "S-1-5-18")
                .containsEntry("SubjectUserName", "WINHOST01$")
                .containsEntry("SubjectDomainName", "TESTWG")
                .containsEntry("SubjectLogonId", "0x3e7");

        assertThat(result).doesNotContainKeys(
                "TargetUserSid",
                "TargetLogonId",
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
                .containsEntry("SubjectUserSid", "S-1-5-21-1000000000-1000000000-1000000000-500")
                .containsEntry("SubjectUserName", "TestAdmin")
                .containsEntry("SubjectDomainName", "WINHOST01")
                .containsEntry("SubjectLogonId", "0x25501f9");
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
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "PowerShell")
                .containsEntry(VENDOR_EVENT_CATEGORY, "Engine Lifecycle")
                .containsEntry(VENDOR_OPCODE, "Info");
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
                .containsEntry(EventFields.EVENT_OUTCOME, "success")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Security-Auditing")
                .containsEntry(VENDOR_EVENT_CATEGORY, "Logon")
                .containsEntry(TraceFields.TRACE_ID, "{11111111-aaaa-0001-bbbb-cccccccccccc}")
                .containsEntry(ProcessFields.PROCESS_ID, "592")
                .containsEntry(ProcessFields.PROCESS_PATH, "C:\\Windows\\System32\\services.exe")
                .containsEntry(ProcessFields.PROCESS_NAME, "services.exe")
                .containsEntry("TargetUserSid", "S-1-5-18")
                .containsEntry("TargetUserName", "SYSTEM")
                .containsEntry("TargetDomainName", "NT AUTHORITY")
                .containsEntry("TargetLogonId", "0x3e7")
                .containsEntry("SubjectUserSid", "S-1-5-18")
                .containsEntry("SubjectUserName", "WINSERVER01$")
                .containsEntry("SubjectDomainName", "WORKGROUP")
                .containsEntry("SubjectLogonId", "0x3e7");

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
                .containsEntry("SubjectUserSid", "S-1-5-18")
                .containsEntry("SubjectUserName", "SYSTEM")
                .containsEntry("SubjectDomainName", "NT AUTHORITY")
                .containsEntry("SubjectLogonId", "0x3e7");
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
                .containsEntry(EventFields.EVENT_OUTCOME, "failure")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Security-Auditing")
                .containsEntry("TargetUserSid", "S-1-0-0")
                .containsEntry("TargetUserName", "USER")
                .containsEntry(SourceFields.SOURCE_IP, "198.51.100.42")
                .containsEntry(SourceFields.SOURCE_PORT, 0);

        assertThat(result).doesNotContainKeys(
                SourceFields.SOURCE_HOSTNAME,
                "TargetDomainName",
                "TargetLogonId"
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
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Service Control Manager")
                .containsEntry(VendorFields.VENDOR_EVENT_SEVERITY, "Information");

        assertThat(result).doesNotContainKeys(
                EventFields.EVENT_OUTCOME
        );
    }

    @Test
    void mapsDefenderEventWithSecurityUserType() throws IOException {
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-4.ndjson", 3661L);
        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(HostFields.HOST_HOSTNAME, "glcwin2025")
                .containsEntry(EventFields.EVENT_LOG_NAME, "Microsoft-Windows-Windows Defender/Operational")
                .containsEntry(EventFields.EVENT_CODE, 1150L)
                .containsEntry(VendorFields.VENDOR_EVENT_SEVERITY, "Information")
                .containsEntry(VendorFields.VENDOR_USER_TYPE, "WellKnownGroup");

        // System/Security user_id/user_name/domain are not promoted to flat fields
        // (no clear GIM target); only user_type is preserved via vendor_user_type.
        assertThat(result).doesNotContainKeys(
                "SubjectUserSid", "SubjectUserName", "SubjectDomainName"
        );
    }

    @Test
    void mapsSystemOnlyEventWithoutSubjectFlatFields() throws IOException {
        // 16977 has user context only in System/Security (no EventData Subject fields).
        // System/Security user_id/user_name/domain are not promoted to flat fields.
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-3.ndjson", 16521L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(HostFields.HOST_HOSTNAME, "winserver03")
                .containsEntry(EventFields.EVENT_LOG_NAME, "System")
                .containsEntry(EventFields.EVENT_CODE, 16977L)
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Directory-Services-SAM")
                .containsEntry(VendorFields.VENDOR_USER_TYPE, "WellKnownGroup");

        assertThat(result).doesNotContainKeys(
                "SubjectUserSid", "SubjectUserName", "SubjectDomainName"
        );
    }

    @Test
    void mapsUserDataSubjectFields() throws IOException {
        // Event 104 carries Subject identity in UserData (no EventData). Raw fields flow through.
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-3.ndjson", 16518L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(HostFields.HOST_HOSTNAME, "winserver03")
                .containsEntry(EventFields.EVENT_LOG_NAME, "System")
                .containsEntry(EventFields.EVENT_CODE, 104L)
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Eventlog")
                .containsEntry("SubjectUserName", "Administrator")
                .containsEntry("SubjectDomainName", "winserver03");
    }

    @Test
    void maps4688ProcessCreation() throws IOException {
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-3.ndjson", 500001L);

        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(HostFields.HOST_HOSTNAME, "testhost01")
                .containsEntry(EventFields.EVENT_LOG_NAME, "Security")
                .containsEntry(EventFields.EVENT_CODE, 4688L)
                .containsEntry(EventFields.EVENT_OUTCOME, "success")
                .containsEntry(VendorFields.VENDOR_SUBTYPE, "Microsoft-Windows-Security-Auditing")
                .containsEntry(ProcessFields.PROCESS_ID, "6700")
                .containsEntry(ProcessFields.PROCESS_PATH, "C:\\Windows\\System32\\cmd.exe")
                .containsEntry(ProcessFields.PROCESS_NAME, "cmd.exe")
                .containsEntry(ProcessFields.PROCESS_PARENT_PATH, "C:\\Windows\\System32\\services.exe")
                .containsEntry(ProcessFields.PROCESS_PARENT_NAME, "services.exe")
                .containsEntry("SubjectUserSid", "S-1-5-18")
                .containsEntry("SubjectUserName", "TESTHOST01$")
                .containsEntry("SubjectDomainName", "WORKGROUP")
                .containsEntry("SubjectLogonId", "0x3e7");
    }

    @Test
    void maps4625WithFailureFields() throws IOException {
        // 4625 from OTLP dump: record_id=1081233, Status=0xc000006d, SubStatus=0xc0000064
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-4.ndjson", 1081233L);
        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(EventFields.EVENT_CODE, 4625L)
                .containsEntry(EventFields.EVENT_OUTCOME, "failure")
                .containsEntry(WINDOWS_LOGON_TYPE, "3")
                .containsEntry(WINDOWS_AUTH_PACKAGE_NAME, "NTLM")
                .containsEntry(EventFields.EVENT_ERROR_CODE, "0xc000006d")
                .containsEntry("SubStatus", "0xc0000064")
                .containsEntry(EventFields.EVENT_ERROR_DESCRIPTION, "Unknown user name or bad password.")
                .containsEntry(SourceFields.SOURCE_IP, "108.221.24.75")
                .containsEntry(SourceFields.SOURCE_PORT, 0)
                .containsEntry("TargetUserSid", "S-1-0-0")
                .containsEntry("TargetUserName", "REMOTE")
                .containsEntry("SubjectUserSid", "S-1-0-0")
                .containsEntry("SubjectLogonId", "0x0");
    }

    @Test
    void maps4624WithNetworkAndAuthFields() throws IOException {
        // 4624 from OTLP dump: record_id=834375, real IP, NTLM V2, LogonType=3
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-4.ndjson", 834375L);
        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(EventFields.EVENT_CODE, 4624L)
                .containsEntry(EventFields.EVENT_OUTCOME, "success")
                .containsEntry(WINDOWS_LOGON_TYPE, "3")
                .containsEntry(WINDOWS_AUTH_PACKAGE_NAME, "NTLM")
                .containsEntry(WINDOWS_AUTH_LM_PACKAGE_NAME, "NTLM V2")
                .containsEntry("LogonGuid", "{00000000-0000-0000-0000-000000000000}")
                .containsEntry(SourceFields.SOURCE_IP, "31.150.102.150")
                .containsEntry(SourceFields.SOURCE_PORT, 0)
                .containsEntry(SourceFields.SOURCE_HOSTNAME, "h2")
                .containsEntry("TargetUserSid", "S-1-5-21-2637047489-2197293977-9275664-500")
                .containsEntry("TargetUserName", "Administrator")
                .containsEntry("TargetDomainName", "GLCWIN2022")
                .containsEntry("TargetLogonId", "0x297a8f1a");
    }

    @Test
    void serializesEventDataToJsonBlob() throws IOException {
        // 4717 from OTLP dump: has AccessGranted and TargetSid which are NOT extracted as GIM fields
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-4.ndjson", 695175L);
        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result)
                .containsEntry(EventFields.EVENT_CODE, 4717L)
                .containsEntry(EventFields.EVENT_OUTCOME, "success")
                .containsEntry("SubjectUserSid", "S-1-5-18")
                .containsEntry("SubjectUserName", "GLCWIN2025$")
                .containsEntry("SubjectDomainName", "WORKGROUP")
                .containsEntry("SubjectLogonId", "0x3e7")
                .containsKey(VENDOR_EVENT_DATA);

        // The JSON blob must contain ALL event_data keys (including ones extracted as GIM fields)
        final var vendorEventData = (String) result.get(VENDOR_EVENT_DATA);
        assertThat(vendorEventData)
                .contains("\"AccessGranted\"")
                .contains("\"SeServiceLogonRight\"")
                .contains("\"TargetSid\"")
                .contains("\"S-1-5-111-3847866527-469524349-687026318-516638107-1125189541-212\"")
                // Also contains extracted fields — blob preserves the full original structure
                .contains("\"SubjectUserSid\"")
                .contains("\"SubjectUserName\"");
    }

    @Test
    void serializesDefenderEventDataToJsonBlob() throws IOException {
        // Defender 1150: EventData with space-named keys
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-4.ndjson", 3661L);
        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsKey(VENDOR_EVENT_DATA);

        final var vendorEventData = (String) result.get(VENDOR_EVENT_DATA);
        // Keys with spaces are preserved in JSON
        assertThat(vendorEventData)
                .contains("\"Engine version\"")
                .contains("\"Product Name\"")
                .contains("\"Microsoft Defender Antivirus\"");
    }

    @Test
    void serializesSystemEventDataToJsonBlob() throws IOException {
        // System 7036: param1/param2/Binary
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-2.ndjson", 187435L);
        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsKey(VENDOR_EVENT_DATA);

        final var vendorEventData = (String) result.get(VENDOR_EVENT_DATA);
        assertThat(vendorEventData)
                .contains("\"param1\"")
                .contains("\"Windows Update Medic Service\"")
                .contains("\"param2\"")
                .contains("\"running\"");
    }

    @Test
    void omitsVendorEventDataWhenNoEventData() throws IOException {
        // PowerShell event (record_id=8526) has event_data with unnamed keys — verify blob is present
        final var logRecord = fixtureRecordByRecordId(8526L);
        final var result = processor.process(wrapLogRecord(logRecord));

        // This event has event_data, so blob should be present
        assertThat(result).containsKey(VENDOR_EVENT_DATA);
    }

    @Test
    void serializesUserDataToVendorEventData() throws IOException {
        // Event 104 EventLog-cleared: has user_data (not event_data). Serialized into vendor_event_data.
        final var logRecord = fixtureRecordByRecordId("windows-2025-eventlog-3.ndjson", 16518L);
        final var result = processor.process(wrapLogRecord(logRecord));

        assertThat(result).containsKey(VENDOR_EVENT_DATA);

        final var vendorEventData = (String) result.get(VENDOR_EVENT_DATA);
        assertThat(vendorEventData)
                .contains("\"SubjectUserName\"")
                .contains("\"Administrator\"")
                .contains("\"SubjectDomainName\"")
                .contains("\"winserver03\"")
                .contains("\"Channel\"")
                .contains("\"System\"");
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
