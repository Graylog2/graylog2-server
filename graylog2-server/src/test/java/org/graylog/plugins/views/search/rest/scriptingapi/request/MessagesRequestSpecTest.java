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
package org.graylog.plugins.views.search.rest.scriptingapi.request;

import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseEntryDataType;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MessagesRequestSpecTest {

    @Test
    void testSchemaCreation() {
        final Map<String, String> fieldTypes = Map.of(
                "age", "long",
                "salary", "double",
                "position", "keyword"
        );

        final List<String> specifiedFields = List.of("age", "salary", "position", "nvmd");
        final List<ResponseSchemaEntry> schema = new MessagesRequestSpec("", Set.of(), null, "age", null, 0, 1, specifiedFields)
                .getSchema(fieldTypes);

        assertThat(schema).containsAll(
                Set.of(
                        ResponseSchemaEntry.field("age", ResponseEntryDataType.NUMERIC),
                        ResponseSchemaEntry.field("salary", ResponseEntryDataType.NUMERIC),
                        ResponseSchemaEntry.field("position", ResponseEntryDataType.STRING),
                        ResponseSchemaEntry.field("nvmd", ResponseEntryDataType.UNKNOWN)
                )
        );

    }
}
