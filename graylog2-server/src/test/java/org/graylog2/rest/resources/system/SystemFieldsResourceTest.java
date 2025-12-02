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
package org.graylog2.rest.resources.system;

import org.assertj.core.api.Assertions;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.plugin.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashSet;
import java.util.List;

class SystemFieldsResourceTest {

    private Indices indices;
    private SystemFieldsResource resource;

    @BeforeEach
    void setUp() {
        final IndexSetRegistry indexSetRegistry = Mockito.mock(IndexSetRegistry.class);

        Mockito.when(indexSetRegistry.getIndexWildcards()).thenReturn(new String[]{"index_1", "index_2", "index_3"});

        indices = Mockito.mock(Indices.class);
        Mockito.when(indices.getAllMessageFields(new String[]{"index_1"})).thenReturn(new LinkedHashSet<>(List.of("common_field", "1A", "1B", "1C", "1D", "1E")));
        Mockito.when(indices.getAllMessageFields(new String[]{"index_2"})).thenReturn(new LinkedHashSet<>(List.of("common_field", "2A", "2B", "2C", "2D", "2E")));
        Mockito.when(indices.getAllMessageFields(new String[]{"index_3"})).thenReturn(new LinkedHashSet<>(List.of("common_field", "3A")));
        resource = new SystemFieldsResource(indices, indexSetRegistry);
    }

    @Test
    void testDefaultFields() {
        Assertions.assertThat(resource.fields(5).fields())
                .hasSize(8) // count with those extra added 3 standard fields
                .contains(Message.FIELD_SOURCE, Message.FIELD_MESSAGE, Message.FIELD_TIMESTAMP);
    }

    @Test
    void testDefaultFieldsUnlimited() {
        Assertions.assertThat(resource.fields(-1).fields())
                .hasSize(12)
                // This is tricky - if we don't set limit, we don't add default fields explicitly. But if they are not
                // in index set fields, they won't be included in results.
                .doesNotContain(Message.FIELD_SOURCE, Message.FIELD_MESSAGE, Message.FIELD_TIMESTAMP);
    }



    @Test
    void testOnlyFirstIndexSetUsed() {
        Assertions.assertThat(resource.fields(5).fields())
                .hasSize(8) // count with those extra added 3 standard fields
                .contains(Message.FIELD_SOURCE, Message.FIELD_MESSAGE, Message.FIELD_TIMESTAMP)
                .contains("common_field", "1A", "1B", "1C", "1D");

        Mockito.verify(indices, Mockito.never()).getAllMessageFields(new String[]{"index_2"});
        Mockito.verify(indices, Mockito.never()).getAllMessageFields(new String[]{"index_3"});
    }

    @Test
    void testMoreIndicesUsed() {
        Assertions.assertThat(resource.fields(10).fields())
                .hasSize(13) // count with those extra added 3 standard fields
                .contains(Message.FIELD_SOURCE, Message.FIELD_MESSAGE, Message.FIELD_TIMESTAMP)
                .contains("common_field", "1A", "1B", "1C", "1D", "1E", "2A", "2B", "2C", "2D");

        Mockito.verify(indices, Mockito.never()).getAllMessageFields(new String[]{"index_3"});
    }
}
