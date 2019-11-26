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
package org.graylog.testing.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.collect.ImmutableMap;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.indices.CloseIndex;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.Refresh;
import io.searchbox.indices.aliases.AddAliasMapping;
import io.searchbox.indices.aliases.ModifyAliases;
import io.searchbox.indices.aliases.RemoveAliasMapping;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.template.DeleteTemplate;
import io.searchbox.indices.template.GetTemplate;
import io.searchbox.indices.template.PutTemplate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Strings.nullToEmpty;
import static org.assertj.core.api.Assertions.assertThat;

public class Client {
    private static final Duration ES_TIMEOUT = Duration.seconds(5L);
    private final JestClient client;

    public Client(JestClient client) {
        this.client = client;
    }

    public void createIndex(String index) {
        createIndex(index, 1, 0);
    }

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

    public String createRandomIndex(String prefix) {
        final String indexName = prefix + System.nanoTime();

        createIndex(indexName);
        waitForGreenStatus(indexName);

        return indexName;
    }

    public void deleteIndices(String... indices) {
        for (String index : indices)
            if (indicesExists(index)) {
                final DeleteIndex deleteIndex = new DeleteIndex.Builder(index).build();
                executeWithExpectedSuccess(deleteIndex, "failed to delete index " + index);
            }
    }

    public void closeIndex(String index) {
        final CloseIndex closeIndex = new CloseIndex.Builder(index).build();
        executeWithExpectedSuccess(closeIndex, "failed to close index " + index);
    }

    public boolean indicesExists(String... indices) {
        final IndicesExists indicesExists = new IndicesExists.Builder(Arrays.asList(indices)).build();
        final JestResult indicesExistsResponse =
                execute(indicesExists, "failed to check for existence of indices: " + Arrays.toString(indices));

        return indicesExistsResponse.isSucceeded();
    }

    public void addAliasMapping(String indexName, String alias) {
        final AddAliasMapping addAliasMapping = new AddAliasMapping.Builder(indexName, alias).build();
        final ModifyAliases addAliasRequest = new ModifyAliases.Builder(addAliasMapping).build();

        executeWithExpectedSuccess(addAliasRequest, "failed to add alias " + alias + " for index " + indexName);
    }

    public void removeAliasMapping(String indexName, String alias) {
        final RemoveAliasMapping removeAliasMapping = new RemoveAliasMapping.Builder(indexName, alias).build();
        final ModifyAliases removeAliasRequest = new ModifyAliases.Builder(removeAliasMapping).build();

        executeWithExpectedSuccess(removeAliasRequest, "failed to remove alias " + alias + " for index " + indexName);
    }

    public JsonNode getMapping(String... indices) {
        final GetMapping getMapping = new GetMapping.Builder().addIndex(Arrays.asList(indices)).build();

        final JestResult response = executeWithExpectedSuccess(getMapping, "");

        return response.getJsonObject();
    }

    public JsonNode getTemplate(String templateName) {
        final GetTemplate templateRequest = new GetTemplate.Builder(templateName).build();
        final JestResult templateResponse =
                executeWithExpectedSuccess(templateRequest, "failed to get template " + templateName);

        return templateResponse.getJsonObject();
    }

    public JsonNode getTemplates() {
        return getTemplate("");
    }

    public void putTemplate(String templateName, Object source) {
        final PutTemplate templateRequest = new PutTemplate.Builder(templateName, source).build();
        executeWithExpectedSuccess(templateRequest, "failed to put template " + templateName);
    }

    public void deleteTemplates(String... templates) {
        for (String template : templates) {
            final DeleteTemplate templateRequest = new DeleteTemplate.Builder(template).build();
            executeWithExpectedSuccess(templateRequest, "failed to delete template " + template);
        }
    }

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

    public <T extends JestResult> T executeWithExpectedSuccess(Action<T> jestAction, String errorMessage) {
        final T response = execute(jestAction, errorMessage);
        assertSucceeded(response);
        return response;
    }

    public <T extends JestResult> T execute(Action<T> jestAction, String errorMessage) {
        try {
            return client.execute(jestAction);
        } catch (IOException e) {
            throw new UncheckedIOException(errorMessage, e);
        }
    }

    public void assertSucceeded(JestResult jestResult) {
        final String errorMessage = nullToEmpty(jestResult.getErrorMessage());
        assertThat(jestResult.isSucceeded())
                .overridingErrorMessage(errorMessage)
                .isTrue();
    }

    public void refreshNode() {
        executeWithExpectedSuccess(new Refresh.Builder().build(), "Couldn't refresh elasticsearch node");
    }
}
