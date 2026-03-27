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
package org.graylog2.search;

import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;

class SearchQueryFieldTest {

    @Test
    void createWithBsonFilterCreator() {
        final SearchQueryField.BsonFilterCreator creator =
                (fieldName, fieldValue) -> eq("custom_field", fieldValue.getValue());

        final SearchQueryField field = SearchQueryField.create("db_field", SearchQueryField.Type.STRING, creator);

        assertThat(field.getDbField()).isEqualTo("db_field");
        assertThat(field.getFieldType()).isEqualTo(SearchQueryField.Type.STRING);
        assertThat(field.getBsonFilterCreator()).isPresent();
    }

    @Test
    void createWithoutBsonFilterCreator() {
        final SearchQueryField field = SearchQueryField.create("db_field", SearchQueryField.Type.STRING);

        assertThat(field.getBsonFilterCreator()).isEmpty();
    }

    @Test
    void createStringFieldWithoutBsonFilterCreator() {
        final SearchQueryField field = SearchQueryField.create("db_field");

        assertThat(field.getBsonFilterCreator()).isEmpty();
    }
}
