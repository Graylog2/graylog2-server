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
package org.graylog.collectors.opamp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.auto.value.AutoValue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import okhttp3.HttpUrl;
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
import org.graylog.collectors.config.CollectorConfig;
import org.graylog.collectors.config.CollectorPipelineConfig;
import org.graylog.collectors.config.CollectorServiceConfig;
import org.graylog.collectors.config.TLSConfigurationSettings;
import org.graylog.collectors.config.exporter.OtlpExporterConfig;
import org.graylog.collectors.config.exporter.OtlpGrpcExporterConfig;
import org.graylog.collectors.config.exporter.OtlpHttpExporterConfig;
import org.graylog.collectors.config.processor.CollectorProcessorConfig;
import org.graylog.collectors.config.processor.ResourceProcessorConfig;
import org.graylog.collectors.config.receiver.CollectorReceiverConfig;
import org.graylog.collectors.config.receiver.NoopReceiverConfig;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CoalescedActions;
import org.graylog.collectors.db.CollectorInstanceDTO;
import org.graylog.collectors.db.CollectorInstanceReport;
import org.graylog.collectors.db.SourceDTO;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog.collectors.opamp.auth.AgentTokenService;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class OpAmpService {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpService.class);
    public static final String REMOTE_CONFIG_KEY = "collector.yaml";

    private static final JsonFormat.Printer PROTO_PRINTER = JsonFormat.printer().omittingInsignificantWhitespace();

    private final EnrollmentTokenService enrollmentTokenService;
    private final AgentTokenService agentTokenService;
    private final OpAmpCaService opAmpCaService;
    private final CertificateService certificateService;
    private final CollectorInstanceService collectorInstanceService;
    private final ClusterConfigService clusterConfigService;
    private final FleetTransactionLogService txnLogService;
    private final SourceService sourceService;
    private final ObjectMapper yamlObjectMapper;

    @Inject
    public OpAmpService(EnrollmentTokenService enrollmentTokenService,
                        AgentTokenService agentTokenService,
                        OpAmpCaService opAmpCaService,
                        CertificateService certificateService,
                        CollectorInstanceService collectorInstanceService,
                        ClusterConfigService clusterConfigService,
                        FleetTransactionLogService txnLogService,
                        SourceService sourceService) {
        this.enrollmentTokenService = enrollmentTokenService;
        this.agentTokenService = agentTokenService;
        this.opAmpCaService = opAmpCaService;
        this.certificateService = certificateService;
        this.collectorInstanceService = collectorInstanceService;
        this.clusterConfigService = clusterConfigService;
        this.txnLogService = txnLogService;
        this.sourceService = sourceService;
        this.yamlObjectMapper = new ObjectMapper(new YAMLFactory()
                .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE))
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .registerModule(new Jdk8Module());
    }

    /**
     * Converts the given protobuf message to a JSON string.
     *
     * @param message the message to convert
     * @return the JSON string or the #toString() result if the JSON converter fails
     */
    private static String toProtoString(GeneratedMessageV3 message) {
        try {
            return PROTO_PRINTER.print(message);
        } catch (Exception e) {
            return message.toString();
        }
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
            case "agent" -> agentTokenService.validateAgentToken(token, transport).map(i -> i);
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

    @WithSpan
    public ServerToAgent handleMessage(AgentToServer message, OpAmpAuthContext authContext) {
        return switch (authContext) {
            case OpAmpAuthContext.Enrollment e -> handleEnrollment(message, e);
            case OpAmpAuthContext.Identified i -> handleIdentifiedMessage(message, i);
        };
    }

    @WithSpan
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
                buildOtherConnectionSettings(connectionSettingsBuilder, collectorsConfig, opAmpCaService, clusterConfigService);
            }

            return ServerToAgent.newBuilder().setInstanceUid(message.getInstanceUid()).setConnectionSettings(connectionSettingsBuilder).build();
        } catch (Exception e) {
            LOG.error("Enrollment failed for collector {}", instanceUid, e);
            return errorResponse(message, "Enrollment failed: " + e.getMessage());
        }
    }

    // TODO: Do we still need the "other_settings" now that we have the "own_logs" settings in place?
    static void buildOtherConnectionSettings(ConnectionSettingsOffers.Builder builder, CollectorsConfig config, OpAmpCaService opAmpCaService, ClusterConfigService clusterConfigService) {
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

    static void buildConnectionSettings(ServerToAgent.Builder builder, @Nullable OtlpExporterConfig exporterConfig) {
        if (exporterConfig == null) {
            builder.setConnectionSettings(Opamp.ConnectionSettingsOffers.newBuilder()
                    .setOwnLogs(Opamp.TelemetryConnectionSettings.newBuilder()
                            .setDestinationEndpoint(""))); // Empty signals: don't send logs
            return;
        }

        final var caCert = exporterConfig.tls().caPem().orElseThrow();
        final var minVersion = exporterConfig.tls().minVersion();
        final var maxVersion = exporterConfig.tls().maxVersion().orElse("");
        final var serverName = exporterConfig.tls().serverNameOverride();

        final var endpoint = requireNonNull(HttpUrl.parse(exporterConfig.endpoint()), "endpoint is null")
                .newBuilder()
                // We pass settings that we can't represent in OpAMP protobuf messages as endpoint URL params.
                .addQueryParameter("tls_server_name", serverName)
                .addQueryParameter("log_level", "info") // TODO: Make log level configurable
                .build()
                .toString();

        builder.setConnectionSettings(Opamp.ConnectionSettingsOffers.newBuilder()
                .setOwnLogs(Opamp.TelemetryConnectionSettings.newBuilder()
                        .setDestinationEndpoint(endpoint)
                        .setTls(Opamp.TLSConnectionSettings.newBuilder()
                                .setMinVersion(minVersion)
                                .setMaxVersion(maxVersion)
                                .setCaPemContents(caCert))));
    }

    @WithSpan
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
                        LOG.atDebug().setMessage("[{}/{}] Health update: {}")
                                .addArgument(instanceUid).addArgument(sequenceNum)
                                .addArgument(() -> toProtoString(message.getHealth()))
                                .log();
                    }
                }
                case AgentCapabilities_ReportsHeartbeat -> {
                    // TODO do we care? heartbeats are just normal messages that come at least every x interval unless
                    // there are other messages that reset the timer
                }
                case AgentCapabilities_AcceptsRemoteConfig -> {
                    // TODO determine whether applicable config has changed for this agent and include updated config in our response
                }
                case AgentCapabilities_ReportsRemoteConfig ->
                        handleRemoteConfig(instanceUid, sequenceNum, message, updateBuilder);
                default -> LOG.debug("[{}/{}] Ignoring capability of {}", instanceUid, sequenceNum, cap);
            }
        }

        // let's save the report and load the previously known values for the important properties
        // previousState is not the entire document, but the minimal version to avoid high deserialization cost
        final Optional<CollectorInstanceService.MinimalCollectorInstanceDTO> previousState = collectorInstanceService.createOrUpdateFromReport(updateBuilder.build());
        final boolean seqConsecutive = previousState
                .filter(prevState -> (prevState.messageSeqNum() + 1) == sequenceNum)
                .isPresent();

        // determine our response
        final ServerToAgent.Builder responseBuilder = ServerToAgent.newBuilder()
                .setCapabilities(Opamp.ServerCapabilities.ServerCapabilities_AcceptsStatus_VALUE)
                .setInstanceUid(message.getInstanceUid());

        LOG.debug("[{}/{}] previously seen state {} - consecutive: {}", instanceUid, sequenceNum, previousState, seqConsecutive);
        if (!seqConsecutive) {
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
            LOG.debug("[{}/{}] {} unprocessed markers for this collector (last processed tnx id {}) coalesced to {}",
                    instanceUid, sequenceNum, unprocessedMarkers.size(), lastProcessedTxnSeq, coalesced);

            // do this first. in case there's no configured endpoint we don't have to perform the more expensive stuff
            final ExporterConfigs exporterConfigs = getExporterConfigs();

            if (coalesced.recomputeIngestConfig()) {
                // The connection settings should only be sent when they change. Not having a config is a change, too.
                if (agentCapabilities.contains(Opamp.AgentCapabilities.AgentCapabilities_ReportsOwnLogs)) {
                    // The "own_logs" are always transmitted via HTTP according to OpAMP.
                    buildConnectionSettings(responseBuilder, exporterConfigs.httpConfig().orElse(null));
                }
            }

            final var configBuilder = CollectorConfig.builder();
            if (coalesced.recomputeConfig() || coalesced.recomputeIngestConfig()) {
                final var effectiveEndpoint = exporterConfigs.getDefault().orElseThrow();
                final var effectiveFleetId = (coalesced.newFleetId() == null) ? fleetId : coalesced.newFleetId();
                LOG.debug("[{}/{}] Computing new collector config for fleet id {}", instanceUid, sequenceNum, effectiveFleetId);

                final Map<String, CollectorReceiverConfig> receiverConfigs = Maps.newHashMap();
                try (final var sources = sourceService.streamAllByFleet(effectiveFleetId)) {
                    sources.map(SourceDTO::toReceiverConfig)
                            .flatMap(Optional::stream)
                            .forEach(receiverConfig -> receiverConfigs.put(receiverConfig.name(), receiverConfig));
                }

                // The Collector must at least have one receiver to avoid a startup error.
                if (receiverConfigs.isEmpty()) {
                    final var noop = NoopReceiverConfig.instance();
                    receiverConfigs.put(noop.name(), noop);
                }

                final var receiverGroups = receiverConfigs.values().stream()
                        .collect(Collectors.groupingBy(CollectorReceiverConfig::type));

                configBuilder.receivers(receiverConfigs);
                configBuilder.exporters(Map.of(effectiveEndpoint.getName(), effectiveEndpoint));

                final Map<String, CollectorProcessorConfig> receiverProcessors = receiverGroups.keySet().stream()
                        .map(component -> ResourceProcessorConfig.builder(component)
                                .attributes(List.of(ResourceProcessorConfig.collectorComponentAttribute(component)))
                                .build())
                        .collect(Collectors.toMap(CollectorProcessorConfig::name, Function.identity()));

                configBuilder.processors(receiverProcessors);

                final var pipelines = receiverGroups.entrySet().stream()
                        .collect(Collectors.toMap(e -> f("logs/%s", e.getKey()), e -> CollectorPipelineConfig.builder()
                                .receivers(e.getValue().stream().map(CollectorReceiverConfig::name).collect(Collectors.toSet()))
                                .exporters(Set.of(effectiveEndpoint.getName()))
                                .processors(receiverProcessors.values().stream()
                                        .filter(config -> e.getKey().equals(config.id()))
                                        .map(CollectorProcessorConfig::name)
                                        .collect(Collectors.toSet()))
                                .build()));

                configBuilder.service(CollectorServiceConfig.builder()
                        .pipelines(pipelines)
                        .build());
                try {
                    final String configYaml = yamlObjectMapper.writeValueAsString(configBuilder.build());

                    responseBuilder.setRemoteConfig(Opamp.AgentRemoteConfig.newBuilder()
                            .setConfig(Opamp.AgentConfigMap.newBuilder()
                                    .putConfigMap(REMOTE_CONFIG_KEY, Opamp.AgentConfigFile.newBuilder()
                                            .setContentType("application/yaml")
                                            .setBody(ByteString.copyFromUtf8(configYaml))
                                            .build())
                                    .build())
                            .setConfigHash(ByteString.copyFromUtf8(String.valueOf(coalesced.maxSeq()))).build() // TODO consistent hashing
                    );
                } catch (JsonProcessingException e) {
                    LOG.error("[{}/{}] Remote config could not be serialized", instanceUid, sequenceNum, e);
                }
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

    @WithSpan
    private void handleRemoteConfig(String instanceUid,
                                    long sequenceNum,
                                    AgentToServer message,
                                    CollectorInstanceReport.Builder updateBuilder) {
        if (!message.hasRemoteConfigStatus()) {
            return;
        }

        final var logEvent = LOG.atDebug()
                .setMessage("[{}/{}] Remote config status {}: {}")
                .addArgument(instanceUid)
                .addArgument(sequenceNum);

        final var status = message.getRemoteConfigStatus();
        switch (status.getStatus()) {
            case RemoteConfigStatuses_APPLIED -> {
                try {
                    final var hashString = status.getLastRemoteConfigHash().toStringUtf8();
                    if (isNotBlank(hashString)) {
                        logEvent.addArgument("APPLIED").addArgument(() -> toProtoString(status)).log();
                        final var txnSeq = Longs.tryParse(hashString);
                        if (txnSeq != null) {
                            updateBuilder.lastProcessedTxnSeq(txnSeq);
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to get last remote config hash from status", e);
                }
            }
            case RemoteConfigStatuses_APPLYING -> {
                logEvent.addArgument("APPLYING").addArgument(() -> toProtoString(status)).log();
            }
            case RemoteConfigStatuses_FAILED -> {
                logEvent.addArgument("FAILED").addArgument(() -> toProtoString(status)).log();
                // TODO: Store error message in instance document
            }
            case RemoteConfigStatuses_UNSET -> {
                logEvent.addArgument("UNSET").addArgument(() -> toProtoString(status)).log();
            }
        }
    }

    @AutoValue
    abstract static class ExporterConfigs {
        abstract Optional<OtlpExporterConfig> grpcConfig();

        abstract Optional<OtlpExporterConfig> httpConfig();

        /**
         * Returns the default exporter config. Prefers gRPC to HTTP.
         */
        Optional<OtlpExporterConfig> getDefault() {
            return grpcConfig().or(this::httpConfig);
        }

        boolean isEmpty() {
            return grpcConfig().isEmpty() && httpConfig().isEmpty();
        }

        static Builder builder() {
            return new AutoValue_OpAmpService_ExporterConfigs.Builder();
        }

        @AutoValue.Builder
        abstract static class Builder {
            abstract Builder grpcConfig(OtlpExporterConfig grpcConfig);

            abstract Builder httpConfig(OtlpExporterConfig httpConfig);

            abstract ExporterConfigs build();
        }
    }

    @Nonnull
    private ExporterConfigs getExporterConfigs() {
        final CollectorsConfig collectorsConfig = clusterConfigService.get(CollectorsConfig.class);
        if (collectorsConfig == null) {
            throw new IllegalStateException("Unable to determine collector input config, cannot send remote config.");
        }
        var httpEndpoint = collectorsConfig.http();
        var grpcEndpoint = collectorsConfig.grpc();
        if (httpEndpoint == null && grpcEndpoint == null) {
            throw new IllegalStateException("No collector input configured, cannot send remote config.");
        }

        final var clusterId = clusterConfigService.get(ClusterId.class);
        final var caCert = opAmpCaService.getOpAmpCa().certificate();
        final var tlsSettings = TLSConfigurationSettings.withCACert(clusterId.clusterId(), caCert);
        final var builder = ExporterConfigs.builder();

        if (grpcEndpoint != null && grpcEndpoint.enabled()) {
            builder.grpcConfig(OtlpGrpcExporterConfig.builder()
                    .endpoint(f("%s:%s", grpcEndpoint.hostname(), grpcEndpoint.port()))
                    .tls(tlsSettings)
                    .build());
        }
        if (httpEndpoint != null && httpEndpoint.enabled()) {
            builder.httpConfig(OtlpHttpExporterConfig.builder()
                    .endpoint(f("https://%s:%s", httpEndpoint.hostname(), httpEndpoint.port()))
                    .tls(tlsSettings)
                    .build());
        }

        return builder.build();
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
