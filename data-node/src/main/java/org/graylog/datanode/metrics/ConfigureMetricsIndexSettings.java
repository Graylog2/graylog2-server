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

import org.apache.commons.io.FileUtils;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.management.OpensearchProcess;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.StateMachineTracer;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.Request;
import org.graylog.shaded.opensearch2.org.opensearch.client.RequestOptions;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.CreateDataStreamRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.PutComposableIndexTemplateRequest;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.ComposableIndexTemplate;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.DataStream;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.Template;
import org.graylog.shaded.opensearch2.org.opensearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

public class ConfigureMetricsIndexSettings implements StateMachineTracer {

    private final Logger log = LoggerFactory.getLogger(ConfigureMetricsIndexSettings.class);

    private final OpensearchProcess process;
    private final Configuration configuration;

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
            process.restClient().ifPresent(client -> {

                updateDataStreamTemplate(client);
                configureMetricsIsm(client);

            });
        }
    }

    private void updateDataStreamTemplate(RestHighLevelClient client) {
        Settings settings = Settings.EMPTY; // use default settings
        ComposableIndexTemplate template = new ComposableIndexTemplate(List.of(configuration.getMetricsStream() + "*"),
                new Template(settings, null, null),
                null, null, null, null, // all default
                new ComposableIndexTemplate.DataStreamTemplate(new DataStream.TimestampField(configuration.getMetricsTimestamp())));
        var request = new PutComposableIndexTemplateRequest()
                .name(configuration.getMetricsTemplate())
                .indexTemplate(template);
        try {
            final AcknowledgedResponse response = client.indices().putIndexTemplate(request, RequestOptions.DEFAULT);
            if (!response.isAcknowledged()) {
                log.error("Unable to create metrics data stream template");
            }
        } catch (IOException e) {
            log.error("Unable to create metrics data stream template");
        }
    }

    private void createDataStreamBackingIndex(RestHighLevelClient client) {
        try {
            CreateDataStreamRequest createDataStreamRequest = new CreateDataStreamRequest(configuration.getMetricsStream());
            final AcknowledgedResponse response = client.indices().createDataStream(createDataStreamRequest, RequestOptions.DEFAULT);
            if (!response.isAcknowledged()) {
                log.error("Unable to create backing index for metrics data stream");
            }
        } catch (IOException e) {
            log.error("Unable to create backing index for metrics data stream");
        }
    }

    private void configureMetricsIsm(RestHighLevelClient client) {
        //TODO dynamically create ism with config values using either jackson or freemarker
        try {
            final URL resource = getClass().getResource("metrics-ism.json");
            File metricsIsm = new File(resource.getFile());
            final String ism = FileUtils.readFileToString(metricsIsm, Charset.defaultCharset());
            final Request ismRequest = new Request("PUT", "_plugins/_ism/policies/gl_purge_metrics");
            ismRequest.setJsonEntity(ism);
            final Response response = client.getLowLevelClient().performRequest(ismRequest);
            if (response.getStatusLine().getStatusCode() != 200) {
                log.error("Error creating ism for metrics rollup (Status {})", response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            log.error("Could not read ism config for metrics");
        }

    }

}
