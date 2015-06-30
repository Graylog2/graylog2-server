package org.graylog2.indexer.nosqlunit;

import com.google.common.collect.ImmutableSet;
import com.lordofthejars.nosqlunit.core.DatabaseOperation;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.indices.Indices;

import java.io.InputStream;
import java.util.Set;

public class IndexCreatingDatabaseOperation implements DatabaseOperation<Client> {
    private final DatabaseOperation<Client> databaseOperation;
    private final Client client;
    private final Set<String> indexes;

    public IndexCreatingDatabaseOperation(DatabaseOperation<Client> databaseOperation, Set<String> indexes) {
        this.databaseOperation = databaseOperation;
        this.client = databaseOperation.connectionManager();
        this.indexes = ImmutableSet.copyOf(indexes);
    }

    @Override
    public void insert(InputStream dataScript) {
        final IndicesAdminClient indicesAdminClient = client.admin().indices();
        for (String index : indexes) {
            IndicesExistsResponse indicesExistsResponse = indicesAdminClient.prepareExists(index)
                    .execute()
                    .actionGet();

            if (indicesExistsResponse.isExists()) {
                client.admin().indices().prepareDelete(index).execute().actionGet();
            }

            Indices indices = new Indices(client, new ElasticsearchConfiguration(), new IndexMapping(client));
            if (!indices.create(index)) {
                throw new IllegalStateException("Couldn't create index " + index);
            }
        }

        databaseOperation.insert(dataScript);
    }

    @Override
    public void deleteAll() {
        databaseOperation.deleteAll();
    }

    @Override
    public boolean databaseIs(InputStream expectedData) {
        return databaseOperation.databaseIs(expectedData);
    }

    @Override
    public Client connectionManager() {
        return databaseOperation.connectionManager();
    }
}
