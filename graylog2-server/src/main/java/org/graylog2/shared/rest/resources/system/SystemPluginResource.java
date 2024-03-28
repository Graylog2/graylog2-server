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
package org.graylog2.shared.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.Capabilities;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.plugins.responses.PluginList;
import org.graylog2.rest.models.system.plugins.responses.PluginMetaDataValue;
import org.graylog2.shared.rest.InlinePermissionCheck;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;
import java.util.Set;

@Api(value = "System/Plugins", description = "Plugin information")
@Path("/system/plugins")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SystemPluginResource extends RestResource {
    private final Set<PluginMetaData> pluginMetaDataSet;
    private final NodeId nodeId;

    @Inject
    public SystemPluginResource(Set<PluginMetaData> pluginMetaDataSet, NodeId nodeId) {
        this.pluginMetaDataSet = pluginMetaDataSet;
        this.nodeId = nodeId;
    }

    @GET
    @Timed
    @ApiOperation(value = "List all installed plugins on this node.")
    @InlinePermissionCheck
    public PluginList list() {
        checkPermission(RestPermissions.SYSTEM_READ, nodeId.getNodeId());
        final List<PluginMetaDataValue> pluginMetaDataValues = Lists.newArrayList();

        for (PluginMetaData pluginMetaData : pluginMetaDataSet) {
            pluginMetaDataValues.add(PluginMetaDataValue.create(
                    pluginMetaData.getUniqueId(),
                    pluginMetaData.getName(),
                    pluginMetaData.getAuthor(),
                    pluginMetaData.getURL(),
                    pluginMetaData.getVersion().toString(),
                    pluginMetaData.getDescription(),
                    pluginMetaData.getRequiredVersion().toString(),
                    Capabilities.toStringSet(pluginMetaData.getRequiredCapabilities())
            ));
        }

        return PluginList.create(pluginMetaDataValues);
    }
}
