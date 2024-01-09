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
package org.graylog.storage.opensearch2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.CreateDataStreamRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.GetDataStreamRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.GetDataStreamResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.PutComposableIndexTemplateRequest;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.ComposableIndexTemplate;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.DataStream;
import org.graylog.shaded.opensearch2.org.opensearch.common.compress.CompressedXContent;
import org.graylog2.indexer.datastream.DataStreamAdapter;
import org.graylog2.indexer.indices.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;

public class DataStreamAdapterOS2 implements DataStreamAdapter {

    private final Logger log = LoggerFactory.getLogger(DataStreamAdapterOS2.class);
    private final OpenSearchClient client;
    private final ObjectMapper objectMapper;

    @Inject
    public DataStreamAdapterOS2(OpenSearchClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean ensureDataStreamTemplate(@Nonnull String templateName, @Nonnull Template template, @Nonnull String timestampField) {
        final ComposableIndexTemplate.DataStreamTemplate datastreamTemplate =
                new ComposableIndexTemplate.DataStreamTemplate(new DataStream.TimestampField(timestampField));

        CompressedXContent serializedMapping = null;
        try {
            serializedMapping = new CompressedXContent(objectMapper.writeValueAsString(template.mappings()));
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize mappings for data stream", e);
        }
        var settings = org.graylog.shaded.opensearch2.org.opensearch.common.settings.Settings.builder().loadFromMap(template.settings()).build();
        var osTemplate = new org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.Template(settings, serializedMapping, null);
        var indexTemplate = new ComposableIndexTemplate(template.indexPatterns(), osTemplate, null, template.order(), null, null, datastreamTemplate);
        var request = new PutComposableIndexTemplateRequest()
                .name(templateName)
                .indexTemplate(indexTemplate);

        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().putIndexTemplate(request, requestOptions),
                "Unable to create data stream template " + templateName);

        return result.isAcknowledged();
    }

    @Override
    public void createDataStream(@Nonnull String dataStreamName) {
        CreateDataStreamRequest createDataStreamRequest = new CreateDataStreamRequest(dataStreamName);
        final GetDataStreamResponse dataStream = client.execute((c, requestOptions) ->
                c.indices().getDataStream(new GetDataStreamRequest(dataStreamName), requestOptions));
        if (dataStream.getDataStreams().isEmpty()) {
            client.execute((c, requestOptions) -> c.indices().createDataStream(createDataStreamRequest, requestOptions));
        }
    }
}
