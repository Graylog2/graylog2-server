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
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.collectors.CollectorCaService;
import org.graylog.collectors.CollectorInputService;
import org.graylog.collectors.CollectorLogsDestinationService;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.CollectorsPermissions;
import org.graylog.collectors.FleetService;
import org.graylog.collectors.FleetTransactionLogService;
import org.graylog.collectors.TokenSigningKey;
import org.graylog.collectors.db.MarkerType;
import org.graylog.collectors.input.CollectorIngestHttpInput;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.rest.RestTools;
import org.graylog2.shared.rest.resources.RestResource;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;
import static org.graylog2.shared.utilities.StringUtils.f;

@Tag(name = "Collectors/Config", description = "Managed collector configuration")
@Path("/collectors/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class CollectorsConfigResource extends RestResource {
    private final CollectorsConfigService collectorsConfigService;
    private final CollectorInputService collectorInputService;
    private final CollectorLogsDestinationService collectorLogsDestinationService;
    private final URI httpExternalUri;
    private final FleetService fleetService;
    private final FleetTransactionLogService fleetTransactionLogService;
    private final EnrollmentTokenService enrollmentTokenService;
    private final CollectorCaService collectorCaService;

    @Inject
    public CollectorsConfigResource(CollectorsConfigService collectorsConfigService,
                                    CollectorInputService collectorInputService,
                                    CollectorLogsDestinationService collectorLogsDestinationService,
                                    HttpConfiguration httpConfiguration,
                                    FleetService fleetService,
                                    FleetTransactionLogService fleetTransactionLogService,
                                    EnrollmentTokenService enrollmentTokenService,
                                    CollectorCaService collectorCaService) {
        this.collectorsConfigService = collectorsConfigService;
        this.collectorInputService = collectorInputService;
        this.collectorLogsDestinationService = collectorLogsDestinationService;
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();
        this.fleetService = fleetService;
        this.fleetTransactionLogService = fleetTransactionLogService;
        this.enrollmentTokenService = enrollmentTokenService;
        this.collectorCaService = collectorCaService;
    }

    @GET
    @Operation(summary = "Get collectors configuration")
    @RequiresPermissions(CollectorsPermissions.CONFIGURATION_READ)
    public CollectorsConfig get(@Context ContainerRequestContext requestContext) {
        return collectorsConfigService.get().orElseGet(() -> {
            final var hostname = RestTools.buildExternalUri(requestContext.getHeaders(), httpExternalUri).getHost();
            return CollectorsConfig.createDefault(hostname);
        });
    }

    @AuditEvent(type = AuditEventTypes.COLLECTORS_CONFIG_UPDATE)
    @PUT
    @Operation(summary = "Update collectors configuration")
    @RequiresPermissions(CollectorsPermissions.CONFIGURATION_EDIT)
    public CollectorsConfig put(@Valid @NotNull @RequestBody(required = true, useParameterTypeSchema = true) CollectorsConfigRequest request) throws ValidationException {
        validateThresholds(request);
        collectorCaService.ensureInitialized();
        collectorLogsDestinationService.ensureExists();

        final var existing = collectorsConfigService.get();
        final String creatorUserId = SecurityUtils.getSubject().getPrincipal().toString();

        final String httpInputId = collectorInputService.reconcile(
                request.http(),
                existing.map(CollectorsConfig::http).orElse(null),
                CollectorIngestHttpInput.class.getCanonicalName(),
                CollectorIngestHttpInput.NAME,
                creatorUserId);
        final Duration effectiveOffline = request.collectorOfflineThreshold() != null
                ? request.collectorOfflineThreshold() : CollectorsConfig.DEFAULT_OFFLINE_THRESHOLD;
        final Duration effectiveVisibility = request.collectorDefaultVisibilityThreshold() != null
                ? request.collectorDefaultVisibilityThreshold() : CollectorsConfig.DEFAULT_VISIBILITY_THRESHOLD;
        final Duration effectiveExpiration = request.collectorExpirationThreshold() != null
                ? request.collectorExpirationThreshold() : CollectorsConfig.DEFAULT_EXPIRATION_THRESHOLD;

        final TokenSigningKey tokenSigningKey;
        if (existing.isPresent()) {
            tokenSigningKey = existing.get().tokenSigningKey();
        } else {
            try {
                tokenSigningKey = enrollmentTokenService.createTokenSigningKey();
            } catch (Exception e) {
                throw new InternalServerErrorException("Could not create token signing key", e);
            }
        }

        final var config = CollectorsConfig.builder()
                .caCertId(collectorCaService.getCaCertId())
                .signingCertId(collectorCaService.getSigningCertId())
                .tokenSigningKey(tokenSigningKey)
                .otlpServerCertId(collectorCaService.getOtlpServerCertId())
                .http(request.http().toConfig(httpInputId))
                .collectorOfflineThreshold(effectiveOffline)
                .collectorDefaultVisibilityThreshold(effectiveVisibility)
                .collectorExpirationThreshold(effectiveExpiration)
                .build();

        collectorsConfigService.save(config);

        // TODO: We should probably compare the existing and new config to avoid the marker for unrelated changes.
        final var fleetIds = fleetService.getAllFleetIds();
        if (!fleetIds.isEmpty()) {
            fleetTransactionLogService.appendFleetMarker(fleetIds, MarkerType.INGEST_CONFIG_CHANGED);
        }

        return config;
    }

    private void validateThresholds(CollectorsConfigRequest request) throws ValidationException {
        final Duration offlineThreshold = request.collectorOfflineThreshold();
        final Duration visibilityThreshold = request.collectorDefaultVisibilityThreshold();
        final Duration expirationThreshold = request.collectorExpirationThreshold();
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
    }
}
