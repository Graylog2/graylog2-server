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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.annotation.Nonnull;
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
import org.graylog.collectors.CollectorCaService;
import org.graylog.collectors.CollectorInstanceService;
import org.graylog.collectors.CollectorOSType;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.SourceService;
import org.graylog.collectors.config.CollectorAttributes;
import org.graylog.collectors.config.CollectorConfig;
import org.graylog.collectors.config.CollectorPipelineConfig;
import org.graylog.collectors.config.CollectorServiceConfig;
import org.graylog.collectors.config.TLSConfigurationSettings;
import org.graylog.collectors.config.exporter.OtlpExporterConfig;
import org.graylog.collectors.config.exporter.OtlpHttpExporterConfig;
import org.graylog.collectors.config.extension.FileStorageExtensionConfig;
import org.graylog.collectors.config.processor.CollectorProcessorConfig;
import org.graylog.collectors.config.processor.ResourceProcessorConfig;
import org.graylog.collectors.config.receiver.CollectorReceiverConfig;
import org.graylog.collectors.config.receiver.NoopReceiverConfig;
import org.graylog.collectors.db.Attribute;
import org.graylog.collectors.db.CoalescedActions;
import org.graylog.collectors.db.CollectorInstanceReport;
import org.graylog.collectors.db.SourceDTO;
import org.graylog.collectors.db.TransactionMarker;
import org.graylog.collectors.opamp.auth.AgentTokenService;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog.collectors.opamp.transport.OpAmpAuthContext;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.graylog.collectors.config.processor.ResourceProcessorConfig.Attribute.upsert;
import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class OpAmpService {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpService.class);
    public static final String REMOTE_CONFIG_KEY = "collector.yaml";

    private static final JsonFormat.Printer PROTO_PRINTER = JsonFormat.printer().omittingInsignificantWhitespace();

    private final EnrollmentTokenService enrollmentTokenService;
    private final AgentTokenService agentTokenService;
    private final CollectorCaService collectorCaService;
    private final CertificateService certificateService;
    private final CollectorInstanceService collectorInstanceService;
    private final CollectorsConfigService collectorsConfigService;
    private final ClusterIdService clusterIdService;
    private final FleetTransactionLogService txnLogService;
    private final SourceService sourceService;
    private final ObjectMapper yamlObjectMapper;

    @Inject
    public OpAmpService(EnrollmentTokenService enrollmentTokenService,
                        AgentTokenService agentTokenService,
                        CollectorCaService collectorCaService,
                        CertificateService certificateService,
                        CollectorInstanceService collectorInstanceService,
                        CollectorsConfigService collectorsConfigService,
                        ClusterIdService clusterIdService,
                        FleetTransactionLogService txnLogService,
                        SourceService sourceService) {
        this.enrollmentTokenService = enrollmentTokenService;
        this.agentTokenService = agentTokenService;
        this.collectorCaService = collectorCaService;
        this.certificateService = certificateService;
        this.collectorInstanceService = collectorInstanceService;
        this.collectorsConfigService = collectorsConfigService;
        this.clusterIdService = clusterIdService;
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
    private static String toProtoString(MessageOrBuilder message) {
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
            case "enrollment" -> enrollmentTokenService.validateToken(token)
                    .map(dto -> new OpAmpAuthContext.Enrollment(dto, transport));
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

    /**
     * Handles an OpAMP enrollment request.
     * <p>
     * If no record exists for the instance UID, signs the CSR and inserts a new record; the token
     * usage counter is incremented.
     * <p>
     * If a record already exists, performs a proof-of-possession check: the CSR's public key must
     * match the public key in the stored active certificate. On mismatch, the request is rejected
     * without changing any state. On match, the CSR is signed and the existing record is re-issued
     * via {@link CollectorInstanceService#reEnroll}. The token usage counter is incremented only
     * when the incoming token id differs from the stored token id, suppressing double-counts on
     * phantom-write retries by the same token; in that case only the token's {@code last_used_at}
     * timestamp is updated.
     * <p>
     * Re-enrollment is the intended recovery path for a collector that lost its certificate but
     * retained its private key — including the phantom-write scenario that motivated this design.
     *
     * @param message the enrollment AgentToServer message
     * @param auth    the enrollment-token-authenticated context
     * @return the ServerToAgent response carrying the issued certificate or an error
     */
    @WithSpan
    private ServerToAgent handleEnrollment(AgentToServer message, OpAmpAuthContext.Enrollment auth) {
        final String instanceUid = bytesToUuidString(message.getInstanceUid().toByteArray());

        try {
            final var collectorConfig = collectorsConfigService.getOrDefault();

            final var csrPem = getCsr(message);
            if (csrPem.isEmpty()) {
                return errorResponse(message, "Missing CSR in enrollment request");
            }

            // CSR parsing also includes verification. If parsing succeeds, the collector has proven that it possesses
            // the private key for the contained public key.
            final var csr = PemUtils.parseCsr(csrPem.get());

            // Check if we need to prohibit re-enrollment. We only allow re-enrollment for collectors when their
            // keypair has not changed.
            final var existingInstance = collectorInstanceService.findByInstanceUid(instanceUid);
            if (existingInstance.isPresent()) {
                final var publicKeyFromExistingCollector = PemUtils.parseCertificate(
                        existingInstance.get().activeCertificatePem()).getPublicKey();
                final var publicKeyFromCSR = csr.publicKey();
                if (!Arrays.equals(publicKeyFromExistingCollector.getEncoded(), publicKeyFromCSR.getEncoded())) {
                    LOG.warn("Rejecting re-enrollment for collector {}: CSR public key does not match public key in " +
                            "stored certificate", instanceUid);
                    return errorResponse(message, "Enrollment rejected.");
                }
            }

            // Sign CSR with OpAMP CA
            final CertificateEntry issuerCert = collectorCaService.getSigningCert();
            final X509Certificate agentCert = certificateService.builder().signCsr(csr, issuerCert, instanceUid,
                    collectorConfig.collectorCertLifetime());
            final var issuedCert = IssuedCertificate.of(agentCert, issuerCert);

            // (Re-)enroll
            existingInstance.ifPresentOrElse(instance -> {
                if (!instance.fleetId().equals(auth.token().fleetId())) {
                    LOG.warn("Ignoring fleet {} from enrollment token for re-enrollment of existing collector {}. " +
                                    "Current assignment to fleet {} will be kept.", auth.token().fleetId(),
                            instance.instanceUid(), instance.fleetId());
                }
                final var enrolled = collectorInstanceService.reEnroll(instance.id(),
                        instance.activeCertificateFingerprint(), issuedCert, auth.token().id());
                LOG.info("Re-enrolled existing collector {}. Current fleet: {}", enrolled.instanceUid(),
                        enrolled.fleetId());
                // Don't count token usage for consecutive enrollments of the same collector, but
                // still bump last_used_at so the token doesn't look dormant while a collector
                // depends on it for recovery.
                if (!Objects.equals(auth.token().id(), instance.enrollmentTokenId())) {
                    enrollmentTokenService.incrementUsage(auth.token().id());
                } else {
                    enrollmentTokenService.markUsed(auth.token().id());
                }
            }, () -> {
                final var enrolled = collectorInstanceService.enroll(instanceUid, auth.token().fleetId(), issuedCert,
                        auth.token().id());
                LOG.info("Enrolled collector {}. Assigned fleet: {}", enrolled.instanceUid(), enrolled.fleetId());
                enrollmentTokenService.incrementUsage(auth.token().id());
            });

            // Return certificate and connection settings
            final var connectionSettingsBuilder = ConnectionSettingsOffers.newBuilder();
            setOpampConnectionSettings(connectionSettingsBuilder, issuedCert.certPem(), collectorConfig.collectorHeartbeatInterval());

            return serverToAgentBuilder(message)
                    .setConnectionSettings(connectionSettingsBuilder)
                    .build();
        } catch (Exception e) {
            LOG.error("Enrollment failed for collector {}", instanceUid, e);
            return errorResponse(message, "Enrollment failed");
        }
    }

    private void setOpampConnectionSettings(ConnectionSettingsOffers.Builder connectionSettingsBuilder, String certPem, Duration heartbeatInterval) {
        connectionSettingsBuilder.setOpamp(OpAMPConnectionSettings.newBuilder()
                .setHeartbeatIntervalSeconds(heartbeatInterval.toSeconds())
                .setCertificate(TLSCertificate.newBuilder().setCert(ByteString.copyFromUtf8(certPem))));
    }

    /**
     * Extracts the PEM-encoded CSR from the OpAMP message.
     * <p>
     * The OpAMP protobuf carries the CSR as opaque {@code bytes}; Graylog's collectors transmit it
     * as PEM, and this method returns the UTF-8 decoded string for downstream parsing by
     * {@link PemUtils#parseCsr(String)}.
     *
     * @param message the OpAMP AgentToServer message
     * @return the PEM-encoded CSR, or empty if the message does not carry a CSR
     */
    private Optional<String> getCsr(AgentToServer message) {
        if (!message.hasConnectionSettingsRequest()
                || !message.getConnectionSettingsRequest().hasOpamp()
                || !message.getConnectionSettingsRequest().getOpamp().hasCertificateRequest()) {
            return Optional.empty();
        }

        final var csrPem = message.getConnectionSettingsRequest()
                .getOpamp()
                .getCertificateRequest()
                .getCsr()
                .toStringUtf8();

        if (csrPem.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(csrPem);
    }

    static void buildConnectionSettings(ServerToAgent.Builder builder, OtlpExporterConfig exporterConfig) {
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

        builder.setConnectionSettings(ConnectionSettingsOffers.newBuilder()
                .setOwnLogs(Opamp.TelemetryConnectionSettings.newBuilder()
                        .setDestinationEndpoint(endpoint)
                        .setTls(Opamp.TLSConnectionSettings.newBuilder()
                                .setMinVersion(minVersion)
                                .setMaxVersion(maxVersion)
                                .setCaPemContents(caCert))));
    }

    /**
     * Builds the OTel Collector config that we push down to an agent.
     * <p>
     * For each enabled source whose receiver supports the agent's OS, creates: a receiver (named
     * {@code <receiverType>/<sourceId>}), a resource processor (named {@code resource/<sourceId>})
     * that stamps {@code collector.fleet.id}, {@code collector.source.id}, and
     * {@code collector.receiver.type}, and a pipeline (named {@code logs/<sourceId>}) wiring the
     * receiver to the processor and the shared exporter.
     * <p>
     * If no sources survive filtering, falls back to a single no-op receiver with a minimal
     * pipeline so the Collector passes startup validation.
     */
    @VisibleForTesting
    static CollectorConfig buildCollectorConfig(String fleetId,
                                                List<SourceDTO> sources,
                                                CollectorOSType osType,
                                                OtlpExporterConfig exporterConfig) {
        final Map<String, CollectorReceiverConfig> receiverConfigs = Maps.newHashMap();
        final Map<String, CollectorProcessorConfig> processorConfigs = Maps.newHashMap();
        final Map<String, CollectorPipelineConfig> pipelines = Maps.newHashMap();

        sources.stream().filter(SourceDTO::enabled).forEach(source -> {
            final var receiverConfig = source.toReceiverConfig()
                    .filter(config -> config.osSupport().contains(osType))
                    .orElse(null);

            if (receiverConfig == null) {
                return;
            }

            receiverConfigs.put(receiverConfig.name(), receiverConfig);

            final var processorConfig = ResourceProcessorConfig.builder(source.id())
                    .attributes(List.of(
                            upsert(CollectorAttributes.COLLECTOR_RECEIVER_TYPE, receiverConfig.type()),
                            upsert(CollectorAttributes.COLLECTOR_SOURCE_ID, source.id()),
                            upsert(CollectorAttributes.COLLECTOR_FLEET_ID, fleetId)))
                    .build();
            processorConfigs.put(processorConfig.name(), processorConfig);

            pipelines.put(f("logs/%s", source.id()), CollectorPipelineConfig.builder()
                    .receivers(Set.of(receiverConfig.name()))
                    .processors(Set.of(processorConfig.name()))
                    .exporters(Set.of(exporterConfig.getName()))
                    .build());
        });

        // The Collector must at least have one receiver wired into one pipeline to avoid a startup error.
        if (pipelines.isEmpty()) {
            final var noop = NoopReceiverConfig.instance();
            receiverConfigs.put(noop.name(), noop);
            pipelines.put(f("logs/%s", noop.name()), CollectorPipelineConfig.builder()
                    .receivers(Set.of(noop.name()))
                    .exporters(Set.of(exporterConfig.getName()))
                    .build());
        }

        final var storageExtensionConfig = FileStorageExtensionConfig.defaultInstance();

        return CollectorConfig.builder()
                .extensions(Map.of(storageExtensionConfig.name(), storageExtensionConfig))
                .receivers(receiverConfigs)
                .exporters(Map.of(exporterConfig.getName(), exporterConfig))
                .processors(processorConfigs)
                .service(CollectorServiceConfig.builder()
                        .extensions(Set.of(storageExtensionConfig.name()))
                        .pipelines(pipelines)
                        .build())
                .build();
    }

    private ServerToAgent.Builder serverToAgentBuilder(AgentToServer message) {
        final var builder = ServerToAgent.newBuilder().setInstanceUid(message.getInstanceUid());

        // This field MUST be set in the first ServerToAgent sent by the Server and MAY be omitted in subsequent
        // ServerToAgent messages by setting it to UnspecifiedServerCapability value.
        if (message.getSequenceNum() < 1) {
            builder.setCapabilities(
                    Opamp.ServerCapabilities.ServerCapabilities_AcceptsStatus_VALUE
                            | Opamp.ServerCapabilities.ServerCapabilities_OffersRemoteConfig_VALUE
                            | Opamp.ServerCapabilities.ServerCapabilities_AcceptsEffectiveConfig_VALUE
                            | Opamp.ServerCapabilities.ServerCapabilities_OffersConnectionSettings_VALUE
            );
        } else {
            builder.setCapabilities(Opamp.ServerCapabilities.ServerCapabilities_Unspecified_VALUE);
        }

        return builder;
    }

    @WithSpan
    private ServerToAgent handleIdentifiedMessage(AgentToServer message, OpAmpAuthContext.Identified auth) {
        final String instanceUid = bytesToUuidString(message.getInstanceUid().toByteArray());

        if (LOG.isTraceEnabled()) {
            try {
                LOG.trace("Message from enrolled instance <{}>:\n{}", instanceUid, JsonFormat.printer().print(message));
            } catch (Exception e) {
                LOG.trace("Couldn't serialize message from instance <{}>", instanceUid, e);
            }
        }

        // payload and authentication context uids must match
        if (!Objects.equals(instanceUid, auth.instanceUid())) {
            return errorResponse(message, "Invalid instanceUid");
        }
        final long sequenceNum = message.getSequenceNum();
        LOG.debug("[{}/{}] Handling OpAMP message from collector: {}", instanceUid, sequenceNum, message);

        var appliedTxnSeq = OptionalLong.empty();
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
                    // TODO determine whether applicable config has changed for this collector and include updated config in our response
                }
                case AgentCapabilities_ReportsRemoteConfig ->
                        appliedTxnSeq = handleRemoteConfig(instanceUid, sequenceNum, message, updateBuilder);
                default -> LOG.debug("[{}/{}] Ignoring capability of {}", instanceUid, sequenceNum, cap);
            }
        }

        // let's save the report and load the previously known values for the important properties
        // previousState is not the entire document, but the minimal version to avoid high deserialization cost
        final var instanceReport = updateBuilder.build();
        final var previousState = collectorInstanceService.updateFromReport(instanceReport);
        final boolean seqConsecutive = (previousState.messageSeqNum() + 1) == sequenceNum;

        final var osType = switch (previousState.osType()) {
            // On the first report the "os.type" non-identifying attribute might not be present in the previous state.
            // We will always run into this case when a Collector from an unsupported operating system connects.
            case UNKNOWN -> CollectorInstanceService.extractOsTypeFromReport(instanceReport);
            case LINUX, MACOS, WINDOWS -> previousState.osType();
        };

        // determine our response
        final ServerToAgent.Builder responseBuilder = serverToAgentBuilder(message);

        // Don't fetch the config, we might not need it. If we need it, only get it once.
        final Supplier<CollectorsConfig> configSupplier = Suppliers.memoize(collectorsConfigService::getOrDefault);

        LOG.debug("[{}/{}] Previously seen state {} - consecutive: {}", instanceUid, sequenceNum, previousState, seqConsecutive);
        if (!seqConsecutive) {
            // either we haven't seen messages from this collector before (which means we've just started)
            // or the sequence numbers aren't consecutive, which means we have missed one or more messages.
            // in either case we need to request a full state report
            responseBuilder.setFlags(Opamp.ServerToAgentFlags.ServerToAgentFlags_ReportFullState_VALUE);
            LOG.debug("[{}/{}] Non-consecutive sequence detected, requesting full state report from this collector.", instanceUid, sequenceNum);
        } else {
            // If we would only look at the applied transaction sequence from the previousState, we would reply
            // with a config update for a remote config status APPLIED request.
            final var lastProcessedTxnSeq = requireNonNull(appliedTxnSeq, "appliedTxnSeq is null")
                    .orElse(previousState.lastProcessTxnSeq());

            final String fleetId = previousState.fleetId();

            final List<TransactionMarker> unprocessedMarkers = txnLogService.getUnprocessedMarkers(fleetId, instanceUid, lastProcessedTxnSeq);
            final CoalescedActions coalesced = txnLogService.coalesce(unprocessedMarkers);
            LOG.debug("[{}/{}] {} unprocessed markers for this collector (last processed tnx id {}) coalesced to {}",
                    instanceUid, sequenceNum, unprocessedMarkers.size(), lastProcessedTxnSeq, coalesced);

            final OtlpExporterConfig exporterConfig = getExporterConfig(configSupplier);

            if (coalesced.recomputeIngestConfig()) {
                // The connection settings should only be sent when they change. Not having a config is a change, too.
                if (agentCapabilities.contains(Opamp.AgentCapabilities.AgentCapabilities_ReportsOwnLogs)) {
                    // The "own_logs" are always transmitted via HTTP according to OpAMP.
                    buildConnectionSettings(responseBuilder, exporterConfig);
                }
            }

            if (coalesced.recomputeConfig() || coalesced.recomputeIngestConfig()) {
                final var effectiveFleetId = (coalesced.newFleetId() == null) ? fleetId : coalesced.newFleetId();
                LOG.debug("[{}/{}] Computing new collector config for fleet id {}", instanceUid, sequenceNum, effectiveFleetId);

                final List<SourceDTO> sources;
                try (final var sourcesStream = sourceService.streamAllByFleet(effectiveFleetId)) {
                    sources = sourcesStream.toList();
                }

                final var collectorConfig = buildCollectorConfig(effectiveFleetId, sources, osType, exporterConfig);

                try {
                    final String configYaml = yamlObjectMapper.writeValueAsString(collectorConfig);

                    responseBuilder.setRemoteConfig(Opamp.AgentRemoteConfig.newBuilder()
                            .setConfig(Opamp.AgentConfigMap.newBuilder()
                                    .putConfigMap(REMOTE_CONFIG_KEY, Opamp.AgentConfigFile.newBuilder()
                                            .setContentType("application/yaml")
                                            .setBody(ByteString.copyFromUtf8(configYaml))
                                            .build())
                                    .build())
                            .setConfigHash(ByteString.copyFromUtf8(String.valueOf(coalesced.maxSeq()))).build() // TODO consistent hashing
                    );
                    if (coalesced.newFleetId() != null) {
                        // once everything has worked and if we have reassigned the collector to a new fleet, update the instance document
                        collectorInstanceService.updateCurrentFleet(instanceUid, coalesced.newFleetId());
                        LOG.debug("[{}/{}] Updated current fleet ID to {}", instanceUid, sequenceNum, coalesced.newFleetId());
                    }
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

        // Collectors send new CSRs when they need to renew their certificate.
        getCsr(message).ifPresent(csr -> handleRenewal(responseBuilder, instanceUid, csr, configSupplier));

        return responseBuilder.build();
    }

    /**
     * Handles a certificate renewal request sent by an already-authenticated collector inside the
     * Identified channel.
     * <p>
     * Unlike enrollment, renewal arrives over a mutually-authenticated JWT session: the collector
     * has already proven it holds its current active key. The new CSR is parsed and signed; the
     * new cert is stored as the collector's {@code next_certificate_*} alongside the existing
     * active cert. The collector activates it by authenticating with the new cert on a subsequent
     * connection.
     * <p>
     * Failures are logged but do not tear down the OpAMP session — the collector's existing cert
     * remains valid and it may retry renewal later.
     *
     * @param responseBuilder the outgoing ServerToAgent builder to add the new cert to on success
     * @param instanceUid     the collector's OpAMP instance UID (for logging / inserting the cert)
     * @param csrPem          the PEM-encoded renewal CSR
     * @param configSupplier  memoized access to the collectors config (cert lifetime, heartbeat)
     */
    private void handleRenewal(ServerToAgent.Builder responseBuilder,
                               String instanceUid,
                               String csrPem,
                               Supplier<CollectorsConfig> configSupplier) {
        LOG.info("Received CSR for certificate renewal from instance: {}", instanceUid);
        try {
            final var config = configSupplier.get();
            final var issuer = collectorCaService.getSigningCert();
            final var cert = certificateService.builder().signCsr(PemUtils.parseCsr(csrPem), issuer, instanceUid, config.collectorCertLifetime());

            final var fingerprint = PemUtils.computeFingerprint(cert);
            final var certPem = PemUtils.toPem(cert);
            final var expiresAt = cert.getNotAfter().toInstant();

            if (!collectorInstanceService.insertNextCertificate(instanceUid, fingerprint, certPem, expiresAt)) {
                LOG.warn("Couldn't insert next certificate for instanceUid {}", instanceUid);
                return;
            }

            LOG.info("Sending new certificate for instance: {} (fingerprint={} expires={})", instanceUid, fingerprint, expiresAt);
            final var connectionSettingsBuilder = ConnectionSettingsOffers.newBuilder();
            setOpampConnectionSettings(connectionSettingsBuilder, certPem, config.collectorHeartbeatInterval());

            responseBuilder.setConnectionSettings(connectionSettingsBuilder.build());
        } catch (Exception e) {
            LOG.error("Failed to send renewal request to agent {}: {}", instanceUid, e.getMessage());
        }
    }

    @WithSpan
    private OptionalLong handleRemoteConfig(String instanceUid,
                                            long sequenceNum,
                                            AgentToServer message,
                                            CollectorInstanceReport.Builder updateBuilder) {
        if (!message.hasRemoteConfigStatus()) {
            return OptionalLong.empty();
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
                            return OptionalLong.of(txnSeq);
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

        return OptionalLong.empty();
    }

    private OtlpExporterConfig getExporterConfig(Supplier<CollectorsConfig> configSupplier) {
        final CollectorsConfig collectorsConfig = configSupplier.get();
        final var httpEndpoint = collectorsConfig.http();

        // We use the long-lived CA cert so intermediate cert rotation is not an issue for Collector mTLS connections.
        final var caCert = collectorCaService.getCaCert().certificate();
        final var tlsSettings = TLSConfigurationSettings.withCACert(clusterIdService.getString(), caCert);

        return OtlpHttpExporterConfig.builder()
                .endpoint(f("https://%s:%s", httpEndpoint.hostname(), httpEndpoint.port()))
                .tls(tlsSettings)
                .build();
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
