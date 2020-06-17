package org.graylog.storage.elasticsearch6.migrations;

import com.fasterxml.jackson.databind.JsonNode;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.State;
import org.graylog2.indexer.cluster.jest.JestUtils;
import org.graylog2.migrations.V20170607164210_MigrateReopenedIndicesToAliases;

import javax.inject.Inject;
import java.util.Collection;

public class V20170607164210_MigrateReopenedIndicesToAliasesClusterStateES6 implements V20170607164210_MigrateReopenedIndicesToAliases.ClusterState {
    private final JestClient jestClient;

    @Inject
    public V20170607164210_MigrateReopenedIndicesToAliasesClusterStateES6(JestClient jestClient) {
        this.jestClient = jestClient;
    }

    @Override
    public JsonNode getForIndices(Collection<String> indices) {
        final String indexList = String.join(",", indices);

        final State request = new State.Builder().withMetadata().indices(indexList).build();

        final JestResult jestResult = JestUtils.execute(jestClient, request, () -> "Couldn't read cluster state for reopened indices " + indices);

        return jestResult.getJsonObject();
    }
}
