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

import org.graylog2.indexer.MessageIndexTemplateProvider;
import org.graylog2.indexer.indices.Template;
import org.graylog2.storage.SearchVersion;
import org.junit.Before;
import org.junit.Rule;

import java.util.Collections;
import java.util.Map;

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

    public String messageTemplateIndexPattern() {
        // TODO Check whether we can use graylog_* here.
        //      This also matches composable ism templates and generates warnings
        return "*";
    }

    private void addGraylogDefaultIndexTemplate() {
        addIndexTemplates(getGraylogDefaultMessageTemplates(searchServer().version()));
    }

    private Map<String, Template> getGraylogDefaultMessageTemplates(SearchVersion version) {
        var template = new MessageIndexTemplateProvider().create(version, null)
                .messageTemplate(messageTemplateIndexPattern(), "standard", 100L, null);
        return Collections.singletonMap("graylog-test-internal", template);
    }

    private void addIndexTemplates(Map<String, Template> templates) {
        for (var template : templates.entrySet()) {
            final String templateName = template.getKey();

            searchServer().client().putTemplate(templateName, template.getValue());
        }
    }

    protected abstract SearchServerInstance searchServer();

    /**
     * Returns a custom Elasticsearch client with a bunch of utility methods.
     *
     * @return the client
     */
    protected Client client() {
        return searchServer().client();
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
        searchServer().importFixtureResource(resourcePath, getClass());
    }
}
