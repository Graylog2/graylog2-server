package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.Version;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
import org.graylog2.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Api(value = "System/Plugin", description = "Plugin information")
@Path("/system/plugins")
@Produces(MediaType.APPLICATION_JSON)
public class PluginResource extends RestResource {
    private final Set<PluginMetaData> pluginMetaDataSet;

    class PluginMetaDataValue {
        public final String unique_id;
        public final String name;
        public final String author;
        public final URL url;
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
