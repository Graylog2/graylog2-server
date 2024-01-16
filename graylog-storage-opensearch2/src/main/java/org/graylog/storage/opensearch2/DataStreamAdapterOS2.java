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
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.DeleteComposableIndexTemplateRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.DeleteDataStreamRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.GetDataStreamRequest;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.GetDataStreamResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.PutComposableIndexTemplateRequest;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.ComposableIndexTemplate;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.DataStream;
import org.graylog.shaded.opensearch2.org.opensearch.common.compress.CompressedXContent;
import org.graylog.storage.opensearch2.ism.IsmApi;
import org.graylog.storage.opensearch2.ism.policy.IsmPolicy;
import org.graylog2.indexer.IndexNotFoundException;
import org.graylog2.indexer.datastream.DataStreamAdapter;
import org.graylog2.indexer.datastream.Policy;
import org.graylog2.indexer.indices.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DataStreamAdapterOS2 implements DataStreamAdapter {

    private final Logger log = LoggerFactory.getLogger(DataStreamAdapterOS2.class);
    private final OpenSearchClient client;
    private final ObjectMapper objectMapper;
    private final IsmApi ismApi;

    @Inject
    public DataStreamAdapterOS2(OpenSearchClient client, ObjectMapper objectMapper, IsmApi ismApi) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.ismApi = ismApi;
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

    protected boolean deleteDataStreamTemplate(@Nonnull String templateName) {
        final DeleteComposableIndexTemplateRequest request = new DeleteComposableIndexTemplateRequest(templateName);
        final AcknowledgedResponse result = client.execute((c, requestOptions) -> c.indices().deleteIndexTemplate(request, requestOptions),
                "Unable to delete data stream template " + templateName);
        return result.isAcknowledged();
    }

    @Override
    public void createDataStream(@Nonnull String dataStreamName) {
        CreateDataStreamRequest createDataStreamRequest = new CreateDataStreamRequest(dataStreamName);
        final GetDataStreamResponse dataStream;
        try {
            client.execute((c, requestOptions) ->
                    c.indices().getDataStream(new GetDataStreamRequest(dataStreamName), requestOptions));
        } catch (IndexNotFoundException e) {
            client.execute((c, requestOptions) -> c.indices().createDataStream(createDataStreamRequest, requestOptions));
        }
    }

    public boolean deleteDataStream(@Nonnull String dataStreamName) {
        return client.execute((c, requestOptions) ->
                c.indices().deleteDataStream(new DeleteDataStreamRequest(dataStreamName), requestOptions)).isAcknowledged();
    }

    protected List<org.graylog.shaded.opensearch2.org.opensearch.client.indices.DataStream> getDataStream(@Nonnull String dataStreamName) {
        final GetDataStreamResponse dataStream = client.execute((c, requestOptions) ->
                c.indices().getDataStream(new GetDataStreamRequest(dataStreamName), requestOptions));
        return dataStream.getDataStreams();
    }

    @Override
    public void applyIsmPolicy(@Nonnull String dataStreamName, @Nonnull Policy policy) {
        // this might need to be adjusted in the future to using versioning for the ism policy.
        // for the time being, we will just remove and reapply the policy to the data stream.
        IsmPolicy ismPolicy = (IsmPolicy) policy;
        final String id = ismPolicy.id();
        if (Objects.isNull(id)) {
            throw new IllegalArgumentException("Policy Id may not be null");
        }
        final Optional<IsmPolicy> osPolicy = ismApi.getPolicy(id);
        if (osPolicy.isPresent()) {
            ismApi.removePolicyFromIndex(dataStreamName);
            ismApi.deletePolicy(id);
        }
        ismApi.createPolicy(ismPolicy.id(), new IsmPolicy(ismPolicy.policy()));
        ismApi.addPolicyToIndex(id, dataStreamName);
    }

    public void deleteIsmPolicy(@Nonnull String policyId) {
        ismApi.deletePolicy(policyId);
    }


}
