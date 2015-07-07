/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
