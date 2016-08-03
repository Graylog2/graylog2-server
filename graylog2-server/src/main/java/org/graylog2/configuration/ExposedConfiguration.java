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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.Configuration;

/**
 * List of configuration values that are safe to return, i.e. do not include any sensitive
 * information. Building a list manually because we need to guarantee never to return any
 * sensitive variables like passwords etc. - See this as a whitelist approach.
 */
@JsonAutoDetect
@AutoValue
public abstract class ExposedConfiguration {
    @JsonProperty("inputbuffer_processors")
    public abstract int inputBufferProcessors();

    @JsonProperty("processbuffer_processors")
    public abstract int processBufferProcessors();

    @JsonProperty("outputbuffer_processors")
    public abstract int outputBufferProcessors();

    @JsonProperty("processor_wait_strategy")
    public abstract String processorWaitStrategy();

    @JsonProperty("inputbuffer_wait_strategy")
    public abstract String inputBufferWaitStrategy();

    @JsonProperty("inputbuffer_ring_size")
    public abstract int inputBufferRingSize();

    @JsonProperty("ring_size")
    public abstract int ringSize();

    @JsonProperty("plugin_dir")
    public abstract String pluginDir();

    @JsonProperty("node_id_file")
    public abstract String nodeIdFile();

    @JsonProperty("allow_highlighting")
    public abstract boolean allowHighlighting();

    @JsonProperty("allow_leading_wildcard_searches")
    public abstract boolean allowLeadingWildcardSearches();

    @JsonProperty("elasticsearch_shards")
    public abstract int shards();

    @JsonProperty("elasticsearch_replicas")
    public abstract int replicas();

    @JsonProperty("stream_processing_timeout")
    public abstract long streamProcessingTimeout();

    @JsonProperty("stream_processing_max_faults")
    public abstract int streamProcessingMaxFaults();

    @JsonProperty("output_module_timeout")
    public abstract long outputModuleTimeout();

    @JsonProperty("stale_master_timeout")
    public abstract int staleMasterTimeout();

    @JsonProperty("disable_index_optimization")
    public abstract boolean disableIndexOptimization();

    @JsonProperty("index_optimization_max_num_segments")
    public abstract int indexOptimizationMaxSegments();

    @JsonProperty("gc_warning_threshold")
    public abstract String gcWarningThreshold();

    public static ExposedConfiguration create(Configuration configuration, ElasticsearchConfiguration esConfiguration) {
        return create(
                configuration.getInputbufferProcessors(),
                configuration.getProcessBufferProcessors(),
                configuration.getOutputBufferProcessors(),
                configuration.getProcessorWaitStrategy().getClass().getName(),
                configuration.getInputBufferWaitStrategy().getClass().getName(),
                configuration.getInputBufferRingSize(),
                configuration.getRingSize(),
                configuration.getPluginDir(),
                configuration.getNodeIdFile(),
                configuration.isAllowHighlighting(),
                configuration.isAllowLeadingWildcardSearches(),
                esConfiguration.getShards(),
                esConfiguration.getReplicas(),
                configuration.getStreamProcessingTimeout(),
                configuration.getStreamProcessingMaxFaults(),
                configuration.getOutputModuleTimeout(),
                configuration.getStaleMasterTimeout(),
                esConfiguration.isDisableIndexOptimization(),
                esConfiguration.getIndexOptimizationMaxNumSegments(),
                configuration.getGcWarningThreshold().toString());
    }

    @JsonCreator
    public static ExposedConfiguration create(
            @JsonProperty("inputbuffer_processors") int inputBufferProcessors,
            @JsonProperty("processbuffer_processors") int processBufferProcessors,
            @JsonProperty("outputbuffer_processors") int outputBufferProcessors,
            @JsonProperty("processor_wait_strategy") String processorWaitStrategy,
            @JsonProperty("inputbuffer_wait_strategy") String inputBufferWaitStrategy,
            @JsonProperty("inputbuffer_ring_size") int inputBufferRingSize,
            @JsonProperty("ring_size") int ringSize,
            @JsonProperty("plugin_dir") String pluginDir,
            @JsonProperty("node_id_file") String nodeIdFile,
            @JsonProperty("allow_highlighting") boolean allowHighlighting,
            @JsonProperty("allow_leading_wildcard_searches") boolean allowLeadingWildcardSearches,
            @JsonProperty("elasticsearch_shards") int shards,
            @JsonProperty("elasticsearch_replicas") int replicas,
            @JsonProperty("stream_processing_timeout") long streamProcessingTimeout,
            @JsonProperty("stream_processing_max_faults") int streamProcessingMaxFaults,
            @JsonProperty("output_module_timeout") long outputModuleTimeout,
            @JsonProperty("stale_master_timeout") int staleMasterTimeout,
            @JsonProperty("disable_index_optimization") boolean disableIndexOptimization,
            @JsonProperty("index_optimization_max_num_segments") int indexOptimizationMaxSegments,
            @JsonProperty("gc_warning_threshold") String gcWarningThreshold) {
        return new AutoValue_ExposedConfiguration(
                inputBufferProcessors,
                processBufferProcessors,
                outputBufferProcessors,
                processorWaitStrategy,
                inputBufferWaitStrategy,
                inputBufferRingSize,
                ringSize,
                pluginDir,
                nodeIdFile,
                allowHighlighting,
                allowLeadingWildcardSearches,
                shards,
                replicas,
                streamProcessingTimeout,
                streamProcessingMaxFaults,
                outputModuleTimeout,
                staleMasterTimeout,
                disableIndexOptimization,
                indexOptimizationMaxSegments,
                gcWarningThreshold);
    }

}
