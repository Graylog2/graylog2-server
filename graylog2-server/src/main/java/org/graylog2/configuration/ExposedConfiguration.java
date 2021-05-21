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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.Configuration;

import java.nio.file.Path;

/**
 * List of configuration values that are safe to return, i.e. do not include any sensitive
 * information. Building a list manually because we need to guarantee never to return any
 * sensitive variables like passwords etc. - See this as a whitelist approach.
 */
@JsonAutoDetect
@AutoValue
@WithBeanGetter
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

    @JsonProperty("bin_dir")
    public abstract Path binDir();

    @JsonProperty("data_dir")
    public abstract Path dataDir();

    @JsonProperty("plugin_dir")
    public abstract Path pluginDir();

    @JsonProperty("node_id_file")
    public abstract String nodeIdFile();

    @JsonProperty("allow_highlighting")
    public abstract boolean allowHighlighting();

    @JsonProperty("allow_leading_wildcard_searches")
    public abstract boolean allowLeadingWildcardSearches();

    @JsonProperty("stream_processing_timeout")
    public abstract long streamProcessingTimeout();

    @JsonProperty("stream_processing_max_faults")
    public abstract int streamProcessingMaxFaults();

    @JsonProperty("output_module_timeout")
    public abstract long outputModuleTimeout();

    @JsonProperty("stale_master_timeout")
    public abstract int staleMasterTimeout();

    @JsonProperty("gc_warning_threshold")
    public abstract String gcWarningThreshold();

    @JsonProperty("forwarder_enabled")
    public abstract boolean forwarderEnabled();

    public static ExposedConfiguration create(Configuration configuration) {
        return create(
                configuration.getInputbufferProcessors(),
                configuration.getProcessBufferProcessors(),
                configuration.getOutputBufferProcessors(),
                configuration.getProcessorWaitStrategy().getClass().getName(),
                configuration.getInputBufferWaitStrategy().getClass().getName(),
                configuration.getInputBufferRingSize(),
                configuration.getRingSize(),
                configuration.getBinDir(),
                configuration.getDataDir(),
                configuration.getPluginDir(),
                configuration.getNodeIdFile(),
                configuration.isAllowHighlighting(),
                configuration.isAllowLeadingWildcardSearches(),
                configuration.getStreamProcessingTimeout(),
                configuration.getStreamProcessingMaxFaults(),
                configuration.getOutputModuleTimeout(),
                configuration.getStaleMasterTimeout(),
                configuration.getGcWarningThreshold().toString(),
                configuration.isForwarderEnabled());
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
            @JsonProperty("bin_dir") Path binDir,
            @JsonProperty("data_dir") Path dataDir,
            @JsonProperty("plugin_dir") Path pluginDir,
            @JsonProperty("node_id_file") String nodeIdFile,
            @JsonProperty("allow_highlighting") boolean allowHighlighting,
            @JsonProperty("allow_leading_wildcard_searches") boolean allowLeadingWildcardSearches,
            @JsonProperty("stream_processing_timeout") long streamProcessingTimeout,
            @JsonProperty("stream_processing_max_faults") int streamProcessingMaxFaults,
            @JsonProperty("output_module_timeout") long outputModuleTimeout,
            @JsonProperty("stale_master_timeout") int staleMasterTimeout,
            @JsonProperty("gc_warning_threshold") String gcWarningThreshold,
            @JsonProperty("forwarder_enabled") boolean forwarderEnabled) {
        return new AutoValue_ExposedConfiguration(
                inputBufferProcessors,
                processBufferProcessors,
                outputBufferProcessors,
                processorWaitStrategy,
                inputBufferWaitStrategy,
                inputBufferRingSize,
                ringSize,
                binDir,
                dataDir,
                pluginDir,
                nodeIdFile,
                allowHighlighting,
                allowLeadingWildcardSearches,
                streamProcessingTimeout,
                streamProcessingMaxFaults,
                outputModuleTimeout,
                staleMasterTimeout,
                gcWarningThreshold,
                forwarderEnabled);
    }

}
