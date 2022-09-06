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
package org.graylog2.indexer;

import com.google.common.collect.ImmutableMap;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.indexer.EventIndexTemplateProvider.EVENT_TEMPLATE_TYPE;
import static org.graylog2.indexer.MessageIndexTemplateProvider.MESSAGE_TEMPLATE_TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexMappingFactoryTest {

    public static final ImmutableMap<String, IndexTemplateProvider> TEMPLATE_PROVIDERS = ImmutableMap.of(
            MESSAGE_TEMPLATE_TYPE, new MessageIndexTemplateProvider(),
            EVENT_TEMPLATE_TYPE, new EventIndexTemplateProvider()
    );

    @ParameterizedTest
    @ValueSource(strings = {
            "1.7.3",
            "2.0.0",
            "3.0.0",
            "4.0.0",
            "5.0.0",
            "6.0.0",
            "8.0.0",
            "9.0.0",
            "OpenSearch:2.0.3"
    })
    void mappingFailsForUnsupportedElasticsearchVersion(final String version) {
        testForUnsupportedVersion(version);
    }

    private void testForUnsupportedVersion(final String version) {
        final IndexMappingFactory indexMappingFactory = new IndexMappingFactory(createNodeWithVersion(version), TEMPLATE_PROVIDERS);

        assertThatThrownBy(() -> indexMappingFactory.createIndexMapping(mock(IndexSetConfig.class)))
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unsupported Search version")
                .hasMessageEndingWith(version)
                .hasNoCause();
    }

    @ParameterizedTest
    @CsvSource({
            "OpenSearch:1.2.3, IndexMapping7",
            "7.8.0, IndexMapping7"
    })
    void createsMessageIndexMappings(final String version, final String expectedMappingClass) throws ClassNotFoundException {
        testForIndexMappingType(version, expectedMappingClass, MESSAGE_TEMPLATE_TYPE);
    }

    @ParameterizedTest
    @CsvSource({
            "OpenSearch:1.2.3, EventsIndexMapping7",
            "7.8.0, EventsIndexMapping7"
    })
    void createsEventIndexMappings(final String version, final String expectedMappingClass) throws ClassNotFoundException {
        testForIndexMappingType(version, expectedMappingClass, EVENT_TEMPLATE_TYPE);
    }

    private void testForIndexMappingType(final String version, final String mappingClassName, final String templateType) throws ClassNotFoundException {
        final IndexMappingFactory indexMappingFactory = new IndexMappingFactory(createNodeWithVersion(version), TEMPLATE_PROVIDERS);

        final IndexSetConfig indexSetConfig = mock(IndexSetConfig.class);
        when(indexSetConfig.indexTemplateType()).thenReturn(Optional.of(templateType));

        final Class<?> expectedMappingClass = Class.forName("org.graylog2.indexer." + mappingClassName);

        assertThat(indexMappingFactory.createIndexMapping(indexSetConfig)).isInstanceOf(expectedMappingClass);
    }

    private Node createNodeWithVersion(final String version) {
        return new Node(() -> Optional.of(SearchVersion.decode(version)));
    }
}
