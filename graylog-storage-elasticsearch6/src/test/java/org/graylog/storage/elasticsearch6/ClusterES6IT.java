package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.joschi.jadconfig.util.Duration;
import io.searchbox.core.Cat;
import io.searchbox.core.CatResult;
import org.graylog.storage.elasticsearch6.testing.ElasticsearchInstanceES6;
import org.graylog.testing.elasticsearch.ElasticsearchInstance;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.ClusterIT;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.junit.Rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.storage.elasticsearch6.testing.TestUtils.jestClient;

public class ClusterES6IT extends ClusterIT {
    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstanceES6.create();

    @Override
    protected ClusterAdapter clusterAdapter(Duration timeout) {
        return new ClusterAdapterES6(jestClient(elasticsearch), timeout);
    }

    @Override
    protected ElasticsearchInstance elasticsearch() {
        return this.elasticsearch;
    }

    @Override
    protected String currentNodeId() {
        final JsonNode node = currentNodeInfo();
        return node.get("id").asText();
    }

    private JsonNode currentNodeInfo() {
        final Cat nodesInfo = new Cat.NodesBuilder()
                .setParameter("h", "id,name,host,ip")
                .setParameter("format", "json")
                .setParameter("full_id", "true")
                .build();
        final CatResult catResult = JestUtils.execute(jestClient(elasticsearch), nodesInfo, () -> "Unable to retrieve current node info");
        final JsonNode result = catResult.getJsonObject().path("result");
        assertThat(result).isNotEmpty();

        return result.path(0);
    }

    @Override
    protected String currentNodeName() {
        final JsonNode node = currentNodeInfo();
        return node.get("name").asText();
    }

    @Override
    protected String currentHostnameOrIp() {
        final JsonNode node = currentNodeInfo();
        final String ip = node.path("ip").asText();
        return node.path("host").asText(ip);
    }
}
