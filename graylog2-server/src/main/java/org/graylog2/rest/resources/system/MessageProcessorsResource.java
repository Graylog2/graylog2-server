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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.messageprocessors.MessageProcessorsConfig;
import org.graylog2.messageprocessors.MessageProcessorsConfigWithDescriptors;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.messageprocessors.MessageProcessor;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import jakarta.inject.Inject;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "System/MessageProcessors", description = "Manage message processors", tags = {CLOUD_VISIBLE})
@Path("/system/messageprocessors")
@Produces(MediaType.APPLICATION_JSON)
public class MessageProcessorsResource extends RestResource {
    private final Set<String> processorClassNames;
    private final ClusterConfigService clusterConfigService;
    private final Set<MessageProcessor.Descriptor> processorDescriptors;

    @Inject
    public MessageProcessorsResource(final Set<MessageProcessor.Descriptor> processorDescriptors,
                                     final ClusterConfigService clusterConfigService) {
        this.processorDescriptors = processorDescriptors;
        this.processorClassNames = processorDescriptors.stream()
                .map(MessageProcessor.Descriptor::className)
                .collect(Collectors.toSet());
        this.clusterConfigService = clusterConfigService;
    }


    @GET
    @Timed
    @ApiOperation(value = "Get message processor configuration")
    @Path("config")
    public MessageProcessorsConfigWithDescriptors config() {
        checkPermission(RestPermissions.CLUSTER_CONFIG_ENTRY_READ);
        final MessageProcessorsConfig config = clusterConfigService.getOrDefault(MessageProcessorsConfig.class,
                MessageProcessorsConfig.defaultConfig());

        return MessageProcessorsConfigWithDescriptors.fromConfig(config.withProcessors(processorClassNames), processorDescriptors);
    }

    @PUT
    @Timed
    @ApiOperation(value = "Update message processor configuration")
    @Path("config")
    @AuditEvent(type = AuditEventTypes.MESSAGE_PROCESSOR_CONFIGURATION_UPDATE)
    public MessageProcessorsConfigWithDescriptors updateConfig(@ApiParam(name = "config", required = true) final MessageProcessorsConfigWithDescriptors configWithDescriptors) {
        checkPermission(RestPermissions.CLUSTER_CONFIG_ENTRY_EDIT);
        final MessageProcessorsConfig config = configWithDescriptors.toConfig();

        clusterConfigService.write(config.withProcessors(processorClassNames));

        return configWithDescriptors;
    }
}
