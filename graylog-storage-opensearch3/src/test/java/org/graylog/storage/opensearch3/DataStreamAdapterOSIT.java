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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.storage.opensearch3.ism.IsmApi;
import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.testing.elasticsearch.SearchInstance;
import org.graylog2.indexer.datastream.policy.IsmPolicy;
import org.graylog2.indexer.datastream.policy.IsmPolicyTest;
import org.graylog2.indexer.indices.Template;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.indices.DataStream;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class DataStreamAdapterOSIT {

    @SearchInstance
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    ObjectMapper objectMapper = new ObjectMapperProvider().get();

    IsmApi ismApi = new IsmApi(openSearchInstance.getOfficialOpensearchClient(), objectMapper);

    DataStreamAdapterOS dataStreamAdapter = new DataStreamAdapterOS(openSearchInstance.getOfficialOpensearchClient(),
            ismApi);


    @Test
    public void testCreateDataStreamAndApplyPolicy() {
        String stream = "testdatastream";

        // create template for data stream
        Template template = new Template(List.of(stream + "*"), new Template.Mappings(Map.of()), 1L,
                new Template.Settings(Map.of("number_of_replicas", 0)));
        String templateName = "datastream-test-template";
        boolean ack = dataStreamAdapter.ensureDataStreamTemplate(templateName, template, "timestamp");
        assertThat(ack).isTrue();

        // create data stream and backing index
        dataStreamAdapter.createDataStream(stream);
        List<DataStream> dataStreams = dataStreamAdapter.getDataStream(stream);
        dataStreamAdapter.createDataStream(stream);
        assertThat(dataStreams).hasSize(1);

        // assert that existing data stream will not be recreated
        List<DataStream> dataStreams2 = dataStreamAdapter.getDataStream(stream);
        assertThat(dataStreams2).hasSize(1);
        assertThat(dataStreams.get(0)).isEqualTo(dataStreams2.get(0));

        // apply ism policy
        IsmPolicy simpleTestPolicy = IsmPolicyTest.createSimpleTestPolicy();
        dataStreamAdapter.applyIsmPolicy(stream, simpleTestPolicy);

        // clean up to avoid exception when deleting indices
        ack = dataStreamAdapter.deleteDataStream(stream);
        assertThat(ack).isTrue();

        assert simpleTestPolicy.id() != null;
        dataStreamAdapter.deleteIsmPolicy(simpleTestPolicy.id());

        ack = dataStreamAdapter.deleteDataStreamTemplate(templateName);
        assertThat(ack).isTrue();

    }

    /**
     * A policy without an {@code ism_template} is only attached to the backing indices that exist when it is applied.
     * Every index created by a later rollover stays unmanaged, so no rollup or deletion ever runs on it.
     *
     * @see <a href="https://github.com/Graylog2/graylog2-server/issues/27040">#27040</a>
     */
    @Test
    public void testBackingIndexCreatedByRolloverIsManagedAutomatically() {
        // must not overlap the index template pattern of the other test in this class
        final String stream = "ismtemplatedatastream";
        final String templateName = stream + "-template";
        final String policyId = "graylog-ism-template-test-policy";

        final Template template = new Template(List.of(stream + "*"), new Template.Mappings(Map.of()), 1L,
                new Template.Settings(Map.of("number_of_replicas", 0)));
        assertThat(dataStreamAdapter.ensureDataStreamTemplate(templateName, template, "timestamp")).isTrue();
        dataStreamAdapter.createDataStream(stream);

        try {
            dataStreamAdapter.applyIsmPolicy(stream, IsmPolicyTest.createTestPolicyWithIsmTemplate(policyId, stream));

            // the stored policy must carry the ism_template, otherwise OpenSearch has no way to attach it to
            // backing indices created later on
            assertThat(ismApi.getPolicy(policyId)).hasValueSatisfying(stored ->
                    assertThat(stored.policy().ismTemplate()).singleElement().satisfies(ismTemplate ->
                            // OpenSearch matches the pattern against the data stream name, not the .ds-* index name
                            assertThat(ismTemplate.indexPatterns()).containsExactly(stream)));

            final List<String> initialBackingIndices = backingIndices(stream);
            assertThat(initialBackingIndices).hasSize(1);

            // this one is managed because applyIsmPolicy() attaches the policy to it explicitly
            await().atMost(1, TimeUnit.MINUTES).pollInterval(1, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(managingPolicyOf(initialBackingIndices.get(0))).isEqualTo(policyId));

            final String rolledOverIndex = rollover(stream);
            assertThat(rolledOverIndex).isNotIn(initialBackingIndices);

            // nothing attaches the policy to the new index explicitly - only the ism_template can do that
            await().atMost(2, TimeUnit.MINUTES).pollInterval(2, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(managingPolicyOf(rolledOverIndex)).isEqualTo(policyId));
        } finally {
            dataStreamAdapter.deleteDataStream(stream);
            dataStreamAdapter.deleteIsmPolicy(policyId);
            dataStreamAdapter.deleteDataStreamTemplate(templateName);
        }
    }

    /**
     * @return the id of the ISM policy managing the index, or {@code null} if the index is not managed
     */
    @Nullable
    private String managingPolicyOf(String index) {
        return ismApi.explainIndex(index)
                .map(explain -> explain.path(index).path("index.plugins.index_state_management.policy_id"))
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .orElse(null);
    }

    private String rollover(String dataStream) {
        return openSearchInstance.getOfficialOpensearchClient().execute(
                () -> openSearchInstance.getOfficialOpensearchClient().sync().indices()
                        .rollover(r -> r.alias(dataStream)).newIndex(),
                "Unable to roll over data stream " + dataStream);
    }

    private List<String> backingIndices(String dataStream) {
        return dataStreamAdapter.getDataStream(dataStream).stream()
                .filter(ds -> ds.name().equals(dataStream))
                .findFirst()
                .map(ds -> ds.indices().stream().map(idx -> idx.indexName()).toList())
                .orElseThrow(() -> new IllegalStateException("Data stream " + dataStream + " not found"));
    }

}
