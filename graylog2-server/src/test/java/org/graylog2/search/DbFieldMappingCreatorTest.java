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

import org.graylog2.rest.resources.entities.EntityAttribute;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DbFieldMappingCreatorTest {

    @Test
    void ignoresUnsearchableFields() {
        final Map<String, SearchQueryField> result = DbFieldMappingCreator.createFromEntityAttributes(
                List.of(
                        EntityAttribute.builder().id("f1").title("Searchable flag not set").build(),
                        EntityAttribute.builder().id("f2").title("Searchable flag set to false").searchable(false).build()
                )
        );
        assertThat(result).isEmpty();
    }

    @Test
    void createsMappingForBothIdAndTitle() {
        final Map<String, SearchQueryField> result = DbFieldMappingCreator.createFromEntityAttributes(
                List.of(
                        EntityAttribute.builder().id("name").title("Nickname").searchable(true).type(SearchQueryField.Type.STRING).build()
                )
        );
        assertThat(result.size())
                .isEqualTo(2);

        assertThat(result.get("name"))
                .isSameAs(result.get("nickname"))
                .satisfies(sField -> assertThat(sField.getDbField()).isEqualTo("name"))
                .satisfies(sField -> assertThat(sField.getFieldType()).isEqualTo(SearchQueryField.Type.STRING));

        assertThat(result.get("Nickname")).isNull(); //title has been automatically changed to lowercase
    }

    @Test
    void doesNotCreateMappingForTitleIfItContainsSpace() {
        final Map<String, SearchQueryField> result = DbFieldMappingCreator.createFromEntityAttributes(
                List.of(
                        EntityAttribute.builder().id("number").title("Complex number").searchable(true).type(SearchQueryField.Type.LONG).build()
                )
        );
        assertThat(result.size())
                .isEqualTo(1);

        assertThat(result.get("number"))
                .satisfies(sField -> assertThat(sField.getDbField()).isEqualTo("number"))
                .satisfies(sField -> assertThat(sField.getFieldType()).isEqualTo(SearchQueryField.Type.LONG));
    }

}
