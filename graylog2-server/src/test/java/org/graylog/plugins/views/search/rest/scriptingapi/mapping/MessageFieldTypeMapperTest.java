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
package org.graylog.plugins.views.search.rest.scriptingapi.mapping;

import org.assertj.core.api.Assertions;
import org.graylog.plugins.views.search.rest.MappedFieldTypeDTO;
import org.graylog.plugins.views.search.rest.scriptingapi.request.RequestedField;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseEntryDataType;
import org.graylog.plugins.views.search.rest.scriptingapi.response.ResponseSchemaEntry;
import org.junit.jupiter.api.Test;

import java.util.Set;

class MessageFieldTypeMapperTest {

    @Test
    void testSchemaCreation() {
        final Set<MappedFieldTypeDTO> knownFields = Set.of(
                MappedFieldTypeDTO.create("age", org.graylog2.indexer.fieldtypes.FieldTypeMapper.LONG_TYPE),
                MappedFieldTypeDTO.create("salary", org.graylog2.indexer.fieldtypes.FieldTypeMapper.DOUBLE_TYPE),
                MappedFieldTypeDTO.create("position", org.graylog2.indexer.fieldtypes.FieldTypeMapper.STRING_TYPE)
        );

        final MessageFieldTypeMapper mapper = new MessageFieldTypeMapper(knownFields);

        Assertions.assertThat(mapper.apply(RequestedField.parse("age"))).isEqualTo(ResponseSchemaEntry.field("age", ResponseEntryDataType.NUMERIC));
        Assertions.assertThat(mapper.apply(RequestedField.parse("salary"))).isEqualTo(ResponseSchemaEntry.field("salary", ResponseEntryDataType.NUMERIC));
        Assertions.assertThat(mapper.apply(RequestedField.parse("position"))).isEqualTo(ResponseSchemaEntry.field("position", ResponseEntryDataType.STRING));
        Assertions.assertThat(mapper.apply(RequestedField.parse("nvmd"))).isEqualTo(ResponseSchemaEntry.field("nvmd", ResponseEntryDataType.UNKNOWN));
    }
}
