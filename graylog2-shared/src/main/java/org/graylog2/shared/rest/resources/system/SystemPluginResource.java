/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.rest.resources.system;

import autovalue.shaded.com.google.common.common.collect.Sets;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.graylog2.rest.models.system.plugins.responses.PluginList;
import org.graylog2.rest.models.system.plugins.responses.PluginMetaDataValue;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Api(value = "System/Plugin", description = "Plugin information")
@Path("/system/plugins")
@Produces(MediaType.APPLICATION_JSON)
public class SystemPluginResource extends RestResource {
    private final Set<PluginMetaData> pluginMetaDataSet;

    @Inject
    public SystemPluginResource(Set<PluginMetaData> pluginMetaDataSet) {
        this.pluginMetaDataSet = pluginMetaDataSet;
    }

    @GET
    @Timed
    @ApiOperation(value = "List all installed plugins on this node.")
    public PluginList list() {
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
                    capabilityToStringSet(pluginMetaData.getRequiredCapabilities())
            ));
        }

        return PluginList.create(pluginMetaDataValues);
    }

    private Set<String> capabilityToStringSet(Set<ServerStatus.Capability> capabilities) {
        final Set<String> stringSet = Sets.newHashSetWithExpectedSize(capabilities.size());
        for (ServerStatus.Capability capability : capabilities) {
            stringSet.add(capability.toString());
        }

        return stringSet;
    }
}
