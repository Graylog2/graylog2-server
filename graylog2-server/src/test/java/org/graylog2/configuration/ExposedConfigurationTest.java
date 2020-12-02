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
package org.graylog2.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.graylog2.Configuration;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ExposedConfigurationTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Test
    public void testCreateWithConfiguration() throws Exception {
        final Configuration configuration = new Configuration();
        final ExposedConfiguration c = ExposedConfiguration.create(configuration);

        assertThat(c.inputBufferProcessors()).isEqualTo(configuration.getInputbufferProcessors());
        assertThat(c.processBufferProcessors()).isEqualTo(configuration.getProcessBufferProcessors());
        assertThat(c.outputBufferProcessors()).isEqualTo(configuration.getOutputBufferProcessors());
        assertThat(c.processorWaitStrategy()).isEqualTo(configuration.getProcessorWaitStrategy().getClass().getName());
        assertThat(c.inputBufferWaitStrategy()).isEqualTo(configuration.getInputBufferWaitStrategy().getClass().getName());
        assertThat(c.inputBufferRingSize()).isEqualTo(configuration.getInputBufferRingSize());
        assertThat(c.ringSize()).isEqualTo(configuration.getRingSize());
        assertThat(c.binDir()).isEqualTo(configuration.getBinDir());
        assertThat(c.dataDir()).isEqualTo(configuration.getDataDir());
        assertThat(c.pluginDir()).isEqualTo(configuration.getPluginDir());
        assertThat(c.nodeIdFile()).isEqualTo(configuration.getNodeIdFile());
        assertThat(c.allowHighlighting()).isEqualTo(configuration.isAllowHighlighting());
        assertThat(c.allowLeadingWildcardSearches()).isEqualTo(configuration.isAllowLeadingWildcardSearches());
        assertThat(c.streamProcessingTimeout()).isEqualTo(configuration.getStreamProcessingTimeout());
        assertThat(c.streamProcessingMaxFaults()).isEqualTo(configuration.getStreamProcessingMaxFaults());
        assertThat(c.outputModuleTimeout()).isEqualTo(configuration.getOutputModuleTimeout());
        assertThat(c.staleMasterTimeout()).isEqualTo(configuration.getStaleMasterTimeout());
        assertThat(c.gcWarningThreshold()).isEqualTo(configuration.getGcWarningThreshold().toString());
    }

    @Test
    public void testSerialization() throws Exception {
        final Configuration configuration = new Configuration();
        final ExposedConfiguration c = ExposedConfiguration.create(configuration);

        final String json = objectMapper.writeValueAsString(c);
        assertThat((int) JsonPath.read(json, "$.inputbuffer_processors")).isEqualTo(c.inputBufferProcessors());
        assertThat((int) JsonPath.read(json, "$.processbuffer_processors")).isEqualTo(c.processBufferProcessors());
        assertThat((int) JsonPath.read(json, "$.outputbuffer_processors")).isEqualTo(c.outputBufferProcessors());
        assertThat((String) JsonPath.read(json, "$.processor_wait_strategy")).isEqualTo(c.processorWaitStrategy());
        assertThat((String) JsonPath.read(json, "$.inputbuffer_wait_strategy")).isEqualTo(c.inputBufferWaitStrategy());
        assertThat((int) JsonPath.read(json, "$.inputbuffer_ring_size")).isEqualTo(c.inputBufferRingSize());
        assertThat((int) JsonPath.read(json, "$.ring_size")).isEqualTo(c.ringSize());
        assertThat(URI.create(JsonPath.read(json, "$.bin_dir"))).isEqualTo(c.binDir().toUri());
        assertThat(URI.create(JsonPath.read(json, "$.data_dir"))).isEqualTo(c.dataDir().toUri());
        assertThat(URI.create(JsonPath.read(json, "$.plugin_dir"))).isEqualTo(c.pluginDir().toUri());
        assertThat((String) JsonPath.read(json, "$.node_id_file")).isEqualTo(c.nodeIdFile());
        assertThat((boolean) JsonPath.read(json, "$.allow_highlighting")).isEqualTo(c.allowHighlighting());
        assertThat((boolean) JsonPath.read(json, "$.allow_leading_wildcard_searches")).isEqualTo(c.allowLeadingWildcardSearches());
        assertThat((int) JsonPath.read(json, "$.stream_processing_timeout")).isEqualTo((int) c.streamProcessingTimeout());
        assertThat((int) JsonPath.read(json, "$.stream_processing_max_faults")).isEqualTo(c.streamProcessingMaxFaults());
        assertThat((int) JsonPath.read(json, "$.output_module_timeout")).isEqualTo((int) c.outputModuleTimeout());
        assertThat((int) JsonPath.read(json, "$.stale_master_timeout")).isEqualTo(c.staleMasterTimeout());
        assertThat((String) JsonPath.read(json, "$.gc_warning_threshold")).isEqualTo(c.gcWarningThreshold());
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
                "  \"bin_dir\": \"bin\"," +
                "  \"data_dir\": \"data\"," +
                "  \"plugin_dir\": \"plugin\"," +
                "  \"node_id_file\": \"/etc/graylog/server/node-id\"," +
                "  \"allow_highlighting\": false," +
                "  \"allow_leading_wildcard_searches\": false," +
                "  \"stream_processing_timeout\": 2000," +
                "  \"stream_processing_max_faults\": 3," +
                "  \"output_module_timeout\": 10000," +
                "  \"stale_master_timeout\": 2000," +
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
        assertThat(c.binDir()).isEqualTo(Paths.get((String) JsonPath.read(json, "$.bin_dir")));
        assertThat(c.dataDir()).isEqualTo(Paths.get((String) JsonPath.read(json, "$.data_dir")));
        assertThat(c.pluginDir()).isEqualTo(Paths.get((String) JsonPath.read(json, "$.plugin_dir")));
        assertThat(c.nodeIdFile()).isEqualTo(JsonPath.read(json, "$.node_id_file"));
        assertThat(c.allowHighlighting()).isEqualTo(JsonPath.read(json, "$.allow_highlighting"));
        assertThat(c.allowLeadingWildcardSearches()).isEqualTo(JsonPath.read(json, "$.allow_leading_wildcard_searches"));
        assertThat((int) c.streamProcessingTimeout()).isEqualTo(JsonPath.read(json, "$.stream_processing_timeout"));
        assertThat(c.streamProcessingMaxFaults()).isEqualTo(JsonPath.read(json, "$.stream_processing_max_faults"));
        assertThat((int) c.outputModuleTimeout()).isEqualTo(JsonPath.read(json, "$.output_module_timeout"));
        assertThat(c.staleMasterTimeout()).isEqualTo(JsonPath.read(json, "$.stale_master_timeout"));
        assertThat(c.gcWarningThreshold()).isEqualTo(JsonPath.read(json, "$.gc_warning_threshold"));
    }
}