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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.graylog2.Configuration;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExposedConfigurationTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void testCreateWithConfiguration() throws Exception {
        final Configuration configuration = new Configuration();
        final ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
        final ExposedConfiguration c = ExposedConfiguration.create(configuration, elasticsearchConfiguration);

        assertThat(c.inputBufferProcessors()).isEqualTo(configuration.getInputbufferProcessors());
        assertThat(c.processBufferProcessors()).isEqualTo(configuration.getProcessBufferProcessors());
        assertThat(c.outputBufferProcessors()).isEqualTo(configuration.getOutputBufferProcessors());
        assertThat(c.processorWaitStrategy()).isEqualTo(configuration.getProcessorWaitStrategy().getClass().getName());
        assertThat(c.inputBufferWaitStrategy()).isEqualTo(configuration.getInputBufferWaitStrategy().getClass().getName());
        assertThat(c.inputBufferRingSize()).isEqualTo(configuration.getInputBufferRingSize());
        assertThat(c.ringSize()).isEqualTo(configuration.getRingSize());
        assertThat(c.pluginDir()).isEqualTo(configuration.getPluginDir());
        assertThat(c.nodeIdFile()).isEqualTo(configuration.getNodeIdFile());
        assertThat(c.allowHighlighting()).isEqualTo(configuration.isAllowHighlighting());
        assertThat(c.allowLeadingWildcardSearches()).isEqualTo(configuration.isAllowLeadingWildcardSearches());
        assertThat(c.rotationStrategy()).isEqualTo(elasticsearchConfiguration.getRotationStrategy());
        assertThat(c.retentionStrategy()).isEqualTo(elasticsearchConfiguration.getRetentionStrategy());
        assertThat(c.maxDocsPerIndex()).isEqualTo(elasticsearchConfiguration.getMaxDocsPerIndex());
        assertThat(c.maxSizePerIndex()).isEqualTo(elasticsearchConfiguration.getMaxSizePerIndex());
        assertThat(c.maxTimePerIndex()).isEqualTo(elasticsearchConfiguration.getMaxTimePerIndex());
        assertThat(c.maxNumberOfIndices()).isEqualTo(elasticsearchConfiguration.getMaxNumberOfIndices());
        assertThat(c.shards()).isEqualTo(elasticsearchConfiguration.getShards());
        assertThat(c.replicas()).isEqualTo(elasticsearchConfiguration.getReplicas());
        assertThat(c.streamProcessingTimeout()).isEqualTo(configuration.getStreamProcessingTimeout());
        assertThat(c.streamProcessingMaxFaults()).isEqualTo(configuration.getStreamProcessingMaxFaults());
        assertThat(c.outputModuleTimeout()).isEqualTo(configuration.getOutputModuleTimeout());
        assertThat(c.staleMasterTimeout()).isEqualTo(configuration.getStaleMasterTimeout());
        assertThat(c.disableIndexOptimization()).isEqualTo(elasticsearchConfiguration.isDisableIndexOptimization());
        assertThat(c.indexOptimizationMaxSegments()).isEqualTo(elasticsearchConfiguration.getIndexOptimizationMaxNumSegments());
        assertThat(c.gcWarningThreshold()).isEqualTo(configuration.getGcWarningThreshold().toString());
    }

    @Test
    public void testSerialization() throws Exception {
        final Configuration configuration = new Configuration();
        final ElasticsearchConfiguration elasticsearchConfiguration = new ElasticsearchConfiguration();
        final ExposedConfiguration c = ExposedConfiguration.create(configuration, elasticsearchConfiguration);

        final String json = objectMapper.writeValueAsString(c);
        assertThat(JsonPath.read(json, "$.inputbuffer_processors")).isEqualTo(c.inputBufferProcessors());
        assertThat(JsonPath.read(json, "$.processbuffer_processors")).isEqualTo(c.processBufferProcessors());
        assertThat(JsonPath.read(json, "$.outputbuffer_processors")).isEqualTo(c.outputBufferProcessors());
        assertThat(JsonPath.read(json, "$.processor_wait_strategy")).isEqualTo(c.processorWaitStrategy());
        assertThat(JsonPath.read(json, "$.inputbuffer_wait_strategy")).isEqualTo(c.inputBufferWaitStrategy());
        assertThat(JsonPath.read(json, "$.inputbuffer_ring_size")).isEqualTo(c.inputBufferRingSize());
        assertThat(JsonPath.read(json, "$.ring_size")).isEqualTo(c.ringSize());
        assertThat(JsonPath.read(json, "$.plugin_dir")).isEqualTo(c.pluginDir());
        assertThat(JsonPath.read(json, "$.node_id_file")).isEqualTo(c.nodeIdFile());
        assertThat(JsonPath.read(json, "$.allow_highlighting")).isEqualTo(c.allowHighlighting());
        assertThat(JsonPath.read(json, "$.allow_leading_wildcard_searches")).isEqualTo(c.allowLeadingWildcardSearches());
        assertThat(JsonPath.read(json, "$.rotation_strategy")).isEqualTo(c.rotationStrategy());
        assertThat(JsonPath.read(json, "$.retention_strategy")).isEqualTo(c.retentionStrategy());
        assertThat(JsonPath.read(json, "$.elasticsearch_max_docs_per_index")).isEqualTo(c.maxDocsPerIndex());
        assertThat(JsonPath.read(json, "$.elasticsearch_max_size_per_index")).isEqualTo((int) c.maxSizePerIndex());
        assertThat(JsonPath.read(json, "$.elasticsearch_max_time_per_index")).isEqualTo(c.maxTimePerIndex().toString());
        assertThat(JsonPath.read(json, "$.elasticsearch_max_number_of_indices")).isEqualTo(c.maxNumberOfIndices());
        assertThat(JsonPath.read(json, "$.elasticsearch_shards")).isEqualTo(c.shards());
        assertThat(JsonPath.read(json, "$.elasticsearch_replicas")).isEqualTo(c.replicas());
        assertThat(JsonPath.read(json, "$.stream_processing_timeout")).isEqualTo((int) c.streamProcessingTimeout());
        assertThat(JsonPath.read(json, "$.stream_processing_max_faults")).isEqualTo(c.streamProcessingMaxFaults());
        assertThat(JsonPath.read(json, "$.output_module_timeout")).isEqualTo((int) c.outputModuleTimeout());
        assertThat(JsonPath.read(json, "$.stale_master_timeout")).isEqualTo(c.staleMasterTimeout());
        assertThat(JsonPath.read(json, "$.disable_index_optimization")).isEqualTo(c.disableIndexOptimization());
        assertThat(JsonPath.read(json, "$.index_optimization_max_num_segments")).isEqualTo(c.indexOptimizationMaxSegments());
        assertThat(JsonPath.read(json, "$.gc_warning_threshold")).isEqualTo(c.gcWarningThreshold());
    }

    @Test
    public void testDeserialization() throws Exception {
        final String json = "{" +
                "  \"inputbuffer_processors\": 2," +
                "  \"processbuffer_processors\": 5," +
                "  \"outputbuffer_processors\": 3," +
                "  \"processor_wait_strategy\": \"com.lmax.disruptor.BlockingWaitStrategy\"," +
                "  \"inputbuffer_wait_strategy\": \"com.lmax.disruptor.BlockingWaitStrategy\"," +
                "  \"inputbuffer_ring_size\": 65536," +
                "  \"ring_size\": 65536," +
                "  \"plugin_dir\": \"plugin\"," +
                "  \"node_id_file\": \"/etc/graylog/server/node-id\"," +
                "  \"allow_highlighting\": false," +
                "  \"allow_leading_wildcard_searches\": false," +
                "  \"rotation_strategy\": \"count\"," +
                "  \"retention_strategy\": \"delete\"," +
                "  \"elasticsearch_max_docs_per_index\": 80000000," +
                "  \"elasticsearch_max_size_per_index\": 1073741824," +
                "  \"elasticsearch_max_time_per_index\": \"P1D\"," +
                "  \"elasticsearch_max_number_of_indices\": 20," +
                "  \"elasticsearch_shards\": 4," +
                "  \"elasticsearch_replicas\": 0," +
                "  \"stream_processing_timeout\": 2000," +
                "  \"stream_processing_max_faults\": 3," +
                "  \"output_module_timeout\": 10000," +
                "  \"stale_master_timeout\": 2000," +
                "  \"disable_index_optimization\": false," +
                "  \"index_optimization_max_num_segments\": 1," +
                "  \"gc_warning_threshold\": \"1 second\"" +
                "}";

        final ExposedConfiguration c = objectMapper.readValue(json, ExposedConfiguration.class);

        assertThat(c.inputBufferProcessors()).isEqualTo(JsonPath.read(json, "$.inputbuffer_processors"));
        assertThat(c.processBufferProcessors()).isEqualTo(JsonPath.read(json, "$.processbuffer_processors"));
        assertThat(c.outputBufferProcessors()).isEqualTo(JsonPath.read(json, "$.outputbuffer_processors"));
        assertThat(c.processorWaitStrategy()).isEqualTo(JsonPath.read(json, "$.processor_wait_strategy"));
        assertThat(c.inputBufferWaitStrategy()).isEqualTo(JsonPath.read(json, "$.inputbuffer_wait_strategy"));
        assertThat(c.inputBufferRingSize()).isEqualTo(JsonPath.read(json, "$.inputbuffer_ring_size"));
        assertThat(c.ringSize()).isEqualTo(JsonPath.read(json, "$.ring_size"));
        assertThat(c.pluginDir()).isEqualTo(JsonPath.read(json, "$.plugin_dir"));
        assertThat(c.nodeIdFile()).isEqualTo(JsonPath.read(json, "$.node_id_file"));
        assertThat(c.allowHighlighting()).isEqualTo(JsonPath.read(json, "$.allow_highlighting"));
        assertThat(c.allowLeadingWildcardSearches()).isEqualTo(JsonPath.read(json, "$.allow_leading_wildcard_searches"));
        assertThat(c.rotationStrategy()).isEqualTo(JsonPath.read(json, "$.rotation_strategy"));
        assertThat(c.retentionStrategy()).isEqualTo(JsonPath.read(json, "$.retention_strategy"));
        assertThat(c.maxDocsPerIndex()).isEqualTo(JsonPath.read(json, "$.elasticsearch_max_docs_per_index"));
        assertThat((int) c.maxSizePerIndex()).isEqualTo(JsonPath.read(json, "$.elasticsearch_max_size_per_index"));
        assertThat(c.maxTimePerIndex().toString()).isEqualTo(JsonPath.read(json, "$.elasticsearch_max_time_per_index"));
        assertThat(c.maxNumberOfIndices()).isEqualTo(JsonPath.read(json, "$.elasticsearch_max_number_of_indices"));
        assertThat(c.shards()).isEqualTo(JsonPath.read(json, "$.elasticsearch_shards"));
        assertThat(c.replicas()).isEqualTo(JsonPath.read(json, "$.elasticsearch_replicas"));
        assertThat((int) c.streamProcessingTimeout()).isEqualTo(JsonPath.read(json, "$.stream_processing_timeout"));
        assertThat(c.streamProcessingMaxFaults()).isEqualTo(JsonPath.read(json, "$.stream_processing_max_faults"));
        assertThat((int) c.outputModuleTimeout()).isEqualTo(JsonPath.read(json, "$.output_module_timeout"));
        assertThat(c.staleMasterTimeout()).isEqualTo(JsonPath.read(json, "$.stale_master_timeout"));
        assertThat(c.disableIndexOptimization()).isEqualTo(JsonPath.read(json, "$.disable_index_optimization"));
        assertThat(c.indexOptimizationMaxSegments()).isEqualTo(JsonPath.read(json, "$.index_optimization_max_num_segments"));
        assertThat(c.gcWarningThreshold()).isEqualTo(JsonPath.read(json, "$.gc_warning_threshold"));
    }
}