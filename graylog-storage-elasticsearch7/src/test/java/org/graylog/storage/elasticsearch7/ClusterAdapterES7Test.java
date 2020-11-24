package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.io.Resources;
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClusterAdapterES7Test {
    private static final String nodeId = "I-sZn-HKQhCtdf1JYPcx1A";

    private ElasticsearchClient client;
    private CatApi catApi;
    private PlainJsonApi jsonApi;
    private ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private ClusterAdapterES7 clusterAdapter;

    @BeforeEach
    void setUp() {
        this.client = mock(ElasticsearchClient.class);
        this.catApi = mock(CatApi.class);
        this.jsonApi = mock(PlainJsonApi.class);

        this.clusterAdapter = new ClusterAdapterES7(client, Duration.seconds(1), catApi, jsonApi);
    }

    @Test
    void handlesMissingHostField() throws Exception {
        mockNodesResponse();

        assertThat(this.clusterAdapter.nodeIdToHostName(nodeId)).isEmpty();
    }

    @Test
    void returnsNameForNodeId() throws Exception {
        mockNodesResponse();

        assertThat(this.clusterAdapter.nodeIdToName(nodeId)).isNotEmpty()
                .contains("es02");
    }

    @Test
    void returnsEmptyOptionalForMissingNodeId() throws Exception {
        mockNodesResponse();

        assertThat(this.clusterAdapter.nodeIdToName("foobar")).isEmpty();
    }

    private void mockNodesResponse() throws IOException {
        when(jsonApi.perform(any(), anyString()))
                .thenReturn(objectMapper.readTree(Resources.getResource("nodes-response-without-host-field.json")));
    }
}
