package org.graylog.storage.elasticsearch7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.joschi.jadconfig.util.Duration;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.RestHighLevelClient;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.client.core.MainResponse;
import org.graylog.storage.elasticsearch7.cat.CatApi;
import org.graylog.storage.elasticsearch7.cat.NodeResponse;
import org.graylog.storage.elasticsearch7.testing.ElasticsearchInstanceES7;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.ClusterIT;
import org.graylog2.indexer.cluster.health.ClusterAllocationDiskSettings;
import org.graylog2.indexer.cluster.health.WatermarkSettings;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterES7IT extends ClusterIT {
    @Rule
    public final ElasticsearchInstanceES7 elasticsearch = ElasticsearchInstanceES7.create();

    @Test
    public void getClusterAllocationDiskSettings() {
        final ClusterAllocationDiskSettings clusterAllocationDiskSettings = cluster.getClusterAllocationDiskSettings();

        //Default Elasticsearch settings in Elasticsearch 5.6
        assertThat(clusterAllocationDiskSettings.ThresholdEnabled()).isTrue();
        assertThat(clusterAllocationDiskSettings.watermarkSettings().type()).isEqualTo(WatermarkSettings.SettingsType.PERCENTAGE);
        assertThat(clusterAllocationDiskSettings.watermarkSettings().low()).isEqualTo(85D);
        assertThat(clusterAllocationDiskSettings.watermarkSettings().high()).isEqualTo(90D);
        assertThat(clusterAllocationDiskSettings.watermarkSettings().floodStage()).isEqualTo(95D);
    }

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected ClusterAdapter clusterAdapter(Duration timeout) {
        final ObjectMapper objectMapper = new ObjectMapperProvider().get();
        return new ClusterAdapterES7(elasticsearch.elasticsearchClient(),
                timeout,
                new CatApi(objectMapper),
                new PlainJsonApi(objectMapper));
    }

    @Override
    protected String currentNodeId() {
        return currentNode().id();
    }

    private NodeResponse currentNode() {
        final List<NodeResponse> nodes = elasticsearch.elasticsearchClient().execute(catApi()::nodes);
        return nodes.get(0);
    }

    @Override
    protected String currentNodeName() {
        return currentNode().name();
    }

    @Override
    protected String currentHostnameOrIp() {
        final NodeResponse currentNode = currentNode();
        return Optional.ofNullable(currentNode.host()).orElse(currentNode.ip());
    }

    private CatApi catApi() {
        return new CatApi(new ObjectMapperProvider().get());
    }

    private MainResponse info() {
        return elasticsearch.elasticsearchClient().execute(RestHighLevelClient::info);
    }
}
