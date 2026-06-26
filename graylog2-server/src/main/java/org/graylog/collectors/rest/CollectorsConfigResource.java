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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.collectors.CollectorIngestInputService;
import org.graylog.collectors.CollectorLogsDestinationService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.CollectorsInitializer;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.IngestEndpointConfig;
import org.graylog.collectors.db.MarkerType;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.rest.HideOnCloud;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Manages the collector ingest configuration. Bound in both on-prem and Cloud nodes; the only behavioral difference is
 * that in Cloud the ingest endpoint is server-provisioned (the client-supplied http ingest config is ignored and derived
 * from the node's external URI) and there is no persisted ingest input — so the input-management endpoints are
 * {@link HideOnCloud hidden in Cloud} and {@code PUT} never creates an input. The in-memory ingest input is launched in
 * Cloud by {@code CloudCollectorIngestService}, activated by the config save below.
 */
@Tag(name = "Collectors/Config", description = "Managed collector configuration")
@Path("/collectors/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
@PublicCloudAPI
public class CollectorsConfigResource extends RestResource {
    private final CollectorsConfigService collectorsConfigService;
    private final CollectorIngestInputService collectorIngestInputService;
    private final CollectorLogsDestinationService logsDestinationService;
    private final URI httpExternalUri;
    private final FleetService fleetService;
    private final FleetTransactionLogService fleetTransactionLogService;
    private final CollectorsInitializer collectorsInitializer;
    private final boolean isCloud;

    @Inject
    public CollectorsConfigResource(CollectorsConfigService collectorsConfigService,
                                    CollectorIngestInputService collectorIngestInputService,
                                    CollectorLogsDestinationService logsDestinationService,
                                    HttpConfiguration httpConfiguration,
                                    FleetService fleetService,
                                    FleetTransactionLogService fleetTransactionLogService,
                                    CollectorsInitializer collectorsInitializer,
                                    Configuration configuration) {
        this.collectorsConfigService = collectorsConfigService;
        this.collectorIngestInputService = collectorIngestInputService;
        this.logsDestinationService = logsDestinationService;
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
        this.fleetService = fleetService;
        this.fleetTransactionLogService = fleetTransactionLogService;
        this.collectorsInitializer = collectorsInitializer;
        this.isCloud = configuration.isCloud();
    }

    @GET
    @Operation(summary = "Get collectors configuration")
    @RequiresPermissions(CollectorsPermissions.CONFIGURATION_READ)
    public CollectorsConfig get(@Context ContainerRequestContext requestContext) {
        return collectorsConfigService.get()
                .orElseGet(() -> CollectorsConfig.createDefault(derivedHostname(requestContext)));
    }

    // Separate endpoint so the UI can check input existence without needing read permission on each input.
    // Currently, all users with Reader role have wildcard inputs:read, so per-input filtering is not needed in
    // practice. If more fine-grained read permissions become common, the UI can use these IDs to determine the
    // presence of collector inputs, regardless of the user's read permissions.
    @GET
    @Path("/inputs")
    @Operation(summary = "Get collector ingest input IDs")
    @RequiresPermissions(CollectorsPermissions.CONFIGURATION_READ)
    @HideOnCloud
    public CollectorInputIdsResponse getInputIds() {
        return new CollectorInputIdsResponse(collectorIngestInputService.getInputIds());
    }

    @POST
    @Path("/inputs")
    @Operation(summary = "Create the default collector ingest input")
    @NoAuditEvent("Audit event is emitted by CollectorIngestInputService")
    // Input permissions are checked inline in CollectorIngestInputService#createInput. Collectors config is not
    // altered, only read in this process.
    @RequiresPermissions(CollectorsPermissions.CONFIGURATION_READ)
    @HideOnCloud
    public CollectorInputIdsResponse createInput() throws ValidationException {
        final var config = collectorsConfigService.get()
                .orElseThrow(() -> new BadRequestException("Collectors config has not been initialized yet"));
        collectorIngestInputService.createInput(getSubject(), getCurrentUser().getName(), config.http().port());
        return new CollectorInputIdsResponse(collectorIngestInputService.getInputIds());
    }

    @AuditEvent(type = AuditEventTypes.COLLECTORS_CONFIG_UPDATE)
    @PUT
    @Operation(summary = "Update collectors configuration")
    @RequiresPermissions(CollectorsPermissions.CONFIGURATION_EDIT)
    public CollectorsConfig put(@Context ContainerRequestContext requestContext,
                                @Valid @NotNull @RequestBody(required = true, useParameterTypeSchema = true) CollectorsConfigRequest request) throws ValidationException {

        final var existing = collectorsConfigService.get();

        final CollectorsConfig.Builder configBuilder;
        if (existing.isEmpty()) {
            configBuilder = request.applyTo(CollectorsConfig.builder());
            if (isCloud) {
                // The ingest endpoint is server-provisioned in Cloud; ignore any client-supplied hostname/port.
                configBuilder.http(new IngestEndpointConfig(derivedHostname(requestContext), CollectorsConfig.DEFAULT_HTTP_PORT));
            }
        } else {
            configBuilder = request.applyTo(existing.get().toBuilder());
            if (isCloud) {
                // Keep the server-provisioned endpoint; thresholds are the only thing a Cloud tenant may change.
                configBuilder.http(existing.get().http());
            }
        }

        final var validatedConfig = validateThresholds(configBuilder.build());

        // Ensure the collector-logs destination (index set + stream + stream rule) is set up correctly
        logsDestinationService.ensureExists();

        // Ensure one-time bootstrapping of certs etc. so that references in the config are set
        final var config = existing.isPresent() ? validatedConfig : collectorsInitializer.initialize(validatedConfig);

        collectorsConfigService.save(config);

        // No persisted ingest input exists in Cloud — the input is launched in-memory by CloudCollectorIngestService.
        if (!isCloud && request.createInput()) {
            collectorIngestInputService.createInput(getSubject(), getCurrentUser().getName(), request.http().port());
        }

        // TODO: We should probably compare the existing and new config to avoid the marker for unrelated changes.
        final var fleetIds = fleetService.getAllFleetIds();
        if (!fleetIds.isEmpty()) {
            fleetTransactionLogService.appendFleetMarker(fleetIds, MarkerType.INGEST_CONFIG_CHANGED);
        }

        return config;
    }

    private String derivedHostname(ContainerRequestContext requestContext) {
        final var host = RestTools.buildExternalUri(requestContext.getHeaders(), httpExternalUri).getHost();
        // In Cloud the ingest endpoint is exposed under an "ingest-"-prefixed hostname (same scheme the forwarder
        // ingest endpoint uses).
        return isCloud ? "ingest-" + host : host;
    }

    private CollectorsConfig validateThresholds(CollectorsConfig config) throws ValidationException {
        final Duration offlineThreshold = config.collectorOfflineThreshold();
        final Duration visibilityThreshold = config.collectorDefaultVisibilityThreshold();
        final Duration expirationThreshold = config.collectorExpirationThreshold();
        final Map<String, List<ValidationResult>> errors = new HashMap<>();

        if (offlineThreshold != null && (offlineThreshold.isZero() || offlineThreshold.isNegative())) {
            errors.computeIfAbsent("collector_offline_threshold", k -> new ArrayList<>())
                    .add(new ValidationResult.ValidationFailed("Must be a positive duration"));
        }
        if (visibilityThreshold != null && (visibilityThreshold.isZero() || visibilityThreshold.isNegative())) {
            errors.computeIfAbsent("collector_default_visibility_threshold", k -> new ArrayList<>())
                    .add(new ValidationResult.ValidationFailed("Must be a positive duration"));
        }
        if (expirationThreshold != null && (expirationThreshold.isZero() || expirationThreshold.isNegative())) {
            errors.computeIfAbsent("collector_expiration_threshold", k -> new ArrayList<>())
                    .add(new ValidationResult.ValidationFailed("Must be a positive duration"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        final Duration effectiveOffline = offlineThreshold != null
                ? offlineThreshold : CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD;
        if (effectiveOffline.toMinutes() < 1) {
            errors.computeIfAbsent("collector_offline_threshold", k -> new ArrayList<>())
                    .add(new ValidationResult.ValidationFailed("Must be at least 1 minute"));
        }

        final Duration effectiveVisibility = visibilityThreshold != null
                ? visibilityThreshold : CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD;
        if (!effectiveVisibility.minus(effectiveOffline).isPositive()) {
            errors.computeIfAbsent("collector_default_visibility_threshold", k -> new ArrayList<>())
                    .add(new ValidationResult.ValidationFailed(f("Must be greater than the offline threshold (%s)",
                            formatDurationWords(effectiveOffline.toMillis(), true, true))));
        }

        final Duration effectiveExpiration = expirationThreshold != null
                ? expirationThreshold : CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD;
        if (!effectiveExpiration.minus(effectiveVisibility).isPositive()) {
            errors.computeIfAbsent("collector_expiration_threshold", k -> new ArrayList<>())
                    .add(new ValidationResult.ValidationFailed(
                            f("Must be greater than the visibility threshold (%s)",
                                    formatDurationWords(effectiveVisibility.toMillis(), true, true))));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        return config;
    }
}
