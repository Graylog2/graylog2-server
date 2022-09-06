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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.storage.SearchVersion;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IndexMappingTest {

    private static final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private IndexSetConfig indexSetConfig;

    @BeforeEach
    void setUp() {
        indexSetConfig = mock(IndexSetConfig.class);
        when(indexSetConfig.indexAnalyzer()).thenReturn("standard");
    }

    @ParameterizedTest
    @CsvSource({
            "7.0.0, expected_template7.json",
            "OpenSearch:1.2.3, expected_template7.json"
    })
    void createsValidMappingTemplates(final String versionString, final String expectedTemplateFileName) throws Exception {
        final SearchVersion version = SearchVersion.decode(versionString);
        final IndexMappingTemplate mapping = new MessageIndexTemplateProvider().create(version, Mockito.mock(IndexSetConfig.class));

        final Map<String, Object> template = mapping.toTemplate(indexSetConfig, "sampleIndexTemplate");
        final String fixture = resourceFile(expectedTemplateFileName);

        JSONAssert.assertEquals(json(template), fixture, true);
    }

    private String json(Object value) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    private String resourceFile(String filename) {
        try {
            final URL resource = this.getClass().getResource(filename);
            if (resource == null) {
                Assert.fail("Unable to find resource file for test: " + filename);
            }
            final Path path = Paths.get(resource.toURI());
            final byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
