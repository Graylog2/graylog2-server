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
import org.graylog.shaded.opensearch2.org.opensearch.client.indices.DataStream;
import org.graylog.storage.opensearch2.ism.IsmApi;
import org.graylog.storage.opensearch2.ism.policy.IsmPolicy;
import org.graylog.storage.opensearch2.ism.policy.IsmPolicyTest;
import org.graylog.storage.opensearch2.testing.OpenSearchInstance;
import org.graylog2.indexer.indices.Template;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DataStreamAdapterOS2IT {

    @Rule
    public final OpenSearchInstance openSearchInstance = OpenSearchInstance.create();

    ObjectMapper objectMapper = new ObjectMapperProvider().get();

    DataStreamAdapterOS2 dataStreamAdapter = new DataStreamAdapterOS2(openSearchInstance.openSearchClient(),
            objectMapper, new IsmApi(openSearchInstance.openSearchClient(), objectMapper));


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


}
