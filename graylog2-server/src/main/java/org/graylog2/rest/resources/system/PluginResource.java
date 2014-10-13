/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.Version;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.graylog2.rest.resources.RestResource;

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
public class PluginResource extends RestResource {
    private final Set<PluginMetaData> pluginMetaDataSet;

    static class PluginMetaDataValue {
        public final String unique_id;
        public final String name;
        public final String author;
        public final URI url;
        public final Version version;
        public final String description;
        public final Version required_version;

        PluginMetaDataValue(PluginMetaData pluginMetaData) {
            this.unique_id = pluginMetaData.getUniqueId();
            this.name = pluginMetaData.getName();
            this.author = pluginMetaData.getAuthor();
            this.url = pluginMetaData.getURL();
            this.version = pluginMetaData.getVersion();
            this.description = pluginMetaData.getDescription();
            this.required_version = pluginMetaData.getRequiredVersion();
        }
    }

    @Inject
    public PluginResource(Set<PluginMetaData> pluginMetaDataSet) {
        this.pluginMetaDataSet = pluginMetaDataSet;
    }

    @GET
    @Timed
    @ApiOperation(value = "List all installed plugins on this node.")
    public String list() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("total", pluginMetaDataSet.size());

        List<PluginMetaDataValue> pluginMetaDataValues = Lists.newArrayList();

        for (PluginMetaData pluginMetaData : pluginMetaDataSet) {
            pluginMetaDataValues.add(new PluginMetaDataValue(pluginMetaData));
        }

        result.put("plugins", pluginMetaDataValues);

        return json(result);
    }
}
