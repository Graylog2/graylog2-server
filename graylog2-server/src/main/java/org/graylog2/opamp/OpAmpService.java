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
package org.graylog2.opamp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.MediaType;
import opamp.proto.Anyvalue;
import opamp.proto.Opamp;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.ConnectionSettingsOffers;
import opamp.proto.Opamp.OpAMPConnectionSettings;
import opamp.proto.Opamp.ServerErrorResponse;
import opamp.proto.Opamp.ServerToAgent;
import opamp.proto.Opamp.TLSCertificate;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CoalescedActions;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.CollectorInstanceReport;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog2.opamp.enrollment.EnrollmentTokenService;
import org.graylog2.opamp.transport.OpAmpAuthContext;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.cluster.ClusterId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.stream.Collectors.toCollection;
import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class OpAmpService {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpService.class);

    private final EnrollmentTokenService enrollmentTokenService;
    private final OpAmpCaService opAmpCaService;
    private final CertificateService certificateService;
    private final CollectorInstanceService collectorInstanceService;
    private final ClusterConfigService clusterConfigService;
    private final FleetTransactionLogService txnLogService;
    private final SourceService sourceService;
    private final ObjectMapper objectMapper;

    @Inject
    public OpAmpService(EnrollmentTokenService enrollmentTokenService, OpAmpCaService opAmpCaService, CertificateService certificateService, CollectorInstanceService collectorInstanceService, ClusterConfigService clusterConfigService, FleetTransactionLogService txnLogService, SourceService sourceService, ObjectMapper objectMapper) {
        this.enrollmentTokenService = enrollmentTokenService;
        this.opAmpCaService = opAmpCaService;
        this.certificateService = certificateService;
        this.collectorInstanceService = collectorInstanceService;
        this.clusterConfigService = clusterConfigService;
        this.txnLogService = txnLogService;
        this.sourceService = sourceService;
        this.objectMapper = objectMapper;
    }

    public Optional<OpAmpAuthContext> authenticate(String authHeader, OpAmpAuthContext.Transport transport) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        final String token = authHeader.substring(7);

        final String typ = extractTypHeader(token);
        if (typ == null) {
            LOG.warn("Token missing ctt header");
            return Optional.empty();
        }

        return switch (typ) {
            case "enrollment" -> enrollmentTokenService.validateToken(token, transport).map(e -> e);
            case "agent" -> enrollmentTokenService.validateAgentToken(token, transport).map(i -> i);
            default -> {
                LOG.warn("Unknown token type: {}", typ);
                yield Optional.empty();
            }
        };
    }

    // TODO: Replace with proper JWT library parsing or pull into key locator
    private String extractTypHeader(String token) {
        try {
            final String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            final String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            // Simple extraction - find "typ":"value"
            final int typIndex = headerJson.indexOf("\"ctt\"");
            if (typIndex < 0) {
                return null;
            }
            final int colonIndex = headerJson.indexOf(':', typIndex);
            final int startQuote = headerJson.indexOf('"', colonIndex);
            final int endQuote = headerJson.indexOf('"', startQuote + 1);
            if (startQuote < 0 || endQuote < 0) {
                return null;
            }
            return headerJson.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            LOG.warn("Failed to extract typ header: {}", e.getMessage());
            return null;
        }
    }

    public ServerToAgent handleMessage(AgentToServer message, OpAmpAuthContext authContext) {
        return switch (authContext) {
            case OpAmpAuthContext.Enrollment e -> handleEnrollment(message, e);
            case OpAmpAuthContext.Identified i -> handleIdentifiedMessage(message, i);
        };
    }

    private ServerToAgent handleEnrollment(AgentToServer message, OpAmpAuthContext.Enrollment auth) {
        final String instanceUid = bytesToUuidString(message.getInstanceUid().toByteArray());

        // 1. Reject if already enrolled
        if (collectorInstanceService.existsByInstanceUid(instanceUid)) {
            LOG.warn("Rejecting enrollment: collector {} already enrolled", instanceUid);
            return ServerToAgent.newBuilder().setInstanceUid(message.getInstanceUid()).setErrorResponse(ServerErrorResponse.newBuilder().setErrorMessage("Collector already enrolled")).build();
        }

        // 2. Extract CSR
        if (!message.hasConnectionSettingsRequest() || !message.getConnectionSettingsRequest().hasOpamp() || !message.getConnectionSettingsRequest().getOpamp().hasCertificateRequest()) {
            return errorResponse(message, "Missing CSR in enrollment request");
        }

        final ByteString csrBytes = message.getConnectionSettingsRequest().getOpamp().getCertificateRequest().getCsr();
        if (csrBytes.isEmpty()) {
            return errorResponse(message, "Empty CSR");
        }

        try {
            // 3. Sign CSR with OpAMP CA
            final CertificateEntry enrollmentCa = opAmpCaService.getOpAmpCa();
            final X509Certificate agentCert = certificateService.builder().signCsr(csrBytes.toByteArray(), enrollmentCa, instanceUid, Duration.ofDays(365));

            // 4. Save agent record
            final String fingerprint = PemUtils.computeFingerprint(agentCert);
            final String certPem = PemUtils.toPem(agentCert);

            final CollectorInstanceDTO enroll = collectorInstanceService.enroll(instanceUid, auth.fleetId(), fingerprint, certPem, enrollmentCa.id(), Instant.now());
            LOG.info("[{}/{}] Enrolled collector in fleet {}", enroll.instanceUid(), enroll.messageSeqNum(), enroll.fleetId());

            // 5. Return certificate and connection settings
            final var connectionSettingsBuilder = ConnectionSettingsOffers.newBuilder().setOpamp(OpAMPConnectionSettings.newBuilder().setHeartbeatIntervalSeconds(30).setCertificate(TLSCertificate.newBuilder().setCert(ByteString.copyFromUtf8(certPem))));

            // Add OTLP connection settings from CollectorsConfig
            final var collectorsConfig = clusterConfigService.get(CollectorsConfig.class);
            if (collectorsConfig != null) {
                buildOtlpConnectionSettings(connectionSettingsBuilder, collectorsConfig, opAmpCaService, clusterConfigService);
            }

            return ServerToAgent.newBuilder().setInstanceUid(message.getInstanceUid()).setConnectionSettings(connectionSettingsBuilder).build();
        } catch (Exception e) {
            LOG.error("Enrollment failed for collector {}", instanceUid, e);
            return errorResponse(message, "Enrollment failed: " + e.getMessage());
        }
    }

    static void buildOtlpConnectionSettings(ConnectionSettingsOffers.Builder builder, CollectorsConfig config, OpAmpCaService opAmpCaService, ClusterConfigService clusterConfigService) {
        try {
            final CertificateEntry opAmpCa = opAmpCaService.getOpAmpCa();
            final String caPem = opAmpCa.certificate();

            final Opamp.TLSConnectionSettings tlsSettings = Opamp.TLSConnectionSettings.newBuilder().setCaPemContents(caPem).build();

            final ClusterId clusterId = clusterConfigService.get(ClusterId.class);
            final String serverName = (clusterId != null && clusterId.clusterId() != null) ? clusterId.clusterId() : "";

            if (config.http() != null && config.http().enabled()) {
                final var settingsBuilder = Opamp.OtherConnectionSettings.newBuilder().setDestinationEndpoint(f("https://%s:%d", config.http().hostname(), config.http().port())).setTls(tlsSettings);
                if (!serverName.isEmpty()) {
                    settingsBuilder.putOtherSettings("server_name", serverName);
                }
                builder.putOtherConnections("otlp-http", settingsBuilder.build());
            }
            if (config.grpc() != null && config.grpc().enabled()) {
                final var settingsBuilder = Opamp.OtherConnectionSettings.newBuilder().setDestinationEndpoint(f("https://%s:%d", config.grpc().hostname(), config.grpc().port())).setTls(tlsSettings);
                if (!serverName.isEmpty()) {
                    settingsBuilder.putOtherSettings("server_name", serverName);
                }
                builder.putOtherConnections("otlp-grpc", settingsBuilder.build());
            }
        } catch (Exception e) {
            LOG.warn("Failed to add OTLP connection settings to enrollment response", e);
        }
    }

    private ServerToAgent handleIdentifiedMessage(AgentToServer message, OpAmpAuthContext.Identified auth) {
        final String instanceUid = bytesToUuidString(message.getInstanceUid().toByteArray());

        // payload and authentication context uids must match
        if (!Objects.equals(instanceUid, auth.instanceUid())) {
            return errorResponse(message, "Invalid instanceUid");
        }
        final long sequenceNum = message.getSequenceNum();
        LOG.debug("[{}/{}] Handling OpAMP message from agent: {}", instanceUid, sequenceNum, message);


        final CollectorInstanceReport.Builder updateBuilder = CollectorInstanceReport.builder().instanceUid(instanceUid).messageSeqNum(sequenceNum).capabilities(message.getCapabilities());

        final EnumSet<Opamp.AgentCapabilities> agentCapabilities = fromBitmask(message.getCapabilities());
        for (Opamp.AgentCapabilities cap : agentCapabilities) {
            switch (cap) {
                case AgentCapabilities_ReportsStatus -> {
                    // this capability is always present, but agentDescription is only set when:
                    // 1. this is the first message this agent sends
                    // 2. any of its values change
                    // Otherwise it's "compressed", which means identical values aren't sent again
                    if (message.hasAgentDescription()) {
                        final Opamp.AgentDescription agentDescription = message.getAgentDescription();
                        LOG.debug("[{}/{}] {}", instanceUid, sequenceNum, agentDescription);

                        updateBuilder.identifyingAttributes(extractAttributes(instanceUid, sequenceNum, agentDescription.getIdentifyingAttributesList()));
                        updateBuilder.nonIdentifyingAttributes(extractAttributes(instanceUid, sequenceNum, agentDescription.getNonIdentifyingAttributesList()));
                    }
                }
                case AgentCapabilities_ReportsHealth -> {
                    if (message.hasHealth()) {
                        LOG.debug("[{}/{}] {}", instanceUid, sequenceNum, message.getHealth());
                    }
                }
                case AgentCapabilities_ReportsHeartbeat -> {
                    // TODO do we care? heartbeats are just normal messages that come at least every x interval unless
                    // there are other messages that reset the timer
                }
                case AgentCapabilities_AcceptsRemoteConfig -> {
                    // TODO determine whether applicable config has changed for this agent and include updated config in our response
                }
                default -> LOG.debug("[{}/{}] Ignoring capability of {}", instanceUid, sequenceNum, cap);
            }
        }

        // let's save the report and load the previously known values for the important properties
        // previousState is not the entire document, but the minimal version to avoid high deserialization cost
        final Optional<CollectorInstanceService.MinimalCollectorInstanceDTO> previousState = collectorInstanceService.createOrUpdateFromReport(updateBuilder.build());
        long previousSeqNum = 0L;
        if (previousState.isPresent()) {
            previousSeqNum = previousState.get().messageSeqNum();

        }

        // determine our response
        final ServerToAgent.Builder responseBuilder = ServerToAgent.newBuilder().setCapabilities(Opamp.ServerCapabilities.ServerCapabilities_AcceptsStatus_VALUE).setInstanceUid(message.getInstanceUid());

        LOG.debug("[{}/{}] previously seen sequence number {}", instanceUid, sequenceNum, previousSeqNum);
        if ((previousSeqNum == 0L) || ((previousSeqNum + 1) != sequenceNum)) {
            // either we haven't seen messages from this agent before (which means we've just started)
            // or the sequence numbers aren't consecutive, which means we have missed one or more messages.
            // in either case we need to request a full state report
            responseBuilder.setFlags(Opamp.ServerToAgentFlags.ServerToAgentFlags_ReportFullState_VALUE);
            LOG.debug("[{}/{}] Non-consecutive sequence detected, requesting full state report from this agent.", instanceUid, sequenceNum);
        } else {
            long lastProcessedTxnSeq = previousState.map(CollectorInstanceService.MinimalCollectorInstanceDTO::lastProcessTxnSeq).orElse(0L);
            final String fleetId = previousState.map(CollectorInstanceService.MinimalCollectorInstanceDTO::fleetId).orElse(null);

            final List<TransactionMarker> unprocessedMarkers = txnLogService.getUnprocessedMarkers(fleetId, instanceUid, lastProcessedTxnSeq);
            final CoalescedActions coalesced = txnLogService.coalesce(unprocessedMarkers);
            LOG.debug("[{}/{}] {} unprocessed markers for this collector (last processed tnx id {}) coalesced to {}", instanceUid, sequenceNum, unprocessedMarkers.size(), lastProcessedTxnSeq, coalesced);

            if (coalesced.recomputeConfig()) {
                final var effectiveFleetId = (coalesced.newFleetId() == null) ? fleetId : coalesced.newFleetId();
                LOG.debug("[{}/{}] Computing new collector config for fleet id {}", instanceUid, sequenceNum, effectiveFleetId);

                final Opamp.AgentConfigMap.Builder configMapBuilder = Opamp.AgentConfigMap.newBuilder();
                try (final var sources = sourceService.streamAllByFleet(effectiveFleetId)) {
                    sources.forEach(sourceDTO -> {
                        try {
                            configMapBuilder.putConfigMap(sourceDTO.id(),
                                    Opamp.AgentConfigFile.newBuilder()
                                            .setContentType(MediaType.APPLICATION_JSON)
                                            .setBody(ByteString.copyFromUtf8(objectMapper.writeValueAsString(sourceDTO)))
                                            .build()
                            );
                        } catch (JsonProcessingException e) {
                            LOG.error("[{}/{}] Unable to serialize source configuration: {}", instanceUid, sequenceNum, sourceDTO.name(), e);
                        }
                    });
                }

                responseBuilder.setRemoteConfig(Opamp.AgentRemoteConfig.newBuilder()
                        .setConfig(configMapBuilder)
                        .setConfigHash(ByteString.copyFromUtf8("TODO")).build() // TODO consistent hashing
                );
            }
            if (coalesced.restart()) {
                LOG.debug("[{}/{}] Scheduled restart", instanceUid, sequenceNum);
                responseBuilder.setCommand(Opamp.ServerToAgentCommand.newBuilder().setType(Opamp.CommandType.CommandType_Restart).build());
            }
            if (coalesced.runDiscovery()) {
                LOG.debug("[{}/{}] Scheduled discovery run", instanceUid, sequenceNum);
                // TODO we don't have discovery yet, this is probably a custom message in the future
            }
        }

        return responseBuilder.build();
    }

    @Nonnull
    private static List<Attribute> extractAttributes(String instanceUid, long sequenceNum, List<Anyvalue.KeyValue> attributesList) {
        final List<Attribute> attributes = Lists.newArrayListWithExpectedSize(attributesList.size());
        for (final Anyvalue.KeyValue keyValue : attributesList) {
            final Anyvalue.AnyValue value = keyValue.getValue();
            LOG.debug("[{}/{}] {} = {}", instanceUid, sequenceNum, keyValue.getKey(), value);

            switch (value.getValueCase()) {
                case STRING_VALUE -> attributes.add(Attribute.of(keyValue.getKey(), value.getStringValue()));
                case BOOL_VALUE -> attributes.add(Attribute.of(keyValue.getKey(), value.getBoolValue()));
                case INT_VALUE -> attributes.add(Attribute.of(keyValue.getKey(), value.getIntValue()));
                case DOUBLE_VALUE -> attributes.add(Attribute.of(keyValue.getKey(), value.getDoubleValue()));
                case BYTES_VALUE -> // TODO does this make sense for us?
                        attributes.add(Attribute.of(keyValue.getKey(), value.getBytesValue()));
                case ARRAY_VALUE, KVLIST_VALUE ->
                        LOG.error("[{}/{}] Unsupported value type for identifying attributes, must be a scalar type but is {}", instanceUid, sequenceNum, value.getValueCase());
                case VALUE_NOT_SET ->
                        LOG.error("[{}/{}] Unsupported value type for identifying attributes, the value is undefined", instanceUid, sequenceNum);
            }
        }
        return attributes;
    }

    private static EnumSet<Opamp.AgentCapabilities> fromBitmask(long capabilities) {
        return Arrays.stream(Opamp.AgentCapabilities.values()).filter(c -> c != Opamp.AgentCapabilities.UNRECOGNIZED).filter(c -> (capabilities & c.getNumber()) != 0).collect(toCollection(() -> EnumSet.noneOf(Opamp.AgentCapabilities.class)));
    }

    private ServerToAgent errorResponse(AgentToServer message, String errorMessage) {
        return ServerToAgent.newBuilder().setInstanceUid(message.getInstanceUid()).setErrorResponse(ServerErrorResponse.newBuilder().setErrorMessage(errorMessage)).build();
    }

    private static String bytesToUuidString(byte[] bytes) {
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong()).toString();
    }
}
