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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.configuration.IndexSetsDefaultConfiguration;
import org.graylog2.indexer.IndexSetValidator;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.Duration;

import java.io.IOException;

import static org.graylog2.shared.utilities.StringUtils.f;

@RequiresAuthentication
@Api(value = "System/IndexSetDefaults", description = "Index set defaults")
@Path("/system/indices/index_set_defaults")
@Produces(MediaType.APPLICATION_JSON)
public class IndexSetDefaultsResource extends RestResource {
    private final IndexSetValidator indexSetValidator;
    private final ClusterConfigService clusterConfigService;
    private final Validator validator;

    @Inject
    public IndexSetDefaultsResource(IndexSetValidator indexSetValidator, ClusterConfigService clusterConfigService, Validator validator) {
        this.indexSetValidator = indexSetValidator;
        this.clusterConfigService = clusterConfigService;
        this.validator = validator;
    }

    private static String buildFieldError(String field, String message) {
        return f("Invalid value for field [%s]: %s", field, message);
    }

    /**
     * Save new {@link IndexSetsDefaultConfiguration} cluster configuration object. This method exists to allow additional validation
     * before saving with the {@link ClusterConfigService}.
     */
    @PUT
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update index set defaults configuration")
    @RequiresPermissions({RestPermissions.CLUSTER_CONFIG_ENTRY_CREATE, RestPermissions.CLUSTER_CONFIG_ENTRY_EDIT})
    @AuditEvent(type = AuditEventTypes.CLUSTER_CONFIGURATION_UPDATE)
    public Response update(@ApiParam(name = "body", value = "The payload of the index set defaults configuration", required = true)
                           @NotNull IndexSetsDefaultConfiguration config) throws IOException {
        // Validate scalar fields.
        validator.validate(config).forEach(v -> {
            throw new BadRequestException(buildFieldError(v.getPropertyPath().toString(), v.getMessage()));
        });

        // Perform common refresh interval and retention period validations.
        IndexSetValidator.Violation violation =
                indexSetValidator.validateRefreshInterval(Duration.standardSeconds(
                        config.fieldTypeRefreshIntervalUnit().toSeconds(config.fieldTypeRefreshInterval())));
        if (violation != null) {
            throw new BadRequestException(buildFieldError(IndexSetsDefaultConfiguration.FIELD_TYPE_REFRESH_INTERVAL, violation.message()));
        }

        violation = indexSetValidator.validateRotation(config.rotationStrategyConfig());

        if (violation != null) {
            throw new BadRequestException(buildFieldError(IndexSetsDefaultConfiguration.ROTATION_STRATEGY_CONFIG, violation.message()));
        }

        violation = indexSetValidator.validateRetentionPeriod(config.rotationStrategyConfig(),
                config.retentionStrategyConfig());
        if (violation != null) {
            throw new BadRequestException(buildFieldError(IndexSetsDefaultConfiguration.RETENTION_STRATEGY_CONFIG, violation.message()));
        }

        violation = indexSetValidator.validateDataTieringConfig(config.dataTiering());
        if (violation != null) {
            throw new BadRequestException(buildFieldError(IndexSetConfig.FIELD_DATA_TIERING, violation.message()));
        }

        clusterConfigService.write(config);
        return Response.ok(config).build();
    }
}
