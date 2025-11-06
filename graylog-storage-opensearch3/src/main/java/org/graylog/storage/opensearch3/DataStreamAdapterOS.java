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
package org.graylog.storage.opensearch3;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import org.graylog.shaded.opensearch2.org.opensearch.action.support.master.AcknowledgedResponse;
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.PutComposableIndexTemplateRequest;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.ComposableIndexTemplate;
import org.graylog.shaded.opensearch2.org.opensearch.cluster.metadata.DataStream;
import org.graylog.shaded.opensearch2.org.opensearch.common.compress.CompressedXContent;
import org.graylog.storage.opensearch3.ism.IsmApi;
import org.graylog2.indexer.datastream.DataStreamAdapter;
import org.graylog2.indexer.datastream.Policy;
import org.graylog2.indexer.datastream.policy.IsmPolicy;
import org.graylog2.indexer.indices.Template;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.indices.CreateDataStreamRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class DataStreamAdapterOS implements DataStreamAdapter {

    private final Logger log = LoggerFactory.getLogger(DataStreamAdapterOS.class);
    private final OfficialOpensearchClient opensearchClient;
    private final OpenSearchClient client;
    private final ObjectMapper objectMapper;
    private final IsmApi ismApi;
    private final OpenSearchIndicesClient indicesClient;

    @Inject
    public DataStreamAdapterOS(OfficialOpensearchClient opensearchClient, OpenSearchClient client, ObjectMapper objectMapper, IsmApi ismApi) {
        this.opensearchClient = opensearchClient;
        this.indicesClient = opensearchClient.sync().indices();
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

    @VisibleForTesting
    boolean deleteDataStreamTemplate(@Nonnull String templateName) {
        return opensearchClient.execute(() -> indicesClient.deleteIndexTemplate(r -> r.name(templateName)).acknowledged(),
                "Error deleting data stream template " + templateName);
    }

    @Override
    public void createDataStream(@Nonnull String dataStreamName) {
        CreateDataStreamRequest request = CreateDataStreamRequest.of(r -> r
                .name(dataStreamName)
        );
        opensearchClient.execute(() -> {
            try {
                indicesClient.createDataStream(request);
            } catch (org.opensearch.client.opensearch._types.OpenSearchException e) {
                if (e.response().error().type().equals("resource_already_exists_exception")) {
                    // this is expected, ignore the exception
                    log.debug("Data stream {} already exists, won't be created again", dataStreamName);
                } else {
                    throw e;
                }
            }
            return null;
        }, "");
    }

    /**
     * Updates all replica settings for system indices needed for ism and for the datastream indices to the value provided.
     *
     * @param dataStreamName name of data stream
     * @param replicas       number of replicas for indices
     */
    public void setNumberOfReplicas(@Nonnull String dataStreamName, int replicas) {
        try {
            indicesClient.putSettings(r -> r
                    .index(".opendistro-ism*")
                    .settings(s -> s.numberOfReplicas(replicas))
            );
        } catch (Exception e) {
            log.debug("Could not set replicas for .opendistro-ism system indices");
        }

        try {
            indicesClient.putSettings(r -> r
                    .index(".opendistro-job-scheduler-lock")
                    .settings(s -> s.numberOfReplicas(replicas))
            );
        } catch (Exception e) {
            log.debug("Could not set replicas for .opendistro-job-scheduler-lock system index. It might not exist yet.");
        }

        try {
            indicesClient.putTemplate(r -> r
                    .indexPatterns(List.of(".opendistro-job-scheduler-lock"))
                    .settings("number_of_replicas", JsonData.of(replicas))
            );
        } catch (Exception e) {
            log.debug("Could not set replicas for data stream system indices");
        }

        try {
            opensearchClient.sync().cluster().putSettings(s ->
                    s.persistent("opendistro.index_state_management.history.number_of_replicas", JsonData.of(replicas))
            );
        } catch (Exception e) {
            log.debug("Could not set default replicas for ism history");
        }

        try {
            indicesClient.putSettings(r -> r
                    .index(dataStreamName)
                    .settings(s -> s.numberOfReplicas(replicas))
            );
        } catch (Exception e) {
            log.debug("Could not set replicas for data stream {}", dataStreamName);
        }
    }

    public boolean deleteDataStream(@Nonnull String dataStreamName) {
        return opensearchClient.execute(() -> indicesClient.deleteDataStream(r -> r.name(dataStreamName)).acknowledged(),
                "Error deleting data stream " + dataStreamName);
    }

    @VisibleForTesting
    List<org.opensearch.client.opensearch.indices.DataStream> getDataStream(@Nonnull String dataStreamName) {
        return opensearchClient.execute(() -> indicesClient.getDataStream().dataStreams(), "Error getting data streams");
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
        Optional<IsmPolicy> osPolicy;
        try {
            osPolicy = ismApi.getPolicy(id);
        } catch (Exception e) {
            // delete non-readable policies
            ismApi.removePolicyFromIndex(dataStreamName);
            ismApi.deletePolicy(id);
            osPolicy = Optional.empty();
        }
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
