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
package org.graylog.aws.config;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import jakarta.inject.Inject;

import jakarta.validation.Valid;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Api(value = "AWS/Config", description = "Manage AWS Config settings")
@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class AWSConfigurationResource extends RestResource implements PluginRestResource {
    private final ClusterConfigService clusterConfigService;
    private final Configuration systemConfiguration;

    @Inject
    public AWSConfigurationResource(ClusterConfigService clusterConfigService,
                                    Configuration systemConfiguration) {
        this.clusterConfigService = clusterConfigService;
        this.systemConfiguration = systemConfiguration;
    }

    @PUT
    @ApiOperation(value = "Updates the AWS default configuration.")
    @RequiresPermissions({RestPermissions.CLUSTER_CONFIG_ENTRY_CREATE, RestPermissions.CLUSTER_CONFIG_ENTRY_EDIT})
    @AuditEvent(type = AuditEventTypes.CLUSTER_CONFIGURATION_UPDATE)
    public Response updateConfig(@Valid AWSPluginConfigurationUpdate update) {
        final AWSPluginConfiguration existingConfiguration = clusterConfigService.getOrDefault(
                AWSPluginConfiguration.class,
                AWSPluginConfiguration.createDefault()
        );
        final AWSPluginConfiguration.Builder newConfigBuilder = existingConfiguration.toBuilder()
                .lookupsEnabled(update.lookupsEnabled())
                .lookupRegions(update.lookupRegions())
                .accessKey(update.accessKey())
                .proxyEnabled(update.proxyEnabled());

        final AWSPluginConfiguration newConfiguration = update.secretKey()
                .map(secretKey -> newConfigBuilder.secretKey(secretKey, systemConfiguration.getPasswordSecret()))
                .orElse(newConfigBuilder)
                .build();

        clusterConfigService.write(newConfiguration);
        return Response.accepted(newConfiguration).build();
    }
}
