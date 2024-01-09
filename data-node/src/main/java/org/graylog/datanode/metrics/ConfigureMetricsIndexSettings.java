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
package org.graylog.datanode.metrics;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.management.OpensearchProcess;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.StateMachineTracer;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.storage.opensearch2.DataStreamAdapterOS2;
import org.graylog2.indexer.datastream.DataStreamAdapter;
import org.graylog2.indexer.indices.Template;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class ConfigureMetricsIndexSettings implements StateMachineTracer {

    private final Logger log = LoggerFactory.getLogger(ConfigureMetricsIndexSettings.class);

    private final OpensearchProcess process;
    private final Configuration configuration;
    private DataStreamAdapter dataStreamAdapter;

    public ConfigureMetricsIndexSettings(OpensearchProcess process, Configuration configuration) {
        this.process = process;
        this.configuration = configuration;
    }

    @Override
    public void trigger(ProcessEvent trigger) {
    }

    @Override
    public void transition(ProcessEvent trigger, ProcessState source, ProcessState destination) {
        if (destination == ProcessState.AVAILABLE && source == ProcessState.STARTING) {
            process.openSearchClient().ifPresent(client -> {
                if (dataStreamAdapter == null) {
                    dataStreamAdapter = new DataStreamAdapterOS2(client, new ObjectMapperProvider().get());
                }
                Map<String, Object> mappings =
                        ImmutableMap.of("timestamp",
                                ImmutableMap.of(
                                        "type", "date",
                                        "format", "yyyy-MM-dd HH:mm:ss.SSS||strict_date_optional_time||epoch_millis"),
                                "node", ImmutableMap.of("type", "keyword")
                        );
                Template template = new Template(List.of(configuration.getMetricsStream() + "*"),
                        new Template.Mappings(mappings), 1L, new Template.Settings(Map.of()));
                dataStreamAdapter.ensureDataStreamTemplate(configuration.getMetricsTemplate(), template, configuration.getMetricsTimestamp());
                dataStreamAdapter.createDataStream(configuration.getMetricsStream());
            });
            process.restClient().ifPresent(this::configureMetricsIsm);


        }
    }

    private void updateDataStreamTemplate(RestHighLevelClient client) {

    }

    private void configureMetricsIsm(RestHighLevelClient client) {
        //TODO dynamically create ism with config values using either jackson or freemarker
        try {
            final Request delRequest = new Request("DELETE", "_plugins/_ism/policies/gl_purge_metrics");
            final Response delResponse = client.getLowLevelClient().performRequest(delRequest);

            final URL resource = getClass().getResource("metrics-ism.json");
            File metricsIsm = new File(resource.getFile());
            final String ism = FileUtils.readFileToString(metricsIsm, Charset.defaultCharset());
            final Request ismRequest = new Request("PUT", "_plugins/_ism/policies/gl_purge_metrics");
            ismRequest.setJsonEntity(ism);
            final Response response = client.getLowLevelClient().performRequest(ismRequest);
            if (response.getStatusLine().getStatusCode() != 201) {
                log.error("Error creating ism for metrics rollup (Status {})", response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            log.error("Could not read ism config for metrics", e);
        }

    }

}
