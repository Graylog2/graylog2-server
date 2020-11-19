/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.testing.elasticsearch;

import com.github.zafarkhaja.semver.Version;
import org.junit.Before;
import org.junit.Rule;

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
public abstract class ElasticsearchBaseTest {
    @Rule
    public final SkipDefaultIndexTemplateWatcher skipTemplatesWatcher = new SkipDefaultIndexTemplateWatcher();

    @Before
    public void before() {
        if (!skipTemplatesWatcher.shouldSkip()) {
            addGraylogDefaultIndexTemplate();
        }
    }

    private void addGraylogDefaultIndexTemplate() {
        addIndexTemplates(getGraylogDefaultMessageTemplates(elasticsearch().version()));
    }

    private static Map<String, Map<String, Object>> getGraylogDefaultMessageTemplates(Version version) {
        final Map<String, Object> template =
                indexMappingFor(version).messageTemplate("*", "standard", -1);
        return Collections.singletonMap("graylog-test-internal", template);
    }

    private void addIndexTemplates(Map<String, Map<String, Object>> templates) {
        for (Map.Entry<String, Map<String, Object>> template : templates.entrySet()) {
            final String templateName = template.getKey();

            elasticsearch().client().putTemplate(templateName, template.getValue());
        }
    }

    protected abstract ElasticsearchInstance elasticsearch();

    /**
     * Returns a custom Elasticsearch client with a bunch of utility methods.
     *
     * @return the client
     */
    protected Client client() {
        return elasticsearch().client();
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
        elasticsearch().importFixtureResource(resourcePath, getClass());
    }

    protected Version elasticsearchVersion() {
        return elasticsearch().version();
    }
}
