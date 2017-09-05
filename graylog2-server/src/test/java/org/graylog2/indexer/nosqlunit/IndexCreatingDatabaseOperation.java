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

import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.lordofthejars.nosqlunit.core.DatabaseOperation;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.core.Ping;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.template.PutTemplate;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexMapping2;
import org.graylog2.indexer.IndexMapping5;
import org.graylog2.indexer.IndexSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class IndexCreatingDatabaseOperation implements DatabaseOperation<JestClient> {
    private final DatabaseOperation<JestClient> databaseOperation;
    private final IndexSet indexSet;
    private final JestClient client;
    private final Set<String> indexes;

    public IndexCreatingDatabaseOperation(DatabaseOperation<JestClient> databaseOperation, IndexSet indexSet, Set<String> indexes) {
        this.databaseOperation = databaseOperation;
        this.indexSet = indexSet;
        this.client = databaseOperation.connectionManager();
        this.indexes = ImmutableSet.copyOf(indexes);
    }

    @Override
    public void insert(InputStream dataScript) {
        waitForGreenStatus();

        final Ping ping = new Ping.Builder().build();
        final JestResult pingResult = execute(ping, s -> "Failed to get Elasticsearch version: " + s);
        final String elasticsearchVersionString = pingResult.getJsonObject().path("version").path("number").asText();
        final Version elasticsearchVersion = Version.valueOf(elasticsearchVersionString);

        final IndexMapping indexMapping;
        switch (elasticsearchVersion.getMajorVersion()) {
            case 2:
                indexMapping = new IndexMapping2();
                break;
            case 5:
                indexMapping = new IndexMapping5();
                break;
            default:
                throw new IllegalStateException("Only Elasticsearch 2.x and 5.x are supported");
        }

        final String templateName = "graylog-test-internal";

        final Map<String, Object> template = indexMapping.messageTemplate("*", "standard");
        final PutTemplate putTemplate = new PutTemplate.Builder(templateName, template).build();
        execute(putTemplate, s -> "Couldn't create index template: " + s);

        for (String index : indexes) {
            final IndicesExists indicesExists = new IndicesExists.Builder(index).build();
            final JestResult indicesExistsResponse;
            try {
                indicesExistsResponse = client.execute(indicesExists);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            if (indicesExistsResponse.isSucceeded()) {
                final DeleteIndex deleteIndex = new DeleteIndex.Builder(index).build();
                execute(deleteIndex, s -> "Couldn't delete index: " + s);
            }

            final Map<String, Map<String, Object>> indexSettings = ImmutableMap.of("settings", ImmutableMap.of(
                    "number_of_shards", indexSet.getConfig().shards(),
                    "number_of_replicas", indexSet.getConfig().replicas()
            ));
            final CreateIndex createIndex = new CreateIndex.Builder(index).settings(indexSettings).build();
            execute(createIndex, s -> "Couldn't create index " + s);
        }

        databaseOperation.insert(dataScript);
    }

    @Override
    public void deleteAll() {
        waitForGreenStatus();
        databaseOperation.deleteAll();
    }

    private void waitForGreenStatus() {
        final Health request = new Health.Builder().timeout(15).waitForStatus(Health.Status.YELLOW).build();
        execute(request, s -> "Couldn't check cluster health of Elasticsearch: " + s);
    }

    private <T extends JestResult> T execute(Action<T> request, Function<String, String> errorMessage) {
        try {
            final T result = client.execute(request);

            if (!result.isSucceeded()) {
                throw new IllegalStateException(errorMessage.apply(result.getErrorMessage()));
            }

            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(errorMessage.apply(e.getMessage()), e);
        }
    }

    @Override
    public boolean databaseIs(InputStream expectedData) {
        return databaseOperation.databaseIs(expectedData);
    }

    @Override
    public JestClient connectionManager() {
        return databaseOperation.connectionManager();
    }
}
