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
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
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
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexMapping5;
import org.junit.Before;
import org.junit.Rule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.google.common.base.Strings.nullToEmpty;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class can be used as base class for Elasticsearch integration tests.
 * <p>
 * It starts an Elasticsearch instance for every test method and provides several convenience methods for several
 * index management requests.
 * <p>
 * The class loads the Graylog default index template into Elasticsearch by default but that can be prevented by
 * using the {@link SkipDefaultIndexTemplate} annotation on a test method.
 * <p>
 * Check the {@link #importFixture(String)} method if you need to load fixture data from JSON files.
 */
public class ElasticsearchBaseTest {
    private static final Duration ES_TIMEOUT = Duration.seconds(5L);

    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstance.create();

    @Rule
    public final SkipDefaultIndexTemplateWatcher skipTemplatesWatcher = new SkipDefaultIndexTemplateWatcher();

    @Before
    public void before() throws Exception {
        if (!skipTemplatesWatcher.shouldSkip()) {
            // This is the default Graylog index template we are using when creating new indices
            addIndexTemplates(getMessageTemplates("graylog-test-internal"));
        }
    }

    /**
     * Returns the Elasticsearch client.
     * @return the client
     */
    protected JestClient client() {
        return elasticsearch.client();
    }

    /**
     * Import the given fixture resource path. The given path can be either a single file name or a full
     * resource path to a JSON fixture file. (e.g. "TheTest.json" or "org/graylog/test/TheTest.json")
     * If the resource path is a single filename, the method tries to find the resource in the resource path of
     * the test class.
     *
     * @param resourcePath the fixture resource path
     */
    protected void importFixture(String resourcePath) {
        final URL fixtureResource;

        if (Paths.get(resourcePath).getNameCount() > 1) {
            fixtureResource = Resources.getResource(resourcePath);
        } else {
            fixtureResource = Resources.getResource(getClass(), resourcePath);
        }

        elasticsearch.fixtureImporter().importResource(fixtureResource, client());

        // Make sure the data we just imported is visible
        refreshNode();
    }

    protected Version elasticsearchVersion() {
        return elasticsearch.version();
    }

    protected void refreshNode() {
        try {
            assertSucceeded(client().execute(new Refresh.Builder().build()));
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't refresh elasticsearch node", e);
        }
    }

    protected void createIndex(String index) throws IOException {
        createIndex(index, 1, 0);
    }

    protected void createIndex(String index, int shards, int replicas) throws IOException {
        createIndex(index, shards, replicas, Collections.emptyMap());
    }

    private void createIndex(String index, int shards, int replicas, Map<String, Object> indexSettings) throws IOException {
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
        final JestResult createIndexResponse = client().execute(createIndex);

        assertSucceeded(createIndexResponse);
    }

    protected String createRandomIndex(String prefix) throws IOException {
        final String indexName = prefix + System.nanoTime();

        createIndex(indexName);
        waitForGreenStatus(indexName);

        return indexName;
    }

    protected void deleteIndex(String... indices) throws IOException {
        for (String index : indices)
            if (indicesExists(index)) {
                final DeleteIndex deleteIndex = new DeleteIndex.Builder(index).build();
                final JestResult deleteIndexResponse = client().execute(deleteIndex);

                assertSucceeded(deleteIndexResponse);
            }
    }

    protected void closeIndex(String index) throws IOException {
        final CloseIndex closeIndex = new CloseIndex.Builder(index).build();
        final JestResult closeIndexResponse = client().execute(closeIndex);
        assertSucceeded(closeIndexResponse);
    }


    protected boolean indicesExists(String... indices) throws IOException {
        final IndicesExists indicesExists = new IndicesExists.Builder(Arrays.asList(indices)).build();
        final JestResult indicesExistsResponse = client().execute(indicesExists);

        return indicesExistsResponse.isSucceeded();
    }

    protected void addAliasMapping(String indexName, String alias) throws IOException {
        final AddAliasMapping addAliasMapping = new AddAliasMapping.Builder(indexName, alias).build();
        final ModifyAliases addAliasRequest = new ModifyAliases.Builder(addAliasMapping).build();
        final JestResult addAliasResponse = client().execute(addAliasRequest);
        assertSucceeded(addAliasResponse);
    }

    protected void removeAliasMapping(String indexName, String alias) throws IOException {
        final RemoveAliasMapping removeAliasMapping = new RemoveAliasMapping.Builder(indexName, alias).build();
        final ModifyAliases removeAliasRequest = new ModifyAliases.Builder(removeAliasMapping).build();
        final JestResult removeAliasResponse = client().execute(removeAliasRequest);
        assertSucceeded(removeAliasResponse);
    }

    /**
     * Returns the Graylog index mapping for the active Elasticsearch version.
     *
     * @return the index mapping
     */
    protected IndexMapping indexMapping() {
        switch (elasticsearch.version().getMajorVersion()) {
            case 5:
                return new IndexMapping5();
                // TODO: Needs ES 6 support
            default:
                throw new IllegalStateException("Only Elasticsearch 5.x is supported");
        }
    }

    protected Map<String, Map<String, Object>> getMessageTemplates(String templateName) {
        return Collections.singletonMap(templateName, indexMapping().messageTemplate("*", "standard", -1)
        );
    }

    protected void addIndexTemplates(Map<String, Map<String, Object>> templates) throws IOException {
        for (Map.Entry<String, Map<String, Object>> template : templates.entrySet()) {
            final String templateName = template.getKey();
            // TODO: Use putTemplate() here
            final PutTemplate putTemplate = new PutTemplate.Builder(templateName, template.getValue()).build();
            final JestResult result = client().execute(putTemplate);
            if (!result.isSucceeded()) {
                throw new IllegalStateException("Error while creating template \"" + templateName + "\": " + result.getErrorMessage());
            }
        }
    }

    protected JsonNode getMapping(String... indices) throws IOException {
        final GetMapping getMapping = new GetMapping.Builder().addIndex(Arrays.asList(indices)).build();
        final JestResult mappingResponse = client().execute(getMapping);
        assertSucceeded(mappingResponse);

        return mappingResponse.getJsonObject();
    }

    protected JsonNode getTemplate(String templateName) throws IOException {
        final GetTemplate templateRequest = new GetTemplate.Builder(templateName).build();
        final JestResult templateResponse = client().execute(templateRequest);
        assertSucceeded(templateResponse);

        return templateResponse.getJsonObject();
    }

    protected JsonNode getTemplates() throws IOException {
        // An empty template name returns all existing templates
        return getTemplate("");
    }

    protected void putTemplate(String templateName, Object source) throws IOException {
        final PutTemplate templateRequest = new PutTemplate.Builder(templateName, source).build();
        final JestResult templateResponse = client().execute(templateRequest);
        assertSucceeded(templateResponse);
    }

    protected void deleteTemplate(String templateName) throws IOException {
        final DeleteTemplate templateRequest = new DeleteTemplate.Builder(templateName).build();
        final JestResult templateResponse = client().execute(templateRequest);
        assertSucceeded(templateResponse);
    }

    protected void waitForGreenStatus(String... indices) throws IOException {
        waitForStatus(Health.Status.GREEN, indices);
    }

    private Health.Status waitForStatus(Health.Status status, String... indices) throws IOException {
        final Health health = new Health.Builder()
                .addIndex(Arrays.asList(indices))
                .waitForStatus(status)
                .timeout((int) ES_TIMEOUT.toSeconds())
                .build();

        final JestResult clusterHealthResponse = client().execute(health);
        assertSucceeded(clusterHealthResponse);

        final String actualStatus = clusterHealthResponse.getJsonObject().get("status").asText();
        assertThat(actualStatus)
                .isNotBlank()
                .isEqualTo(status.getKey());

        return Health.Status.valueOf(actualStatus.toUpperCase(Locale.ROOT));
    }

    protected void assertSucceeded(JestResult jestResult) {
        final String errorMessage = nullToEmpty(jestResult.getErrorMessage());
        assertThat(jestResult.isSucceeded())
                .overridingErrorMessage(errorMessage)
                .isTrue();
    }
}
