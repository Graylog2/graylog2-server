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

import com.github.zafarkhaja.semver.Version;
import com.google.common.io.Resources;
import io.searchbox.client.JestClient;
import org.junit.Before;
import org.junit.Rule;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.graylog2.indexer.IndexMappingFactory.indexMappingFor;

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

    @Rule
    public final ElasticsearchInstance elasticsearch = ElasticsearchInstance.create();

    @Rule
    public final SkipDefaultIndexTemplateWatcher skipTemplatesWatcher = new SkipDefaultIndexTemplateWatcher();

    @Before
    public void before() {
        if (!skipTemplatesWatcher.shouldSkip()) {
            addGraylogDefaultIndexTemplate();
        }
    }

    private void addGraylogDefaultIndexTemplate() {
        addIndexTemplates(getGraylogDefaultMessageTemplates(elasticsearch.version()));
    }

    private static Map<String, Map<String, Object>> getGraylogDefaultMessageTemplates(Version version) {
        final Map<String, Object> template =
                indexMappingFor(version).messageTemplate("*", "standard", -1);
        return Collections.singletonMap("graylog-test-internal", template);
    }

    private void addIndexTemplates(Map<String, Map<String, Object>> templates) {
        for (Map.Entry<String, Map<String, Object>> template : templates.entrySet()) {
            final String templateName = template.getKey();

            elasticsearch.client().putTemplate(templateName, template.getValue());
        }
    }

    /**
     * Returns the Elasticsearch client.
     * @return the client
     */
    protected JestClient jestClient() {
        return elasticsearch.jestClient();
    }

    /**
     * Returns a custom Elasticsearch client with a bunch of utility methods.
     *
     * @return the client
     */
    protected Client client() {
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
    @SuppressWarnings("UnstableApiUsage")
    protected void importFixture(String resourcePath) {
        final URL fixtureResource;

        if (Paths.get(resourcePath).getNameCount() > 1) {
            fixtureResource = Resources.getResource(resourcePath);
        } else {
            fixtureResource = Resources.getResource(getClass(), resourcePath);
        }

        elasticsearch.fixtureImporter().importResource(fixtureResource, jestClient());

        // Make sure the data we just imported is visible
        client().refreshNode();
    }

    protected Version elasticsearchVersion() {
        return elasticsearch.version();
    }
}
