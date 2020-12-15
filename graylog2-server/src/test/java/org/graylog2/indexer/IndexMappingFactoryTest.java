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

import com.github.zafarkhaja.semver.Version;
import org.graylog2.indexer.cluster.Node;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexMappingFactoryTest {

    private Node node;

    private IndexMappingFactory sut;

    @BeforeEach
    public void setUp() throws Exception {
        this.node = mock(Node.class);
        this.sut = new IndexMappingFactory(node);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1.7.3",
            "2.0.0",
            "3.0.0",
            "4.0.0",
            "8.0.0",
            "9.0.0"
    })
    void messageMappingFailsForUnsupportedElasticsearchVersion(String version) {
        testForUnsupportedVersion(version, IndexSetConfig.TemplateType.MESSAGES);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1.7.3",
            "2.0.0",
            "3.0.0",
            "4.0.0",
            "8.0.0",
            "9.0.0"
    })
    void eventsMappingFailsForUnsupportedElasticsearchVersion(String version) {
        testForUnsupportedVersion(version, IndexSetConfig.TemplateType.EVENTS);
    }

    private void testForUnsupportedVersion(String version, IndexSetConfig.TemplateType templateType) {
        mockNodeVersion(version);

        assertThatThrownBy(() -> sut.createIndexMapping(templateType))
                .isInstanceOf(ElasticsearchException.class)
                .hasMessageStartingWith("Unsupported Elasticsearch version: " + version)
                .hasNoCause();
    }

    @ParameterizedTest
    @CsvSource({
            "5.0.0, IndexMapping5",
            "5.1.0, IndexMapping5",
            "5.2.0, IndexMapping5",
            "5.3.0, IndexMapping5",
            "5.4.0, IndexMapping5",
            "6.3.1, IndexMapping6",
            "6.8.1, IndexMapping6",
            "7.8.0, IndexMapping7"
    })
    void createsMessageIndexMappings(String version, String expectedMappingClass) throws ClassNotFoundException {
        testForIndexMappingType(version, expectedMappingClass, IndexSetConfig.TemplateType.MESSAGES);
    }

    @ParameterizedTest
    @CsvSource({
            "5.0.0, EventsIndexMapping6",
            "5.1.0, EventsIndexMapping6",
            "5.2.0, EventsIndexMapping6",
            "5.3.0, EventsIndexMapping6",
            "5.4.0, EventsIndexMapping6",
            "6.3.1, EventsIndexMapping6",
            "6.8.1, EventsIndexMapping6",
            "7.8.0, EventsIndexMapping7"
    })
    void createsEventIndexMappings(String version, String expectedMappingClass) throws ClassNotFoundException {
        testForIndexMappingType(version, expectedMappingClass, IndexSetConfig.TemplateType.EVENTS);
    }

    private void testForIndexMappingType(String version, String mappingClassName, IndexSetConfig.TemplateType templateType) throws ClassNotFoundException {
        mockNodeVersion(version);

        final Class<?> expectedMappingClass = Class.forName("org.graylog2.indexer." + mappingClassName);

        assertThat(sut.createIndexMapping(templateType)).isInstanceOf(expectedMappingClass);
    }

    private void mockNodeVersion(String version) {
        when(node.getVersion()).thenReturn(Optional.of(Version.valueOf(version)));
    }
}
