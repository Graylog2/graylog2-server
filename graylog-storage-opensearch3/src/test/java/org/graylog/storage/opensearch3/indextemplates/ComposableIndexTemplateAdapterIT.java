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
package org.graylog.storage.opensearch3.indextemplates;

import org.graylog.storage.opensearch3.testing.OpenSearchInstance;
import org.graylog.storage.opensearch3.testing.OpenSearchTestServerExtension;
import org.graylog2.indexer.indices.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.SourceField;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.GetIndexTemplateRequest;
import org.opensearch.client.opensearch.indices.GetIndexTemplateResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.IndexTemplate;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OpenSearchTestServerExtension.class)
class ComposableIndexTemplateAdapterIT {

    private static final Template SAMPLE_COMPSABLE_TEMPLATE = new Template(
            List.of("something_composable_*"),
            new Template.Mappings(
                    Map.of("dynamic_templates", List.of(),
                            "properties", Map.of("http_response_code", Map.of("type", "long")
                            ),
                            "_source", Map.of("enabled", false))
            ),
            13L,
            new Template.Settings(Map.of())
    );
    
    private ComposableIndexTemplateAdapter toTest;
    private OpenSearchIndicesClient indicesClient;

    @BeforeEach
    void setUp(final OpenSearchInstance openSearchInstance) {
        toTest = new ComposableIndexTemplateAdapter(openSearchInstance.getOfficialOpensearchClient(), new OSSerializationUtils());
        indicesClient = openSearchInstance.getOfficialOpensearchClient().sync().indices();
    }

    @Test
    void testTemplateAdapter() throws Exception {
        assertFalse(toTest.indexTemplateExists("bubamara"));
        assertTrue(toTest.ensureIndexTemplate("bubamara", SAMPLE_COMPSABLE_TEMPLATE));
        assertTrue(toTest.indexTemplateExists("bubamara"));

        final GetIndexTemplateResponse indexTemplateResponse = indicesClient.getIndexTemplate(GetIndexTemplateRequest.builder().name("bubamara").build());
        verifyTemplateIsCorrect(indexTemplateResponse);

        assertTrue(toTest.deleteIndexTemplate("bubamara"));
        assertThrows(RuntimeException.class, () -> toTest.deleteIndexTemplate("pitagoras"));

        assertFalse(toTest.indexTemplateExists("bubamara"));
    }

    private void verifyTemplateIsCorrect(GetIndexTemplateResponse indexTemplateResponse) {
        assertEquals(1, indexTemplateResponse.indexTemplates().size());
        final IndexTemplate indexTemplate = indexTemplateResponse.indexTemplates().getFirst().indexTemplate();
        assertNotNull(indexTemplate);
        assertEquals(List.of("something_composable_*"), indexTemplate.indexPatterns());
        assertEquals(13, indexTemplate.priority());
        assertNotNull(indexTemplate.template());
        final TypeMapping mappings = indexTemplate.template().mappings();
        assertNotNull(mappings);
        assertEquals(Map.of("http_response_code", Property.builder().long_(l -> l).build()), mappings.properties());
        assertEquals(SourceField.builder().enabled(false).build(), mappings.source());
        assertEquals(IndexSettings.builder().build(), indexTemplate.template().settings());
    }
}
