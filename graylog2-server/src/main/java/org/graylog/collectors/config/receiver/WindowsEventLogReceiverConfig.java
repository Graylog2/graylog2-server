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
package org.graylog.collectors.config.receiver;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.auto.value.AutoValue;
import org.graylog.collectors.config.GoDurationSerializer;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * OTel collector windowseventlog receiver configuration.
 *
 * @see <a href="https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/windowseventlogreceiver">windowseventlog receiver</a>
 */
@AutoValue
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public abstract class WindowsEventLogReceiverConfig implements CollectorReceiverConfig, CollectorStanzaReceiver {
    public static final String RECEIVER_TYPE = "windowseventlog";

    private static final List<String> DEFAULT_CHANNELS = List.of(
            "Application",
            "System",
            "Security",
            "Setup",
            "Microsoft-Windows-Windows Defender/Operational",
            "Microsoft-Windows-TerminalServices-LocalSessionManager/Operational",
            "Microsoft-Windows-PowerShell/Operational",
            "Windows PowerShell"
    );

    public enum StartAt {
        @JsonProperty("beginning")
        BEGINNING,
        @JsonProperty("end")
        END
    }

    public String type() {
        return RECEIVER_TYPE;
    }

    @JsonIgnore
    public abstract List<String> channels();

    @JsonProperty("channel_list")
    public List<String> channelList() {
        final var channelSet = new HashSet<>(channels());
        if (includeDefaultChannels()) {
            channelSet.addAll(DEFAULT_CHANNELS);
        }
        return List.copyOf(channelSet);
    }

    @JsonIgnore
    public abstract boolean includeDefaultChannels();

    @JsonProperty("max_reads")
    public abstract int maxReads();

    @JsonProperty("poll_interval")
    @JsonSerialize(using = GoDurationSerializer.class)
    public abstract Duration pollInterval();

    @JsonProperty("max_events_per_poll")
    public abstract int maxEventsPerPoll();

    @JsonProperty("start_at")
    public abstract StartAt startAt();

    @JsonProperty("raw")
    public abstract boolean raw();

    @JsonProperty("include_log_record_original")
    public abstract boolean includeLogRecordOriginal();

    public static Builder builder(String id) {
        return new AutoValue_WindowsEventLogReceiverConfig.Builder()
                .name(f("windowseventlog/%s", id))
                .channels(List.of())
                .includeDefaultChannels(true)
                .startAt(StartAt.END)
                .maxReads(100)
                .pollInterval(Duration.ofSeconds(1))
                .maxEventsPerPoll(0)
                .raw(false)
                .includeLogRecordOriginal(false);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);

        public abstract Builder channels(List<String> channels);

        public abstract Builder includeDefaultChannels(boolean includeDefaultChannels);

        public abstract Builder startAt(StartAt startAt);

        public abstract Builder maxReads(int maxReads);

        public abstract Builder pollInterval(Duration pollInterval);

        public abstract Builder maxEventsPerPoll(int maxEventsPerPoll);

        public abstract Builder raw(boolean raw);

        public abstract Builder includeLogRecordOriginal(boolean includeLogRecordOriginal);

        public abstract WindowsEventLogReceiverConfig build();
    }
}

// From a Windows 2019 instance:
//> Get-WinEvent -ListLog *
//LogMode   MaximumSizeInBytes RecordCount LogName
//-------   ------------------ ----------- -------
//Circular            20971520         111 Application
//Circular            20971520           0 HardwareEvents
//Circular             1052672           0 Internet Explorer
//Circular            20971520           0 Key Management Service
//Circular            20971520        6231 Security
//Circular            20971520        1055 System
//Circular            15728640         538 Windows PowerShell
//Circular            20971520             ForwardedEvents
//Circular            10485760           0 Microsoft-AppV-Client/Admin
//Circular            10485760           0 Microsoft-AppV-Client/Operational
//Circular            10485760           0 Microsoft-AppV-Client/Virtual Applications
//Circular             1052672          33 Microsoft-Client-Licensing-Platform/Admin
//Circular             1052672             Microsoft-Management-UI/Admin
//Circular             1052672           0 Microsoft-Rdms-UI/Admin
//Circular             1052672           0 Microsoft-Rdms-UI/Operational
//Circular             1052672           0 Microsoft-User Experience Virtualization-Agent Driver/Operational
//Circular             1052672           0 Microsoft-User Experience Virtualization-App Agent/Operational
//Circular             1052672           0 Microsoft-User Experience Virtualization-IPC/Operational
//Circular             1052672           0 Microsoft-User Experience Virtualization-SQM Uploader/Operational
//Circular             1052672           0 Microsoft-Windows-AAD/Operational
//Circular             1052672           2 Microsoft-Windows-All-User-Install-Agent/Admin
//Circular             1052672           0 Microsoft-Windows-AllJoyn/Operational
//Circular             1052672           0 Microsoft-Windows-AppHost/Admin
//Circular             1052672           0 Microsoft-Windows-AppID/Operational
//Circular             1052672           0 Microsoft-Windows-ApplicabilityEngine/Operational
//Circular             1052672           0 Microsoft-Windows-Application Server-Applications/Admin
//Circular             1052672           0 Microsoft-Windows-Application Server-Applications/Operational
//Circular             1052672           0 Microsoft-Windows-Application-Experience/Program-Compatibility-Assistant
//Circular             1052672           0 Microsoft-Windows-Application-Experience/Program-Compatibility-Troubleshooter
//Circular             1052672           0 Microsoft-Windows-Application-Experience/Program-Inventory
//Circular             1052672           0 Microsoft-Windows-Application-Experience/Program-Telemetry
//Circular             1052672           0 Microsoft-Windows-Application-Experience/Steps-Recorder
//Circular             1052672           0 Microsoft-Windows-AppLocker/EXE and DLL
//Circular             1052672           0 Microsoft-Windows-AppLocker/MSI and Script
//Circular             1052672           0 Microsoft-Windows-AppLocker/Packaged app-Deployment
//Circular             1052672           0 Microsoft-Windows-AppLocker/Packaged app-Execution
//Circular             1052672         133 Microsoft-Windows-AppModel-Runtime/Admin
//Circular             5242880         119 Microsoft-Windows-AppReadiness/Admin
//Circular             5242880          30 Microsoft-Windows-AppReadiness/Operational
//Circular             1052672          30 Microsoft-Windows-AppXDeployment/Operational
//Circular             5242880         739 Microsoft-Windows-AppXDeploymentServer/Operational
//Circular             1052672           0 Microsoft-Windows-AppXDeploymentServer/Restricted
//Circular             1052672           8 Microsoft-Windows-AppxPackaging/Operational
//Circular             1052672             Microsoft-Windows-ASN1/Operational
//Circular             1052672           0 Microsoft-Windows-Audio/CaptureMonitor
//Circular             1052672             Microsoft-Windows-Audio/GlitchDetection
//Circular             1052672             Microsoft-Windows-Audio/Informational
//Circular             1052672           0 Microsoft-Windows-Audio/Operational
//Circular             1052672           0 Microsoft-Windows-Audio/PlaybackManager
//Circular             1052672           0 Microsoft-Windows-Authentication User Interface/Operational
//Circular             1052672             Microsoft-Windows-Authentication/AuthenticationPolicyFailures-DomainController
//Circular             1052672             Microsoft-Windows-Authentication/ProtectedUser-Client
//Circular             1052672             Microsoft-Windows-Authentication/ProtectedUserFailures-DomainController
//Circular             1052672             Microsoft-Windows-Authentication/ProtectedUserSuccesses-DomainController
//Circular             1052672           0 Microsoft-Windows-BackgroundTaskInfrastructure/Operational
//Circular             1052672             Microsoft-Windows-BackgroundTransfer-ContentPrefetcher/Operational
//Circular             1052672           0 Microsoft-Windows-Backup
//Circular             1052672             Microsoft-Windows-Base-Filtering-Engine-Connections/Operational
//Circular             1052672             Microsoft-Windows-Base-Filtering-Engine-Resource-Flows/Operational
//Circular             1052672           0 Microsoft-Windows-BestPractices/Operational
//Circular             1052672           0 Microsoft-Windows-Biometrics/Operational
//Circular             1052672             Microsoft-Windows-Bits-Client/Analytic
//Circular             1052672           1 Microsoft-Windows-Bits-Client/Operational
//Circular             1052672           0 Microsoft-Windows-Bluetooth-BthLEPrepairing/Operational
//Circular             1052672             Microsoft-Windows-Bluetooth-Bthmini/Operational
//Circular             1052672           0 Microsoft-Windows-Bluetooth-MTPEnum/Operational
//Circular             1052672           0 Microsoft-Windows-Bluetooth-Policy/Operational
//Circular             1052672           0 Microsoft-Windows-BranchCacheSMB/Operational
//Circular             1052672           0 Microsoft-Windows-CAPI2/Operational
//Circular             1052672           0 Microsoft-Windows-CertificateServices-Deployment/Operational
//Circular             1052672             Microsoft-Windows-CertificateServicesClient-CredentialRoaming/Operational
//Circular             1052672           0 Microsoft-Windows-CertificateServicesClient-Lifecycle-System/Operational
//Circular             1052672           0 Microsoft-Windows-CertificateServicesClient-Lifecycle-User/Operational
//Circular             1052672             Microsoft-Windows-CertPoleEng/Operational
//Circular            10485760             Microsoft-Windows-CloudStore/Debug
//Circular             1052672           0 Microsoft-Windows-CloudStore/Operational
//Circular             1052672           4 Microsoft-Windows-CodeIntegrity/Operational
//Circular             1052672           0 Microsoft-Windows-Compat-Appraiser/Operational
//Circular             1052672           0 Microsoft-Windows-Containers-BindFlt/Operational
//Circular             1052672           0 Microsoft-Windows-Containers-CCG/Admin
//Circular             1052672           4 Microsoft-Windows-Containers-Wcifs/Operational
//Circular             1052672           0 Microsoft-Windows-Containers-Wcnfs/Operational
//Circular             1052672           0 Microsoft-Windows-CoreApplication/Operational
//Circular             1052672           0 Microsoft-Windows-CorruptedFileRecovery-Client/Operational
//Circular             1052672           0 Microsoft-Windows-CorruptedFileRecovery-Server/Operational
//Circular             1052672           0 Microsoft-Windows-Crypto-DPAPI/BackUpKeySvc
//Circular             1052672             Microsoft-Windows-Crypto-DPAPI/Debug
//Circular             1052672          20 Microsoft-Windows-Crypto-DPAPI/Operational
//Circular             1052672           0 Microsoft-Windows-Crypto-NCrypt/CertInUse
//Circular             1052672             Microsoft-Windows-Crypto-NCrypt/Operational
//Circular             1052672           0 Microsoft-Windows-DAL-Provider/Operational
//Circular             1052672           0 Microsoft-Windows-DataIntegrityScan/Admin
//Circular             1052672           0 Microsoft-Windows-DataIntegrityScan/CrashRecovery
//Circular             1052672           0 Microsoft-Windows-DateTimeControlPanel/Operational
//Circular             1052672           0 Microsoft-Windows-DeviceGuard/Operational
//Circular             1052672           1 Microsoft-Windows-DeviceManagement-Enterprise-Diagnostics-Provider/Admin
//Circular             1052672           0 Microsoft-Windows-DeviceManagement-Enterprise-Diagnostics-Provider/Operational
//Circular             1052672           0 Microsoft-Windows-Devices-Background/Operational
//Circular             1052672          44 Microsoft-Windows-DeviceSetupManager/Admin
//Circular             1052672           0 Microsoft-Windows-DeviceSetupManager/Operational
//Circular             1052672           0 Microsoft-Windows-DeviceSync/Operational
//Circular             1052672           0 Microsoft-Windows-DeviceUpdateAgent/Operational
//Circular             1052672           1 Microsoft-Windows-Dhcp-Client/Admin
//Circular             1052672             Microsoft-Windows-Dhcp-Client/Operational
//Circular             1052672           0 Microsoft-Windows-Dhcpv6-Client/Admin
//Circular             1052672             Microsoft-Windows-Dhcpv6-Client/Operational
//Circular             1052672           4 Microsoft-Windows-Diagnosis-DPS/Operational
//Circular             1052672           0 Microsoft-Windows-Diagnosis-PCW/Operational
//Circular             1052672           0 Microsoft-Windows-Diagnosis-PLA/Operational
//Circular             1052672           0 Microsoft-Windows-Diagnosis-Scheduled/Operational
//Circular             1052672           0 Microsoft-Windows-Diagnosis-Scripted/Admin
//Circular             1052672           0 Microsoft-Windows-Diagnosis-Scripted/Operational
//Circular             1052672           0 Microsoft-Windows-Diagnosis-ScriptedDiagnosticsProvider/Operational
//Circular             1052672           0 Microsoft-Windows-Diagnostics-Networking/Operational
//Circular             1052672           0 Microsoft-Windows-DirectoryServices-Deployment/Operational
//Circular             1052672           0 Microsoft-Windows-DiskDiagnostic/Operational
//Circular             1052672           0 Microsoft-Windows-DiskDiagnosticDataCollector/Operational
//Circular             1052672           0 Microsoft-Windows-DiskDiagnosticResolver/Operational
//Circular             1052672             Microsoft-Windows-DisplayColorCalibration/Operational
//Circular             1052672             Microsoft-Windows-DNS-Client/Operational
//Circular             1052672             Microsoft-Windows-DriverFrameworks-UserMode/Operational
//Circular             1052672           0 Microsoft-Windows-DSC/Admin
//Circular             1052672           0 Microsoft-Windows-DSC/Operational
//Circular             1052672           0 Microsoft-Windows-EapHost/Operational
//Circular             1052672           0 Microsoft-Windows-EapMethods-RasChap/Operational
//Circular             1052672           0 Microsoft-Windows-EapMethods-RasTls/Operational
//Circular             1052672           0 Microsoft-Windows-EapMethods-Sim/Operational
//Circular             1052672           0 Microsoft-Windows-EapMethods-Ttls/Operational
//Circular             1052672           0 Microsoft-Windows-EDP-Application-Learning/Admin
//Circular             1052672           0 Microsoft-Windows-EDP-Audit-Regular/Admin
//Circular             1052672           0 Microsoft-Windows-EDP-Audit-TCB/Admin
//Circular             1052672           0 Microsoft-Windows-EnrollmentPolicyWebService/Admin
//Circular             1052672           0 Microsoft-Windows-EnrollmentWebService/Admin
//Circular             1052672             Microsoft-Windows-ESE/Operational
//Circular             1052672           0 Microsoft-Windows-EventCollector/Operational
//Circular             1052672           0 Microsoft-Windows-Fault-Tolerant-Heap/Operational
//Circular             1052672           0 Microsoft-Windows-FeatureConfiguration/Operational
//Circular             1052672           0 Microsoft-Windows-FederationServices-Deployment/Operational
//Circular             1052672           0 Microsoft-Windows-FileServices-ServerManager-EventProvider/Admin
//Circular             1052672           0 Microsoft-Windows-FileServices-ServerManager-EventProvider/Operational
//Circular             1052672           0 Microsoft-Windows-FileShareShadowCopyProvider/Operational
//Circular             1052672           0 Microsoft-Windows-FMS/Operational
//Circular             4194304           0 Microsoft-Windows-Folder Redirection/Operational
//Circular             1052672           0 Microsoft-Windows-Forwarding/Operational
//Circular             1052672           0 Microsoft-Windows-GenericRoaming/Admin
//Circular             1052672             Microsoft-Windows-glcnd/Admin
//Circular             4194304         139 Microsoft-Windows-GroupPolicy/Operational
//Circular             1052672           6 Microsoft-Windows-HelloForBusiness/Operational
//Circular             1052672           0 Microsoft-Windows-Help/Operational
//Circular             1052672           0 Microsoft-Windows-HomeGroup Control Panel/Operational
//Circular             1052672             Microsoft-Windows-HttpService/Log
//Circular             1052672             Microsoft-Windows-HttpService/Trace
//Circular             1052672           0 Microsoft-Windows-Hyper-V-Guest-Drivers/Admin
//Circular             1052672             Microsoft-Windows-Hyper-V-Guest-Drivers/Operational
//Circular             1052672           0 Microsoft-Windows-Hyper-V-Hypervisor-Admin
//Circular             1052672           0 Microsoft-Windows-Hyper-V-Hypervisor-Operational
//Circular             1052672           0 Microsoft-Windows-IdCtrls/Operational
//Circular             1052672           0 Microsoft-Windows-IKE/Operational
//Circular             1052672           0 Microsoft-Windows-International-RegionalOptionsControlPanel/Operational
//Circular             1052672           2 Microsoft-Windows-International/Operational
//Circular             1052672           0 Microsoft-Windows-Iphlpsvc/Operational
//Circular             1052672           0 Microsoft-Windows-KdsSvc/Operational
//Circular             1052672             Microsoft-Windows-Kerberos-KdcProxy/Operational
//Circular             1052672             Microsoft-Windows-Kerberos/Operational
//Circular             1052672           0 Microsoft-Windows-Kernel-ApphelpCache/Operational
//Circular             1052672          52 Microsoft-Windows-Kernel-Boot/Operational
//Circular             1052672           0 Microsoft-Windows-Kernel-EventTracing/Admin
//Circular             1052672         412 Microsoft-Windows-Kernel-IO/Operational
//Circular             1052672          16 Microsoft-Windows-Kernel-PnP/Configuration
//Circular             1052672           0 Microsoft-Windows-Kernel-Power/Thermal-Operational
//Circular             1052672           2 Microsoft-Windows-Kernel-ShimEngine/Operational
//Circular             1052672           0 Microsoft-Windows-Kernel-StoreMgr/Operational
//Circular             1052672           0 Microsoft-Windows-Kernel-WDI/Operational
//Circular             1052672           0 Microsoft-Windows-Kernel-WHEA/Errors
//Circular             1052672           8 Microsoft-Windows-Kernel-WHEA/Operational
//Circular             1052672          61 Microsoft-Windows-Known Folders API Service
//Circular             1052672           2 Microsoft-Windows-LanguagePackSetup/Operational
//Circular            52428800           0 Microsoft-Windows-LAPS/Operational
//Circular             1052672             Microsoft-Windows-LinkLayerDiscoveryProtocol/Operational
//Circular             1052672         604 Microsoft-Windows-LiveId/Operational
//Circular             1052672             Microsoft-Windows-LSA/Operational
//Circular             1052672           0 Microsoft-Windows-ManagementTools-RegistryProvider/Operational
//Circular             1052672           0 Microsoft-Windows-ManagementTools-TaskManagerProvider/Operational
//Circular             1052672             Microsoft-Windows-MediaFoundation-Performance/SARStreamResource
//Circular             1052672           0 Microsoft-Windows-MemoryDiagnostics-Results/Debug
//Circular             1052672           0 Microsoft-Windows-MiStreamProvider/Operational
//Circular             1052672           0 Microsoft-Windows-Mobile-Broadband-Experience-Parser-Task/Operational
//Circular             1052672           0 Microsoft-Windows-Mobile-Broadband-Experience-SmsRouter/Admin
//Circular             1052672           0 Microsoft-Windows-Mprddm/Operational
//Circular             1052672           0 Microsoft-Windows-MsLbfoProvider/Operational
//Circular             1052672             Microsoft-Windows-MSPaint/Admin
//Circular             1052672           0 Microsoft-Windows-MUI/Admin
//Circular             1052672          12 Microsoft-Windows-MUI/Operational
//Circular             1052672             Microsoft-Windows-Ncasvc/Operational
//Circular             1052672           7 Microsoft-Windows-NCSI/Operational
//Circular             1052672             Microsoft-Windows-NDIS/Operational
//Circular             1052672           0 Microsoft-Windows-NdisImPlatform/Operational
//Circular             1052672           1 Microsoft-Windows-NetworkLocationWizard/Operational
//Circular             1052672          25 Microsoft-Windows-NetworkProfile/Operational
//Circular             1052672           0 Microsoft-Windows-NetworkProvider/Operational
//Circular             1052672           0 Microsoft-Windows-NlaSvc/Operational
//Circular            33554432          44 Microsoft-Windows-Ntfs/Operational
//Circular             1052672           4 Microsoft-Windows-Ntfs/WHC
//Circular             1052672           0 Microsoft-Windows-NTLM/Operational
//Circular             1052672           0 Microsoft-Windows-OfflineFiles/Operational
//Circular             1052672             Microsoft-Windows-OneX/Operational
//Circular             1052672           0 Microsoft-Windows-OOBE-Machine-DUI/Operational
//Circular             1052672             Microsoft-Windows-OtpCredentialProvider/Operational
//Circular             1052672           0 Microsoft-Windows-PackageStateRoaming/Operational
//Circular            16777216           5 Microsoft-Windows-Partition/Diagnostic
//Circular             1052672           0 Microsoft-Windows-PerceptionRuntime/Operational
//Circular             1052672           0 Microsoft-Windows-PerceptionSensorDataService/Operational
//Circular             1052672           0 Microsoft-Windows-PersistentMemory-Nvdimm/Operational
//Circular             1052672           0 Microsoft-Windows-PersistentMemory-PmemDisk/Operational
//Circular             1052672           0 Microsoft-Windows-PersistentMemory-ScmBus/Certification
//Circular             1052672             Microsoft-Windows-PersistentMemory-ScmBus/Operational
//Circular             1052672           0 Microsoft-Windows-Policy/Operational
//Circular             1052672           0 Microsoft-Windows-PowerShell-DesiredStateConfiguration-FileDownloadManager/Oper...
//Retain            1048985600           0 Microsoft-Windows-PowerShell/Admin
//Circular            15728640         546 Microsoft-Windows-PowerShell/Operational
//Circular             1052672           0 Microsoft-Windows-PrintBRM/Admin
//Circular             1052672           1 Microsoft-Windows-PrintService/Admin
//Circular             1052672             Microsoft-Windows-PrintService/Operational
//Circular             1052672           0 Microsoft-Windows-PriResources-Deployment/Operational
//Circular             1052672             Microsoft-Windows-Program-Compatibility-Assistant/Analytic
//Circular             1052672           0 Microsoft-Windows-Program-Compatibility-Assistant/CompatAfterUpgrade
//Circular             1052672             Microsoft-Windows-Proximity-Common/Diagnostic
//Circular             1052672           0 Microsoft-Windows-PushNotification-Platform/Admin
//Circular             1052672          75 Microsoft-Windows-PushNotification-Platform/Operational
//Circular             1052672             Microsoft-Windows-RasAgileVpn/Operational
//Circular             1052672           0 Microsoft-Windows-ReadyBoost/Operational
//Circular             1052672           0 Microsoft-Windows-ReFS/Operational
//Circular             1052672           0 Microsoft-Windows-Regsvr32/Operational
//Circular             1052672           0 Microsoft-Windows-RemoteApp and Desktop Connections/Admin
//Circular             1052672           0 Microsoft-Windows-RemoteApp and Desktop Connections/Operational
//Circular             1052672           0 Microsoft-Windows-RemoteDesktopServices-RdpCoreTS/Admin
//Circular             1052672        1967 Microsoft-Windows-RemoteDesktopServices-RdpCoreTS/Operational
//Circular             1052672           0 Microsoft-Windows-RemoteDesktopServices-RemoteFX-Synth3dvsc/Admin
//Circular             1052672           1 Microsoft-Windows-RemoteDesktopServices-SessionServices/Operational
//Circular             1052672             Microsoft-Windows-Remotefs-Rdbss/Operational
//Circular             1052672           3 Microsoft-Windows-Resource-Exhaustion-Detector/Operational
//Circular             1052672           0 Microsoft-Windows-Resource-Exhaustion-Resolver/Operational
//Circular             1052672           0 Microsoft-Windows-RestartManager/Operational
//Circular             1052672             Microsoft-Windows-RRAS/Operational
//Circular             1052672           0 Microsoft-Windows-SearchUI/Operational
//Circular             1052672           0 Microsoft-Windows-Security-Adminless/Operational
//Circular             1052672           0 Microsoft-Windows-Security-Audit-Configuration-Client/Operational
//Circular             1052672           0 Microsoft-Windows-Security-EnterpriseData-FileRevocationManager/Operational
//Circular             1052672             Microsoft-Windows-Security-ExchangeActiveSyncProvisioning/Operational
//Circular             1052672             Microsoft-Windows-Security-IdentityListener/Operational
//Circular             1052672           0 Microsoft-Windows-Security-LessPrivilegedAppContainer/Operational
//Circular             1052672         112 Microsoft-Windows-Security-Mitigations/KernelMode
//Circular             1052672           0 Microsoft-Windows-Security-Mitigations/UserMode
//Circular             1052672           0 Microsoft-Windows-Security-Netlogon/Operational
//Circular             1052672           0 Microsoft-Windows-Security-SPP-UX-GenuineCenter-Logging/Operational
//Circular             1052672           2 Microsoft-Windows-Security-SPP-UX-Notifications/ActionCenter
//Circular             1052672           0 Microsoft-Windows-Security-UserConsentVerifier/Audit
//Circular             1052672             Microsoft-Windows-SecurityMitigationsBroker/Admin
//Circular             1052672           0 Microsoft-Windows-SecurityMitigationsBroker/Operational
//Circular             1052672           0 Microsoft-Windows-SENSE/Operational
//Circular             1052672           0 Microsoft-Windows-SenseIR/Operational
//Circular             1052672           0 Microsoft-Windows-ServerManager-ConfigureSMRemoting/Operational
//Circular             1052672          13 Microsoft-Windows-ServerManager-DeploymentProvider/Operational
//Circular             1052672           0 Microsoft-Windows-ServerManager-MgmtProvider/Operational
//Circular             1052672           0 Microsoft-Windows-ServerManager-MultiMachine/Admin
//Circular             1052672         540 Microsoft-Windows-ServerManager-MultiMachine/Operational
//Circular             1052672             Microsoft-Windows-ServiceReportingApi/Debug
//Circular             1052672           0 Microsoft-Windows-SettingSync-Azure/Debug
//Circular             1052672           0 Microsoft-Windows-SettingSync-Azure/Operational
//Circular             1052672           0 Microsoft-Windows-SettingSync-OneDrive/Debug
//Circular             1052672           0 Microsoft-Windows-SettingSync-OneDrive/Operational
//Circular             1052672          26 Microsoft-Windows-SettingSync/Debug
//Circular             1052672           1 Microsoft-Windows-SettingSync/Operational
//Circular             1052672           0 Microsoft-Windows-Shell-ConnectedAccountState/ActionCenter
//Circular             1052672           0 Microsoft-Windows-Shell-Core/ActionCenter
//Circular             1052672         329 Microsoft-Windows-Shell-Core/AppDefaults
//Circular             1052672           0 Microsoft-Windows-Shell-Core/LogonTasksChannel
//Circular             1052672         307 Microsoft-Windows-Shell-Core/Operational
//Circular             1052672          60 Microsoft-Windows-ShellCommon-StartLayoutPopulation/Operational
//Circular             1052672           0 Microsoft-Windows-SilProvider/Operational
//Circular             1052672           0 Microsoft-Windows-SmartCard-Audit/Authentication
//Circular             1052672           0 Microsoft-Windows-SmartCard-DeviceEnum/Operational
//Circular             1052672           0 Microsoft-Windows-SmartCard-TPM-VCard-Module/Admin
//Circular             1052672           0 Microsoft-Windows-SmartCard-TPM-VCard-Module/Operational
//Circular             1052672             Microsoft-Windows-SmartScreen/Debug
//Circular             8388608           0 Microsoft-Windows-SmbClient/Audit
//Circular             8388608          24 Microsoft-Windows-SmbClient/Connectivity
//Circular             8388608           0 Microsoft-Windows-SMBClient/Operational
//Circular             8388608           0 Microsoft-Windows-SmbClient/Security
//Circular             1052672           0 Microsoft-Windows-SMBDirect/Admin
//Circular             8388608           0 Microsoft-Windows-SMBServer/Audit
//Circular             8388608           0 Microsoft-Windows-SMBServer/Connectivity
//Circular             8388608          18 Microsoft-Windows-SMBServer/Operational
//Circular             8388608           0 Microsoft-Windows-SMBServer/Security
//Circular             1052672           0 Microsoft-Windows-SMBWitnessClient/Admin
//Circular             1052672           0 Microsoft-Windows-SMBWitnessClient/Informational
//Circular             5242880          73 Microsoft-Windows-StateRepository/Operational
//Circular             1052672           0 Microsoft-Windows-StateRepository/Restricted
//Circular             1052672             Microsoft-Windows-Storage-ATAPort/Admin
//Circular             1052672             Microsoft-Windows-Storage-ATAPort/Operational
//Circular             1052672             Microsoft-Windows-Storage-ClassPnP/Admin
//Circular             6291456           0 Microsoft-Windows-Storage-ClassPnP/Operational
//Circular             1052672             Microsoft-Windows-Storage-Disk/Admin
//Circular             1052672             Microsoft-Windows-Storage-Disk/Operational
//Circular             1052672             Microsoft-Windows-Storage-Storport/Admin
//Circular             6291456          31 Microsoft-Windows-Storage-Storport/Health
//Circular            33554432          39 Microsoft-Windows-Storage-Storport/Operational
//Circular             1052672           0 Microsoft-Windows-Storage-Tiering/Admin
//Circular             1052672           0 Microsoft-Windows-StorageManagement-PartUtil/Operational
//Circular            33554432           0 Microsoft-Windows-StorageManagement/Operational
//Circular             1052672           0 Microsoft-Windows-StorageSpaces-Api/Operational
//Circular            16777216           0 Microsoft-Windows-StorageSpaces-Driver/Diagnostic
//Circular             1052672           4 Microsoft-Windows-StorageSpaces-Driver/Operational
//Circular             1052672           0 Microsoft-Windows-StorageSpaces-ManagementAgent/WHC
//Circular            16777216           0 Microsoft-Windows-StorageSpaces-SpaceManager/Diagnostic
//Circular             1052672           0 Microsoft-Windows-StorageSpaces-SpaceManager/Operational
//Circular            20000000         317 Microsoft-Windows-Store/Operational
//Circular           314572800       89203 Microsoft-Windows-SystemDataArchiver/Diagnostic
//Circular             1052672           0 Microsoft-Windows-SystemSettingsThreshold/Operational
//Circular             1052672          16 Microsoft-Windows-TaskScheduler/Maintenance
//Circular            10485760             Microsoft-Windows-TaskScheduler/Operational
//Circular             1052672           0 Microsoft-Windows-TCPIP/Operational
//Circular             1052672           0 Microsoft-Windows-TerminalServices-ClientUSBDevices/Admin
//Circular             1052672           0 Microsoft-Windows-TerminalServices-ClientUSBDevices/Operational
//Circular             1052672           0 Microsoft-Windows-TerminalServices-LocalSessionManager/Admin
//Circular             1052672          65 Microsoft-Windows-TerminalServices-LocalSessionManager/Operational
//Circular             1052672           0 Microsoft-Windows-TerminalServices-PnPDevices/Admin
//Circular             1052672           0 Microsoft-Windows-TerminalServices-PnPDevices/Operational
//Circular             1052672           0 Microsoft-Windows-TerminalServices-Printers/Admin
//Circular             1052672           0 Microsoft-Windows-TerminalServices-Printers/Operational
//Circular             1052672           0 Microsoft-Windows-TerminalServices-RDPClient/Operational
//Circular             1052672           2 Microsoft-Windows-TerminalServices-RemoteConnectionManager/Admin
//Circular             1052672        1532 Microsoft-Windows-TerminalServices-RemoteConnectionManager/Operational
//Circular             1052672           0 Microsoft-Windows-TerminalServices-ServerUSBDevices/Admin
//Circular             1052672           0 Microsoft-Windows-TerminalServices-ServerUSBDevices/Operational
//Circular             1052672           0 Microsoft-Windows-TerminalServices-SessionBroker-Client/Admin
//Circular             1052672           0 Microsoft-Windows-TerminalServices-SessionBroker-Client/Operational
//Circular             1052672           0 Microsoft-Windows-Time-Service-PTP-Provider/PTP-Operational
//Circular             1052672         393 Microsoft-Windows-Time-Service/Operational
//Circular             1052672           0 Microsoft-Windows-TWinUI/Operational
//Circular             1052672           0 Microsoft-Windows-TZSync/Operational
//Circular             1052672           0 Microsoft-Windows-TZUtil/Operational
//Circular             1052672           0 Microsoft-Windows-UAC-FileVirtualization/Operational
//Circular             1052672           0 Microsoft-Windows-UAC/Operational
//Circular             1052672           0 Microsoft-Windows-UniversalTelemetryClient/Operational
//Circular             1052672           0 Microsoft-Windows-User Control Panel/Operational
//Circular             1052672           9 Microsoft-Windows-User Device Registration/Admin
//Circular             4194304          18 Microsoft-Windows-User Profile Service/Operational
//Circular             1052672           0 Microsoft-Windows-User-Loader/Operational
//Circular             1052672           0 Microsoft-Windows-UserPnp/ActionCenter
//Circular             1052672          13 Microsoft-Windows-UserPnp/DeviceInstall
//Circular             1052672           0 Microsoft-Windows-VDRVROOT/Operational
//Circular             1052672           0 Microsoft-Windows-VerifyHardwareSecurity/Admin
//Circular             1052672             Microsoft-Windows-VerifyHardwareSecurity/Operational
//Circular             1052672           0 Microsoft-Windows-VHDMP-Operational
//Circular             1052672           0 Microsoft-Windows-Volume/Diagnostic
//Circular             1052672          10 Microsoft-Windows-VolumeSnapshot-Driver/Operational
//Circular             1052672           0 Microsoft-Windows-VPN-Client/Operational
//Circular             1052672           0 Microsoft-Windows-VPN/Operational
//Circular             1052672          40 Microsoft-Windows-Wcmsvc/Operational
//Circular             1052672             Microsoft-Windows-WebAuth/Operational
//Circular             5242880           8 Microsoft-Windows-WebAuthN/Operational
//Circular             1052672             Microsoft-Windows-WebIO-NDF/Diagnostic
//Circular             1052672             Microsoft-Windows-WEPHOSTSVC/Operational
//Circular             1052672           0 Microsoft-Windows-WER-PayloadHealth/Operational
//Circular             1052672           0 Microsoft-Windows-WFP/Operational
//Circular             1052672          20 Microsoft-Windows-Win32k/Operational
//Circular             1052672         122 Microsoft-Windows-Windows Defender/Operational
//Circular             1052672           0 Microsoft-Windows-Windows Defender/WHC
//Circular             1052672           0 Microsoft-Windows-Windows Firewall With Advanced Security/ConnectionSecurity
//Circular             1052672             Microsoft-Windows-Windows Firewall With Advanced Security/ConnectionSecurityVer...
//Circular             1052672         153 Microsoft-Windows-Windows Firewall With Advanced Security/Firewall
//Circular             1052672           4 Microsoft-Windows-Windows Firewall With Advanced Security/FirewallDiagnostics
//Circular             1052672             Microsoft-Windows-Windows Firewall With Advanced Security/FirewallVerbose
//Circular             1052672             Microsoft-Windows-WindowsColorSystem/Operational
//Circular             1052672           0 Microsoft-Windows-WindowsSystemAssessmentTool/Operational
//Circular             1052672             Microsoft-Windows-WindowsUIImmersive/Operational
//Circular             1052672          12 Microsoft-Windows-WindowsUpdateClient/Operational
//Circular             1052672             Microsoft-Windows-WinHTTP-NDF/Diagnostic
//Circular             1052672           0 Microsoft-Windows-WinHttp/Operational
//Circular             1052672             Microsoft-Windows-WinINet-Capture/Analytic
//Circular             1052672           0 Microsoft-Windows-WinINet-Config/ProxyConfigChanged
//Circular             1052672           0 Microsoft-Windows-WinINet/Operational
//Circular             1052672         176 Microsoft-Windows-Winlogon/Operational
//Circular             1052672             Microsoft-Windows-WinNat/Oper
//Circular             1052672          20 Microsoft-Windows-WinRM/Operational
//Circular             1052672             Microsoft-Windows-Winsock-AFD/Operational
//Circular             1052672             Microsoft-Windows-Winsock-NameResolution/Operational
//Circular             1052672           0 Microsoft-Windows-Winsock-WS2HELP/Operational
//Circular             1052672           0 Microsoft-Windows-Wired-AutoConfig/Operational
//Circular             1052672         106 Microsoft-Windows-WMI-Activity/Operational
//Circular             1052672           0 Microsoft-Windows-WMPNSS-Service/Operational
//Circular             1052672             Microsoft-Windows-Wordpad/Admin
//Circular             1052672           0 Microsoft-Windows-Workplace Join/Admin
//Circular             1052672           0 Microsoft-Windows-WPD-ClassInstaller/Operational
//Circular             1052672           0 Microsoft-Windows-WPD-CompositeClassDriver/Operational
//Circular             1052672           0 Microsoft-Windows-WPD-MTPClassDriver/Operational
//Circular             1052672             Network Isolation Operational
//Circular             1052672          25 OpenSSH/Admin
//Circular             1052672        1673 OpenSSH/Operational
//Circular             1052672           3 Setup
//Circular             1052672           0 SMSApi
//Circular             1052672             Windows Networking Vpn Plugin Platform/Operational
//Circular             1052672             Windows Networking Vpn Plugin Platform/OperationalVerbose
