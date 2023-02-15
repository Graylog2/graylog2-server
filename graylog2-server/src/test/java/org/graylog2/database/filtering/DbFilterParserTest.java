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
package org.graylog2.database.filtering;

import com.mongodb.client.model.Filters;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.database.filtering.DbFilterParser.FIELD_AND_VALUE_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DbFilterParserTest {

    private DbFilterParser toTest;

    @BeforeEach
    void setUp() {
        toTest = new DbFilterParser();
    }

    @Test
    void returnsEmptyListOnNullFilterList() {
        assertThat(toTest.parse(null, List.of()))
                .isEmpty();
    }

    @Test
    void returnsEmptyListOnEmptyFilterList() {
        assertThat(toTest.parse(List.of(), List.of()))
                .isEmpty();
    }

    @Test
    void throwsExceptionsOnWrongFilterFormat() {
        final List<EntityAttribute> attributes = List.of(
                EntityAttribute.builder().id("good").title("Good").filterable(true).build(),
                EntityAttribute.builder().id("another").title("Hidden and dangerous").filterable(true).build()
        );
        assertThrows(IllegalArgumentException.class, () -> toTest.parse(List.of("No separator"), attributes));
        assertThrows(IllegalArgumentException.class, () -> toTest.parseSingleExpression("No separator", attributes));
        assertThrows(IllegalArgumentException.class, () -> toTest.parse(List.of(FIELD_AND_VALUE_SEPARATOR + "no field name"), attributes));
        assertThrows(IllegalArgumentException.class, () -> toTest.parseSingleExpression(FIELD_AND_VALUE_SEPARATOR + "no field name", attributes));
        assertThrows(IllegalArgumentException.class, () -> toTest.parse(List.of("no field value" + FIELD_AND_VALUE_SEPARATOR), attributes));
        assertThrows(IllegalArgumentException.class, () -> toTest.parseSingleExpression("no field value" + FIELD_AND_VALUE_SEPARATOR, attributes));
        assertThrows(IllegalArgumentException.class, () -> toTest.parse(
                List.of("good" + FIELD_AND_VALUE_SEPARATOR + "one",
                        "another" + FIELD_AND_VALUE_SEPARATOR + "good_one",
                        "single wrong one is enough to throw exception"),
                attributes)
        );
    }

    @Test
    void parsesFilterExpressionCorrectly() {

        assertEquals(Filters.eq("owner", "baldwin"),
                toTest.parseSingleExpression("owner:baldwin", List.of(EntityAttribute.builder().id("owner").title("Owner").filterable(true).build())));

    }
}
