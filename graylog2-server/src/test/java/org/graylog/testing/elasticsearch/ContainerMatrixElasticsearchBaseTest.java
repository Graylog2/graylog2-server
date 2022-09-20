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
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collections;
import java.util.Map;

/**
 * This class can be used as base class for Elasticsearch integration tests.
 * <p>
 * Check the {@link #importFixture(String)} method if you need to load fixture data from JSON files.
 */
public abstract class ContainerMatrixElasticsearchBaseTest {
    private final SearchServerInstance elasticsearch;

    public ContainerMatrixElasticsearchBaseTest(SearchServerInstance elasticsearch) {
        this.elasticsearch = elasticsearch;
    }

    protected SearchServerInstance searchServer() {
        return this.elasticsearch;
    }

    // override this in derived classes to skip import if the default template. See old ElasticsearchBaseTest + IndicesGetAllMessageFieldsIT
    protected boolean skipTemplates() {
        return false;
    }

    @BeforeAll
    public void before() {
        if (!skipTemplates()) {
            addGraylogDefaultIndexTemplate();
        }
    }

    private void addGraylogDefaultIndexTemplate() {
        addIndexTemplates(getGraylogDefaultMessageTemplates(searchServer().version()));
    }

    private static Map<String, Map<String, Object>> getGraylogDefaultMessageTemplates(SearchVersion version) {
        final Map<String, Object> template =
                new MessageIndexTemplateProvider().create(version, null)
                        .messageTemplate("*", "standard", -1);
        return Collections.singletonMap("graylog-test-internal", template);
    }

    private void addIndexTemplates(Map<String, Map<String, Object>> templates) {
        for (Map.Entry<String, Map<String, Object>> template : templates.entrySet()) {
            final String templateName = template.getKey();

            searchServer().client().putTemplate(templateName, template.getValue());
        }
    }

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

    protected SearchVersion elasticsearchVersion() {
        return searchServer().version();
    }
}
