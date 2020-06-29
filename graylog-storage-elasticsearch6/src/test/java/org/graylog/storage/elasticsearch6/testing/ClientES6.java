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
package org.graylog.storage.elasticsearch6.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.cluster.State;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Index;
import io.searchbox.indices.CloseIndex;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.Refresh;
import io.searchbox.indices.aliases.AddAliasMapping;
import io.searchbox.indices.aliases.ModifyAliases;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.template.DeleteTemplate;
import io.searchbox.indices.template.GetTemplate;
import io.searchbox.indices.template.PutTemplate;
import org.graylog.testing.elasticsearch.BulkIndexRequest;
import org.graylog.testing.elasticsearch.Client;
import org.graylog.storage.elasticsearch6.jest.JestUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Iterators.toArray;
import static org.assertj.core.api.Assertions.assertThat;

public class  ClientES6 implements Client {
    private static final Duration ES_TIMEOUT = Duration.seconds(5L);
    private final JestClient client;

    public ClientES6(JestClient client) {
        this.client = client;
    }

    @Override
    public void createIndex(String index) {
        createIndex(index, 1, 0);
    }

    @Override
    public void createIndex(String index, int shards, int replicas) {
        createIndex(index, shards, replicas, Collections.emptyMap());
    }

    private void createIndex(String index, int shards, int replicas, Map<String, Object> indexSettings) {
        final Map<String, Object> settings = new HashMap<>();
        settings.put("number_of_shards", shards);
        settings.put("number_of_replicas", replicas);

        if (indexSettings.containsKey("settings")) {
            @SuppressWarnings("unchecked") final Map<String, Object> customSettings = (Map<String, Object>) indexSettings.get("settings");
            settings.putAll(customSettings);
        }

        final Map<String, Object> mappings = new HashMap<>();
        if (indexSettings.containsKey("mappings")) {
            @SuppressWarnings("unchecked") final Map<String, Object> customMappings = (Map<String, Object>) indexSettings.get("mappings");
            mappings.putAll(customMappings);
        }

        final ImmutableMap<String, Map<String, Object>> finalSettings = ImmutableMap.of(
                "settings", settings,
                "mappings", mappings);
        final CreateIndex createIndex = new CreateIndex.Builder(index)
                .settings(finalSettings)
                .build();

        executeWithExpectedSuccess(createIndex, "failed to create index " + index);
    }

    @Override
    public String createRandomIndex(String prefix) {
        final String indexName = prefix + System.nanoTime();

        createIndex(indexName);
        waitForGreenStatus(indexName);

        return indexName;
    }

    @Override
    public void deleteIndices(String... indices) {
        for (String index : indices)
            if (indicesExists(index)) {
                final DeleteIndex deleteIndex = new DeleteIndex.Builder(index).build();
                executeWithExpectedSuccess(deleteIndex, "failed to delete index " + index);
            }
    }

    @Override
    public void closeIndex(String index) {
        final CloseIndex closeIndex = new CloseIndex.Builder(index).build();
        executeWithExpectedSuccess(closeIndex, "failed to close index " + index);
    }

    @Override
    public boolean indicesExists(String... indices) {
        final IndicesExists indicesExists = new IndicesExists.Builder(Arrays.asList(indices)).build();
        final JestResult indicesExistsResponse =
                execute(indicesExists, "failed to check for existence of indices: " + Arrays.toString(indices));

        return indicesExistsResponse.isSucceeded();
    }

    @Override
    public void addAliasMapping(String indexName, String alias) {
        final AddAliasMapping addAliasMapping = new AddAliasMapping.Builder(indexName, alias).build();
        final ModifyAliases addAliasRequest = new ModifyAliases.Builder(addAliasMapping).build();

        executeWithExpectedSuccess(addAliasRequest, "failed to add alias " + alias + " for index " + indexName);
    }

    @Override
    public JsonNode getMapping(String... indices) {
        final GetMapping getMapping = new GetMapping.Builder().addIndex(Arrays.asList(indices)).build();

        final JestResult response = executeWithExpectedSuccess(getMapping, "");

        return response.getJsonObject();
    }

    @Override
    public JsonNode getTemplate(String templateName) {
        final GetTemplate templateRequest = new GetTemplate.Builder(templateName).build();
        final JestResult templateResponse =
                executeWithExpectedSuccess(templateRequest, "failed to get template " + templateName);

        return templateResponse.getJsonObject();
    }

    @Override
    public JsonNode getTemplates() {
        return getTemplate("");
    }

    @Override
    public void putTemplate(String templateName, Object source) {
        final PutTemplate templateRequest = new PutTemplate.Builder(templateName, source).build();
        executeWithExpectedSuccess(templateRequest, "failed to put template " + templateName);
    }

    @Override
    public void deleteTemplates(String... templates) {
        for (String template : templates) {
            final DeleteTemplate templateRequest = new DeleteTemplate.Builder(template).build();
            executeWithExpectedSuccess(templateRequest, "failed to delete template " + template);
        }
    }

    @Override
    public void waitForGreenStatus(String... indices) {
        waitForStatus(Health.Status.GREEN, indices);
    }

    private void waitForStatus(Health.Status status, String... indices) {
        final Health health = new Health.Builder()
                .addIndex(Arrays.asList(indices))
                .waitForStatus(status)
                .timeout((int) ES_TIMEOUT.toSeconds())
                .build();

        final JestResult clusterHealthResponse =
                executeWithExpectedSuccess(health, "failed to get cluster health");

        final String actualStatus = clusterHealthResponse.getJsonObject().get("status").asText();
        assertThat(actualStatus)
                .isNotBlank()
                .isEqualTo(status.getKey());

        Health.Status.valueOf(actualStatus.toUpperCase(Locale.ROOT));
    }

    private <T extends JestResult> T executeWithExpectedSuccess(Action<T> jestAction, String errorMessage) {
        final T response = execute(jestAction, errorMessage);
        assertSucceeded(response);
        return response;
    }

    private <T extends JestResult> T execute(Action<T> jestAction, String errorMessage) {
        try {
            return client.execute(jestAction);
        } catch (IOException e) {
            throw new UncheckedIOException(errorMessage, e);
        }
    }

    private void assertSucceeded(JestResult jestResult) {
        final String errorMessage = nullToEmpty(jestResult.getErrorMessage());
        assertThat(jestResult.isSucceeded())
                .overridingErrorMessage(errorMessage)
                .isTrue();
    }

    @Override
    public void refreshNode() {
        executeWithExpectedSuccess(new Refresh.Builder().build(), "Couldn't refresh elasticsearch node");
    }

    @Override
    public void bulkIndex(BulkIndexRequest bulkIndexRequest) {
        final Bulk.Builder bulkBuilder = new Bulk.Builder().refresh(true);

        bulkIndexRequest.requests()
                .entrySet()
                .stream()
                .flatMap(this::createBulkIndexActions)
                .forEach(bulkBuilder::addAction);

        final BulkResult bulkResult = executeWithExpectedSuccess(bulkBuilder.build(), "failed to execute bulk request");

        assertThat(bulkResult.getFailedItems()).isEmpty();

    }

    private Stream<Index> createBulkIndexActions(Map.Entry<String, List<Map<String, Object>>> entry) {
        final String indexName = entry.getKey();

        return entry.getValue().stream()
                .map(source -> this.createBulkIndexAction(indexName, source));
    }

    private Index createBulkIndexAction(String indexName, Map<String, Object> source) {
        return new Index.Builder(source)
                .index(indexName)
                .type("test")
                .refresh(true)
                .build();
    }

    @Override
    public void cleanUp() {
        final State request = new State.Builder().withMetadata().build();
        final JsonNode result = JestUtils.execute(client, request, () -> "failed to read state").getJsonObject();

        deleteTemplates(metadataFieldNamesFor(result, "templates"));
        deleteIndices(metadataFieldNamesFor(result, "indices"));
    }

    private String[] metadataFieldNamesFor(JsonNode result, String templates) {
        return toArray(result.get("metadata").get(templates).fieldNames(), String.class);
    }
}
