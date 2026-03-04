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
package org.graylog.collectors.rest;

import com.mongodb.client.model.Filters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.bson.types.ObjectId;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog.collectors.indexer.CollectorLogsIndexTemplateProvider;
import org.graylog.collectors.input.CollectorIngestCodec;
import org.graylog.collectors.input.CollectorIngestGrpcInput;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog.collectors.input.processor.CollectorLogRecordProcessor;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetConfigFactory;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.validation.IndexSetValidator;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedSizeOptimizingStrategyConfig;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog.collectors.opamp.OpAmpCaService;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.graylog2.indexer.indexset.fields.IndexPrefixField.FIELD_INDEX_PREFIX;
import static org.graylog2.indexer.indexset.fields.IndexTemplateTypeField.FIELD_INDEX_TEMPLATE_TYPE;
import static org.graylog2.shared.utilities.StringUtils.f;

@Tag(name = "Collectors/Config", description = "Managed collector configuration")
@Path("/collectors/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class CollectorsConfigResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorsConfigResource.class);

    static final int DEFAULT_HTTP_PORT = 14401;
    static final int DEFAULT_GRPC_PORT = 14402;
    static final String COLLECTOR_LOGS_INDEX_PREFIX = "gl-collector-logs";

    private final ClusterConfigService clusterConfigService;
    private final URI httpExternalUri;
    private final IndexSetService indexSetService;
    private final IndexSetConfigFactory indexSetConfigFactory;
    private final IndexSetValidator indexSetValidator;
    private final InputService inputService;
    private final OpAmpCaService opAmpCaService;
    private final StreamService streamService;
    private final StreamRuleService streamRuleService;

    @Inject
    public CollectorsConfigResource(ClusterConfigService clusterConfigService,
                                    HttpConfiguration httpConfiguration,
                                    IndexSetService indexSetService,
                                    IndexSetConfigFactory indexSetConfigFactory,
                                    IndexSetValidator indexSetValidator,
                                    InputService inputService,
                                    OpAmpCaService opAmpCaService,
                                    StreamService streamService,
                                    StreamRuleService streamRuleService) {
        this.clusterConfigService = clusterConfigService;
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
        this.indexSetService = indexSetService;
        this.indexSetConfigFactory = indexSetConfigFactory;
        this.indexSetValidator = indexSetValidator;
        this.inputService = inputService;
        this.opAmpCaService = opAmpCaService;
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
    }

    @GET
    @Operation(summary = "Get collectors configuration")
    public CollectorsConfig get(@Context ContainerRequestContext requestContext) {
        final var existing = clusterConfigService.get(CollectorsConfig.class);
        if (existing != null) {
            return existing;
        }

        final var hostname = RestTools.buildExternalUri(requestContext.getHeaders(), httpExternalUri).getHost();
        return new CollectorsConfig(
                null, null, null,
                new IngestEndpointConfig(true, hostname, DEFAULT_HTTP_PORT, null),
                new IngestEndpointConfig(false, hostname, DEFAULT_GRPC_PORT, null)
        );
    }

    @NoAuditEvent("TODO")
    @PUT
    @Operation(summary = "Update collectors configuration")
    public CollectorsConfig put(@Valid @NotNull @RequestBody(required = true, useParameterTypeSchema = true) CollectorsConfigRequest request) {
        // 1. Initialize CA hierarchy (creates certs if needed, caches in memory)
        opAmpCaService.ensureInitialized();

        // 2. Ensure collector logs stream and index set exist
        if (request.http().enabled() || request.grpc().enabled()) {
            ensureCollectorLogsStreamAndIndexSet();
        }

        final var existing = clusterConfigService.get(CollectorsConfig.class);

        // 3. Reconcile HTTP input
        final String httpInputId = reconcileInput(
                request.http(),
                existing != null ? existing.http() : null,
                CollectorIngestHttpInput.class.getCanonicalName(),
                CollectorIngestHttpInput.NAME);

        // 4. Reconcile gRPC input
        final String grpcInputId = reconcileInput(
                request.grpc(),
                existing != null ? existing.grpc() : null,
                CollectorIngestGrpcInput.class.getCanonicalName(),
                CollectorIngestGrpcInput.NAME);

        // 5. Build and persist full config
        final var config = new CollectorsConfig(
                opAmpCaService.getOpAmpCaId(),
                opAmpCaService.getTokenSigningCertId(),
                opAmpCaService.getOtlpServerCertId(),
                new IngestEndpointConfig(request.http().enabled(), request.http().hostname(),
                        request.http().port(), httpInputId),
                new IngestEndpointConfig(request.grpc().enabled(), request.grpc().hostname(),
                        request.grpc().port(), grpcInputId)
        );

        clusterConfigService.write(config);
        return config;
    }

    private void ensureCollectorLogsStreamAndIndexSet() {
        // 1. Ensure index set exists
        final String indexSetId = ensureCollectorLogsIndexSet();

        // 2. Ensure stream exists
        ensureCollectorLogsStream(indexSetId);

        // 3. Ensure stream rule exists
        ensureCollectorLogsStreamRule();
    }

    private String ensureCollectorLogsIndexSet() {
        final var query = Filters.and(
                Filters.eq(FIELD_INDEX_TEMPLATE_TYPE,
                        Optional.of(CollectorLogsIndexTemplateProvider.COLLECTOR_LOGS_TEMPLATE_TYPE)),
                Filters.eq(FIELD_INDEX_PREFIX, COLLECTOR_LOGS_INDEX_PREFIX)
        );
        final Optional<IndexSetConfig> existing = indexSetService.findOne(query);
        if (existing.isPresent()) {
            return requireNonNull(existing.get().id(), "index set ID cannot be null");
        }

        final IndexSetConfig indexSetConfig = indexSetConfigFactory.createDefault()
                .title("Collector Logs")
                .description("Index set for collector self-log messages")
                .indexTemplateType(CollectorLogsIndexTemplateProvider.COLLECTOR_LOGS_TEMPLATE_TYPE)
                .isWritable(true)
                .isRegular(false)
                .indexPrefix(COLLECTOR_LOGS_INDEX_PREFIX)
                .indexTemplateName(COLLECTOR_LOGS_INDEX_PREFIX + "-template")
                .rotationStrategyClass(TimeBasedSizeOptimizingStrategy.class.getCanonicalName())
                .rotationStrategyConfig(TimeBasedSizeOptimizingStrategyConfig.builder()
                        .indexLifetimeMin(Period.days(7))
                        .indexLifetimeMax(Period.days(7))
                        .build())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategyConfig(DeletionRetentionStrategyConfig.createDefault())
                .dataTieringConfig(null)
                .build();

        final Optional<IndexSetValidator.Violation> violation = indexSetValidator.validate(indexSetConfig);
        if (violation.isPresent()) {
            throw new InternalServerErrorException(
                    f("Collector logs index set validation failed: %s", violation.get().message()));
        }

        final IndexSetConfig saved = indexSetService.save(indexSetConfig);
        LOG.info("Created collector logs index set <{}/{}>", saved.id(), saved.title());
        return requireNonNull(saved.id(), "index set ID cannot be null");
    }

    private void ensureCollectorLogsStream(String indexSetId) {
        try {
            streamService.load(Stream.COLLECTOR_LOGS_STREAM_ID);
            return; // Stream already exists
        } catch (NotFoundException ignored) {
            // Stream does not exist, create it
        }

        final var stream = StreamImpl.builder()
                .id(Stream.COLLECTOR_LOGS_STREAM_ID)
                .title("Collector Logs")
                .description("Stream containing collector self-logs from managed collectors")
                .disabled(false)
                .createdAt(DateTime.now(DateTimeZone.UTC))
                .creatorUserId("admin")
                .matchingType(StreamImpl.MatchingType.DEFAULT)
                .removeMatchesFromDefaultStream(true)
                .indexSetId(indexSetId)
                .isDefault(false)
                .scope(ImmutableSystemScope.NAME)
                .build();

        try {
            streamService.save(stream);
            LOG.info("Created collector logs stream <{}/{}>", stream.getId(), stream.getTitle());
        } catch (ValidationException e) {
            throw new InternalServerErrorException("Failed to create collector logs stream", e);
        }
    }

    private void ensureCollectorLogsStreamRule() {
        final var existingRules = streamRuleService.loadForStreamId(Stream.COLLECTOR_LOGS_STREAM_ID);
        if (!existingRules.isEmpty()) {
            return; // Rules already exist
        }

        final var rule = streamRuleService.create(Map.of(
                StreamRuleImpl.FIELD_STREAM_ID, new ObjectId(Stream.COLLECTOR_LOGS_STREAM_ID),
                StreamRuleImpl.FIELD_FIELD, CollectorIngestCodec.FIELD_COLLECTOR_RECEIVER_TYPE,
                StreamRuleImpl.FIELD_TYPE, StreamRuleType.EXACT.toInteger(),
                StreamRuleImpl.FIELD_VALUE, CollectorLogRecordProcessor.RECEIVER_TYPE,
                StreamRuleImpl.FIELD_INVERTED, false,
                StreamRuleImpl.FIELD_DESCRIPTION, "Route collector self-logs to dedicated stream"
        ));

        try {
            streamRuleService.save(rule);
            LOG.info("Created collector logs stream rule for stream <{}>", Stream.COLLECTOR_LOGS_STREAM_ID);
        } catch (ValidationException e) {
            throw new InternalServerErrorException("Failed to create collector logs stream rule", e);
        }
    }

    @Nullable
    private String reconcileInput(CollectorsConfigRequest.IngestEndpointRequest requested,
                                  @Nullable IngestEndpointConfig existing,
                                  String inputType,
                                  String title) {
        final String existingInputId = existing != null ? existing.inputId() : null;
        final boolean inputExists = existingInputId != null && inputExistsInDb(existingInputId);

        if (requested.enabled()) {
            if (!inputExists) {
                return createManagedInput(inputType, title);
            }
            // Trigger restart if port changed so transport picks up new config
            if (existing.port() != requested.port()) {
                restartInput(existingInputId);
            }
            return existingInputId;
        } else {
            if (inputExists) {
                deleteManagedInput(existingInputId);
            }
            return null;
        }
    }

    private boolean inputExistsInDb(String inputId) {
        try {
            inputService.find(inputId);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    private String createManagedInput(String inputType, String title) {
        final String creatorUserId = SecurityUtils.getSubject().getPrincipal().toString();
        final var input = inputService.create(Map.of(
                MessageInput.FIELD_TYPE, inputType,
                MessageInput.FIELD_TITLE, title,
                MessageInput.FIELD_CREATOR_USER_ID, creatorUserId,
                MessageInput.FIELD_GLOBAL, true,
                MessageInput.FIELD_CONFIGURATION, Map.of(),
                MessageInput.FIELD_DESIRED_STATE, IOState.Type.RUNNING.name()
        ));
        try {
            return inputService.save(input);
        } catch (ValidationException e) {
            throw new InternalServerErrorException("Failed to create managed input: " + title, e);
        }
    }

    private void restartInput(String inputId) {
        try {
            final Input input = inputService.find(inputId);
            inputService.update(input);
        } catch (NotFoundException e) {
            LOG.warn("Input {} not found during restart attempt", inputId);
        } catch (ValidationException e) {
            throw new InternalServerErrorException("Failed to restart input " + inputId, e);
        }
    }

    private void deleteManagedInput(String inputId) {
        try {
            final Input input = inputService.find(inputId);
            inputService.destroy(input);
        } catch (NotFoundException e) {
            LOG.warn("Input {} not found during delete attempt", inputId);
        }
    }
}
