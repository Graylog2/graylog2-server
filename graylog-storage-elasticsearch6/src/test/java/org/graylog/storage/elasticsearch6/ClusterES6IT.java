package org.graylog.storage.elasticsearch6;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.joschi.jadconfig.util.Duration;
import io.searchbox.core.Cat;
import io.searchbox.core.CatResult;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.ClusterIT;
import org.graylog2.indexer.cluster.jest.JestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ClusterES6IT extends ClusterIT {
    @Override
    protected ClusterAdapter clusterAdapter(Duration timeout) {
        return new ClusterAdapterES6(jestClient(), timeout);
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
        final CatResult catResult = JestUtils.execute(jestClient(), nodesInfo, () -> "Unable to retrieve current node info");
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
