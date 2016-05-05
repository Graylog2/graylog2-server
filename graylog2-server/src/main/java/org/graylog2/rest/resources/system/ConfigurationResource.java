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

package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.Configuration;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.rest.models.system.configuration.ConfigurationList;
import org.graylog2.rest.models.system.configuration.ConfigurationVariable;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;

@RequiresAuthentication
@Api(value = "System/Configuration", description = "Read-only access to configuration variables")
@Path("/system/configuration")
public class ConfigurationResource extends RestResource  {

    private final Configuration configuration;
    private final ElasticsearchConfiguration esConfiguration;

    @Inject
    public ConfigurationResource(Configuration configuration, ElasticsearchConfiguration esConfiguration) {
        this.configuration = configuration;
        this.esConfiguration = esConfiguration;
    }

    /*
     * This call is manually building a map of variables to return because we need to guarantee never to return any
     * sensitive variables like passwords etc. - See this as a whitelist approach.
     */
    @GET
    @Timed
    @ApiOperation(value = "Get all reported configuration variables and their values")
    public ConfigurationList getAll() {
        List<ConfigurationVariable> config = new ArrayList<>();

        config.add(ConfigurationVariable.create("inputbuffer_processors", configuration.getInputbufferProcessors()));
        config.add(ConfigurationVariable.create("processbuffer_processors", configuration.getProcessBufferProcessors()));
        config.add(ConfigurationVariable.create("outputbuffer_processors", configuration.getOutputBufferProcessors()));
        config.add(ConfigurationVariable.create("processor_wait_strategy", configuration.getProcessorWaitStrategy().getClass().getName()));
        config.add(ConfigurationVariable.create("inputbuffer_wait_strategy", configuration.getInputBufferWaitStrategy().getClass().getName()));
        config.add(ConfigurationVariable.create("inputbuffer_ring_size", configuration.getInputBufferRingSize()));
        config.add(ConfigurationVariable.create("ring_size", configuration.getRingSize()));

        config.add(ConfigurationVariable.create("plugin_dir", configuration.getPluginDir()));
        config.add(ConfigurationVariable.create("node_id_file", configuration.getNodeIdFile()));

        config.add(ConfigurationVariable.create("allow_highlighting", configuration.isAllowHighlighting()));
        config.add(ConfigurationVariable.create("allow_leading_wildcard_searches", configuration.isAllowLeadingWildcardSearches()));

        config.add(ConfigurationVariable.create("rotation_strategy", esConfiguration.getRotationStrategy()));
        config.add(ConfigurationVariable.create("retention_strategy", esConfiguration.getRetentionStrategy()));
        config.add(ConfigurationVariable.create("elasticsearch_max_docs_per_index", esConfiguration.getMaxDocsPerIndex()));
        config.add(ConfigurationVariable.create("elasticsearch_max_size_per_index", esConfiguration.getMaxSizePerIndex()));
        config.add(ConfigurationVariable.create("elasticsearch_max_time_per_index", esConfiguration.getMaxTimePerIndex().toString()));
        config.add(ConfigurationVariable.create("elasticsearch_max_number_of_indices", esConfiguration.getMaxNumberOfIndices()));
        config.add(ConfigurationVariable.create("elasticsearch_shards", esConfiguration.getShards()));
        config.add(ConfigurationVariable.create("elasticsearch_replicas", esConfiguration.getReplicas()));

        config.add(ConfigurationVariable.create("stream_processing_timeout", configuration.getStreamProcessingTimeout()));
        config.add(ConfigurationVariable.create("stream_processing_max_faults", configuration.getStreamProcessingMaxFaults()));

        config.add(ConfigurationVariable.create("output_module_timeout", configuration.getOutputModuleTimeout()));
        config.add(ConfigurationVariable.create("stale_master_timeout", configuration.getStaleMasterTimeout()));

        config.add(ConfigurationVariable.create("disable_index_optimization", esConfiguration.isDisableIndexOptimization()));
        config.add(ConfigurationVariable.create("index_optimization_max_num_segments", esConfiguration.getIndexOptimizationMaxNumSegments()));

        config.add(ConfigurationVariable.create("gc_warning_threshold", configuration.getGcWarningThreshold().toString()));

        return ConfigurationList.create(config);
    }

}
