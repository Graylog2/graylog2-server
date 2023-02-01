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
package org.graylog.plugins.views.search.engine.suggestions;

import org.assertj.core.api.Assertions;
import org.graylog2.indexer.fieldtypes.FieldTypeMapper;
import org.junit.jupiter.api.Test;

class SuggestionFieldTypeTest {

    @Test
    void testFieldPropertyMatching() {
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.STRING_TYPE)).isEqualTo(SuggestionFieldType.TEXTUAL);
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.STRING_FTS_TYPE)).isEqualTo(SuggestionFieldType.TEXTUAL);

        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.INT_TYPE)).isEqualTo(SuggestionFieldType.NUMERICAL);
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.LONG_TYPE)).isEqualTo(SuggestionFieldType.NUMERICAL);
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.FLOAT_TYPE)).isEqualTo(SuggestionFieldType.NUMERICAL);
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.DOUBLE_TYPE)).isEqualTo(SuggestionFieldType.NUMERICAL);
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.SHORT_TYPE)).isEqualTo(SuggestionFieldType.NUMERICAL);
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.BYTE_TYPE)).isEqualTo(SuggestionFieldType.NUMERICAL);

        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.DATE_TYPE)).isEqualTo(SuggestionFieldType.OTHER);
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.BINARY_TYPE)).isEqualTo(SuggestionFieldType.OTHER);
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.BOOLEAN_TYPE)).isEqualTo(SuggestionFieldType.OTHER);
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.GEO_POINT_TYPE)).isEqualTo(SuggestionFieldType.OTHER);
        Assertions.assertThat(SuggestionFieldType.fromFieldType(FieldTypeMapper.IP_TYPE)).isEqualTo(SuggestionFieldType.OTHER);
    }
}
