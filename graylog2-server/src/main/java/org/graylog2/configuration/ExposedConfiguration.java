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
package org.graylog2.configuration;

import com.google.common.collect.ImmutableList;
import org.graylog2.Configuration;
import org.graylog2.rest.models.system.configuration.ConfigurationVariable;

/**
 * List of configuration values that are safe to return, i.e. do not include any sensitive
 * information. Building a list manually because we need to guarantee never to return any
 * sensitive variables like passwords etc. - See this as a whitelist approach.
 */
public class ExposedConfiguration {

    private final int inputBufferProcessors;
    private final int processBufferProcessors;
    private final int outputBufferProcessors;

    private final String processorWaitStrategy;
    private final String inputBufferWaitStrategy;
    private final int inputBufferRingSize;
    private final int ringSize;
    private final String pluginDir;
    private final String nodeIdFile;

    private final boolean allowHighlighting;
    private final boolean allowLeadingWildcardSearches;

    private final String rotationStrategy;
    private final String retentionStrategy;

    private final int maxDocsPerIndex;
    private final long maxSizePerIndex;
    private final String maxTimePerIndex;
    private final int maxNumberOfIndices;
    private final int shards;
    private final int replicas;

    private final long streamProcessingTimeout;
    private final int streamProcessingMaxFaults;
    private final long outputModuleTimeout;
    private final int staleMasterTimeout;

    private final boolean disableIndexOptimization;
    private final int indexOptimizationMaxSegments;

    private final String gcWarningThreshold;

    public ExposedConfiguration(Configuration configuration, ElasticsearchConfiguration esConfiguration) {
        this.inputBufferProcessors = configuration.getInputbufferProcessors();
        this.processBufferProcessors = configuration.getProcessBufferProcessors();
        this.outputBufferProcessors = configuration.getOutputBufferProcessors();

        this.processorWaitStrategy = configuration.getProcessorWaitStrategy().getClass().getName();
        this.inputBufferWaitStrategy = configuration.getInputBufferWaitStrategy().getClass().getName();
        this.inputBufferRingSize = configuration.getInputBufferRingSize();

        this.ringSize = configuration.getRingSize();
        this.pluginDir = configuration.getPluginDir();
        this.nodeIdFile = configuration.getNodeIdFile();

        this.allowHighlighting = configuration.isAllowHighlighting();
        this.allowLeadingWildcardSearches = configuration.isAllowLeadingWildcardSearches();

        this.rotationStrategy = esConfiguration.getRotationStrategy();
        this.retentionStrategy = esConfiguration.getRetentionStrategy();

        this.maxDocsPerIndex = esConfiguration.getMaxDocsPerIndex();
        this.maxSizePerIndex = esConfiguration.getMaxSizePerIndex();
        this.maxTimePerIndex = esConfiguration.getMaxTimePerIndex().toString();

        this.maxNumberOfIndices = esConfiguration.getMaxNumberOfIndices();
        this.shards = esConfiguration.getShards();
        this.replicas = esConfiguration.getReplicas();

        this.streamProcessingTimeout = configuration.getStreamProcessingTimeout();
        this.streamProcessingMaxFaults = configuration.getStreamProcessingMaxFaults();
        this.outputModuleTimeout = configuration.getOutputModuleTimeout();
        this.staleMasterTimeout = configuration.getStaleMasterTimeout();

        this.disableIndexOptimization = esConfiguration.isDisableIndexOptimization();
        this.indexOptimizationMaxSegments = esConfiguration.getIndexOptimizationMaxNumSegments();

        this.gcWarningThreshold = configuration.getGcWarningThreshold().toString();
    }

    public ImmutableList<ConfigurationVariable> asList() {
        return ImmutableList.<ConfigurationVariable>builder()
                .add(ConfigurationVariable.create("inputbuffer_processors", inputBufferProcessors))
                .add(ConfigurationVariable.create("processbuffer_processors", processBufferProcessors))
                .add(ConfigurationVariable.create("outputbuffer_processors", outputBufferProcessors))
                .add(ConfigurationVariable.create("processor_wait_strategy", processorWaitStrategy))
                .add(ConfigurationVariable.create("inputbuffer_wait_strategy", inputBufferWaitStrategy))
                .add(ConfigurationVariable.create("inputbuffer_ring_size", inputBufferRingSize))
                .add(ConfigurationVariable.create("ring_size", ringSize))
                .add(ConfigurationVariable.create("plugin_dir", pluginDir))
                .add(ConfigurationVariable.create("node_id_file", nodeIdFile))
                .add(ConfigurationVariable.create("allow_highlighting", allowHighlighting))
                .add(ConfigurationVariable.create("allow_leading_wildcard_searches", allowLeadingWildcardSearches))
                .add(ConfigurationVariable.create("rotation_strategy", rotationStrategy))
                .add(ConfigurationVariable.create("retention_strategy", retentionStrategy))
                .add(ConfigurationVariable.create("elasticsearch_max_docs_per_index", maxDocsPerIndex))
                .add(ConfigurationVariable.create("elasticsearch_max_size_per_index", maxSizePerIndex))
                .add(ConfigurationVariable.create("elasticsearch_max_time_per_index", maxTimePerIndex))
                .add(ConfigurationVariable.create("elasticsearch_max_number_of_indices", maxNumberOfIndices))
                .add(ConfigurationVariable.create("elasticsearch_shards", shards))
                .add(ConfigurationVariable.create("elasticsearch_replicas", replicas))
                .add(ConfigurationVariable.create("stream_processing_timeout", streamProcessingTimeout))
                .add(ConfigurationVariable.create("stream_processing_max_faults", streamProcessingMaxFaults))
                .add(ConfigurationVariable.create("output_module_timeout", outputModuleTimeout))
                .add(ConfigurationVariable.create("stale_master_timeout", staleMasterTimeout))
                .add(ConfigurationVariable.create("disable_index_optimization", disableIndexOptimization))
                .add(ConfigurationVariable.create("index_optimization_max_num_segments", indexOptimizationMaxSegments))
                .add(ConfigurationVariable.create("gc_warning_threshold", gcWarningThreshold))
                .build();
    }

}
