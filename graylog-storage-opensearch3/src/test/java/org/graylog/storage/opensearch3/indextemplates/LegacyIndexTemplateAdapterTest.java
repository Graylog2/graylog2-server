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

import org.graylog.storage.opensearch3.OfficialOpensearchClient;
import org.graylog2.indexer.indices.Template;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.DeleteTemplateRequest;
import org.opensearch.client.opensearch.indices.DeleteTemplateResponse;
import org.opensearch.client.opensearch.indices.ExistsTemplateRequest;
import org.opensearch.client.opensearch.indices.OpenSearchIndicesClient;
import org.opensearch.client.opensearch.indices.PutTemplateRequest;
import org.opensearch.client.opensearch.indices.PutTemplateResponse;
import org.opensearch.client.transport.endpoints.BooleanResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LegacyIndexTemplateAdapterTest {

    private LegacyIndexTemplateAdapter toTest;

    @Mock
    private OSSerializationUtils templateMapper;

    @Mock
    private OpenSearchIndicesClient indicesClient;

    @BeforeEach
    void setUp() {
        OfficialOpensearchClient officialOpensearchClient = mock(OfficialOpensearchClient.class);
        OpenSearchClient syncClient = mock(OpenSearchClient.class);
        doReturn(syncClient).when(officialOpensearchClient).sync();
        doReturn(indicesClient).when(syncClient).indices();

        toTest = new LegacyIndexTemplateAdapter(officialOpensearchClient, templateMapper);
    }

    @Test
    void testEnsureIndexTemplateOnSuccessfulTemplateCreation() throws Exception {
        TypeMapping typeMapping = TypeMapping.builder().build();
        Map<String, JsonData> settings = Map.of();
        Template template = new Template(
                List.of("graylog-*"),
                new Template.Mappings(Map.of()),
                13L,
                new Template.Settings(Map.of())
        );
        doReturn(typeMapping).when(templateMapper).fromMap(template.mappings(), TypeMapping._DESERIALIZER);
        doReturn(settings).when(templateMapper).toJsonDataMap(template.settings());

        doReturn(PutTemplateResponse.builder().acknowledged(true).build())
                .when(indicesClient)
                .putTemplate(
                        PutTemplateRequest.builder()
                                .name("template")
                                .indexPatterns(List.of("graylog-*"))
                                .mappings(typeMapping)
                                .settings(settings)
                                .order(13)
                                .build()

                );

        assertTrue(toTest.ensureIndexTemplate("template", template));
    }

    @Test
    void testEnsureIndexTemplateOnFailedTemplateCreation() throws Exception {
        doReturn(PutTemplateResponse.builder().acknowledged(false).build())
                .when(indicesClient)
                .putTemplate(argThat((ArgumentMatcher<PutTemplateRequest>) argument -> argument.name().startsWith("uninsertable"))
                );

        assertFalse(toTest.ensureIndexTemplate("uninsertable_template", mock(Template.class)));
    }

    @Test
    void testIndexTemplateExists() throws Exception {
        doReturn(new BooleanResponse(true)).when(indicesClient)
                .existsTemplate(ExistsTemplateRequest.builder().name("I_am").build());
        doReturn(new BooleanResponse(false)).when(indicesClient)
                .existsTemplate(ExistsTemplateRequest.builder().name("I_am_not").build());
        doThrow(IOException.class).when(indicesClient)
                .existsTemplate(ExistsTemplateRequest.builder().name("I_am_causing_exceptions").build());

        assertTrue(toTest.indexTemplateExists("I_am"));
        assertFalse(toTest.indexTemplateExists("I_am_not"));
        assertThrows(RuntimeException.class, () -> toTest.indexTemplateExists("I_am_causing_exceptions"));
    }

    @Test
    void testIndexTemplateDeletion() throws Exception {
        doReturn(DeleteTemplateResponse.builder().acknowledged(true).build())
                .when(indicesClient)
                .deleteTemplate(DeleteTemplateRequest.builder().name("I_am").build());
        doReturn(DeleteTemplateResponse.builder().acknowledged(false).build())
                .when(indicesClient)
                .deleteTemplate(DeleteTemplateRequest.builder().name("I_am_not").build());
        doThrow(IOException.class)
                .when(indicesClient)
                .deleteTemplate(DeleteTemplateRequest.builder().name("I_am_causing_exceptions").build());

        assertTrue(toTest.deleteIndexTemplate("I_am"));
        assertFalse(toTest.deleteIndexTemplate("I_am_not"));
        assertThrows(RuntimeException.class, () -> toTest.deleteIndexTemplate("I_am_causing_exceptions"));
    }
}
