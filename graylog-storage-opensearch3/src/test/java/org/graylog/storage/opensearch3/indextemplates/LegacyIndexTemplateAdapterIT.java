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
import org.opensearch.client.opensearch.indices.GetTemplateRequest;
import org.opensearch.client.opensearch.indices.GetTemplateResponse;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.TemplateMapping;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(OpenSearchTestServerExtension.class)
class LegacyIndexTemplateAdapterIT {

    private static final Template SAMPLE_LEGACY_TEMPLATE = new Template(
            List.of("something_*"),
            new Template.Mappings(
                    Map.of("dynamic_templates", List.of(),
                            "properties", Map.of("http_response_code", Map.of("type", "long")
                            ),
                            "_source", Map.of("enabled", true))
            ),
            13L,
            new Template.Settings(Map.of())
    );

    private LegacyIndexTemplateAdapter toTest;
    private OpenSearchIndicesClient indicesClient;

    @BeforeEach
    void setUp(final OpenSearchInstance openSearchInstance) {
        toTest = new LegacyIndexTemplateAdapter(openSearchInstance.getOfficialOpensearchClient(), new OSSerializationUtils());
        indicesClient = openSearchInstance.getOfficialOpensearchClient().sync().indices();
    }

    @Test
    void testTemplateAdapter() throws Exception {
        assertFalse(toTest.indexTemplateExists("bubamara"));
        assertTrue(toTest.ensureIndexTemplate("bubamara", SAMPLE_LEGACY_TEMPLATE));
        assertTrue(toTest.indexTemplateExists("bubamara"));

        final GetTemplateResponse indexTemplateResponse = indicesClient.getTemplate(GetTemplateRequest.builder().name("bubamara").build());
        verifyTemplateIsCorrect(indexTemplateResponse);

        assertTrue(toTest.deleteIndexTemplate("bubamara"));
        assertThrows(RuntimeException.class, () -> toTest.deleteIndexTemplate("pitagoras"));

        assertFalse(toTest.indexTemplateExists("bubamara"));
    }

    private void verifyTemplateIsCorrect(final GetTemplateResponse indexTemplateResponse) {
        assertEquals(1, indexTemplateResponse.result().size());
        final TemplateMapping templateMapping = indexTemplateResponse.get("bubamara");
        assertNotNull(templateMapping);
        assertEquals(List.of("something_*"), templateMapping.indexPatterns());
        assertEquals(13, templateMapping.order());
        assertEquals(Map.of("http_response_code", Property.builder().long_(l -> l).build()), templateMapping.mappings().properties());
        assertEquals(SourceField.builder().enabled(true).build(), templateMapping.mappings().source());
        assertEquals(Map.of(), templateMapping.settings());
    }
}
