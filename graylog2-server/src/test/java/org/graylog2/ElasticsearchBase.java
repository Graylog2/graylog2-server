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
package org.graylog2;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.nosqlunit.elasticsearch.http.ElasticsearchConfiguration;
import com.github.joschi.nosqlunit.elasticsearch.http.ElasticsearchRule;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;
import io.searchbox.core.Ping;
import io.searchbox.indices.CloseIndex;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.aliases.AddAliasMapping;
import io.searchbox.indices.aliases.ModifyAliases;
import io.searchbox.indices.aliases.RemoveAliasMapping;
import io.searchbox.indices.mapping.GetMapping;
import io.searchbox.indices.template.DeleteTemplate;
import io.searchbox.indices.template.GetTemplate;
import io.searchbox.indices.template.PutTemplate;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexMapping5;
import org.junit.Rule;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Strings.nullToEmpty;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class ElasticsearchBase {
    private static final String PROPERTIES_RESOURCE_NAME = "elasticsearch.properties";
    private static final String DEFAULT_PORT = "9200";

    public static final Duration ES_TIMEOUT = Duration.seconds(5L);

    @Rule
    public final ElasticsearchRule elasticsearchRule;

    @Inject
    private JestClient jestClient;

    private final Properties properties;
    private final Version elasticsearchVersion;

    public ElasticsearchBase() {
        this.properties = loadProperties(PROPERTIES_RESOURCE_NAME);
        this.elasticsearchVersion = Version.valueOf(properties.getProperty("version"));
        this.elasticsearchRule = elasticsearchRule().build();
    }

    private Properties loadProperties(String resourceName) {
        final Properties properties = new Properties();
        final URL resource = Resources.getResource(resourceName);
        try (InputStream stream = resource.openStream()) {
            properties.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Error while reading test properties", e);
        }
        return properties;
    }

    protected String getHttpPort() {
        return properties.getProperty("httpPort", DEFAULT_PORT);
    }

    protected String getServer() {
        return "http://localhost:" + getHttpPort();
    }

    protected ElasticsearchConfiguration.Builder elasticsearchConfiguration() {
        return ElasticsearchConfiguration.remoteElasticsearch(getServer());
    }

    protected ElasticsearchRule.Builder elasticsearchRule() {
        final ElasticsearchConfiguration configuration = elasticsearchConfiguration().build();
        return ElasticsearchRule.newElasticsearchRule().configure(configuration);
    }

    protected JestClient client() {
        return jestClient;
    }

    protected void assertSucceeded(JestResult jestResult) {
        final String errorMessage = nullToEmpty(jestResult.getErrorMessage());
        assertThat(jestResult.isSucceeded())
                .overridingErrorMessage(errorMessage)
                .isTrue();
    }

    protected IndexMapping indexMapping() {
        switch (elasticsearchVersion.getMajorVersion()) {
            case 5:
                return new IndexMapping5();
            default:
                throw new IllegalStateException("Only Elasticsearch 5.x is supported");
        }
    }

    public void createIndex(String index) throws IOException {
        createIndex(index, 1, 0);
    }

    public void createIndex(String index, int shards, int replicas) throws IOException {
        createIndex(index, shards, replicas, Collections.emptyMap());
    }

    public void createIndex(String index, int shards, int replicas, Map<String, Object> indexSettings) throws IOException {
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

    public void deleteIndex(String... indices) throws IOException {
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

    public boolean indicesExists(String... indices) throws IOException {
        final IndicesExists indicesExists = new IndicesExists.Builder(Arrays.asList(indices)).build();
        final JestResult indicesExistsResponse = client().execute(indicesExists);

        return indicesExistsResponse.isSucceeded();
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

    protected JsonNode getTemplate(String templateName) throws IOException {
        final GetTemplate templateRequest = new GetTemplate.Builder(templateName).build();
        final JestResult templateResponse = client().execute(templateRequest);
        assertSucceeded(templateResponse);

        return templateResponse.getJsonObject();
    }

    protected JsonNode getTemplates() throws IOException {
        final GetTemplate templateRequest = new GetTemplate.Builder("").build();
        final JestResult templateResponse = client().execute(templateRequest);
        assertSucceeded(templateResponse);

        return templateResponse.getJsonObject();
    }

    protected JsonNode getMapping(String... indices) throws IOException {
        final GetMapping getMapping = new GetMapping.Builder().addIndex(Arrays.asList(indices)).build();
        final JestResult mappingResponse = client().execute(getMapping);
        assertSucceeded(mappingResponse);

        return mappingResponse.getJsonObject();
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


    public void waitForGreenStatus(String... indices) throws IOException {
        waitForStatus(Health.Status.GREEN, indices);
    }

    public Health.Status waitForStatus(Health.Status status, String... indices) throws IOException {
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

    public Version remoteElasticsearchVersion() throws IOException {
        final Ping ping = new Ping.Builder().build();
        final JestResult pingResponse = client().execute(ping);
        assertSucceeded(pingResponse);

        return Version.valueOf(pingResponse.getJsonObject().path("version").path("number").asText());
    }

    public Version getElasticsearchVersion() throws IOException {
        return elasticsearchVersion;
    }

}
