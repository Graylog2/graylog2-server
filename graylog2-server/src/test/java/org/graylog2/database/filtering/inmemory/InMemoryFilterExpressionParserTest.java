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
package org.graylog2.database.filtering.inmemory;

import com.google.common.base.Predicates;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.database.filtering.inmemory.SingleFilterParser.FIELD_AND_VALUE_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryFilterExpressionParserTest {

    private InMemoryFilterExpressionParser toTest;

    @BeforeEach
    void setUp() {
        toTest = new InMemoryFilterExpressionParser();
    }

    @Test
    void returnsAlwaysTruePredicateOnNullFilterList() {
        assertThat(toTest.parse(null, List.of()))
                .isEqualTo(Predicates.alwaysTrue());
    }

    @Test
    void returnsAlwaysTruePredicateOnEmptyFilterList() {
        assertThat(toTest.parse(List.of(), List.of()))
                .isEqualTo(Predicates.alwaysTrue());
    }

    @Test
    void throwsExceptionOnFieldThatDoesNotExistInAttributeList() {

        assertThrows(IllegalArgumentException.class, () ->
                toTest.parse(List.of("strange_field:blabla"),
                        List.of(EntityAttribute.builder()
                                .id("owner")
                                .title("Owner")
                                .filterable(true)
                                .build())
                ));
    }

    @Test
    void throwsExceptionOnFieldThatIsNotFilterable() {

        assertThrows(IllegalArgumentException.class, () ->
                toTest.parse(List.of("owner:juan"),
                        List.of(EntityAttribute.builder()
                                .id("owner")
                                .title("Owner")
                                .filterable(false)
                                .build())
                ));
    }

    @Test
    void throwsExceptionOnWrongFilterFormat() {
        final List<EntityAttribute> attributes = List.of(
                EntityAttribute.builder().id("good").title("Good").filterable(true).build(),
                EntityAttribute.builder().id("another").title("Hidden and dangerous").filterable(true).build()
        );
        assertThrows(IllegalArgumentException.class, () -> toTest.parse(List.of("No separator"), attributes));
        assertThrows(IllegalArgumentException.class, () -> toTest.parse(List.of(FIELD_AND_VALUE_SEPARATOR + "no field name"), attributes));
        assertThrows(IllegalArgumentException.class, () -> toTest.parse(List.of("no field value" + FIELD_AND_VALUE_SEPARATOR), attributes));
        assertThrows(IllegalArgumentException.class, () -> toTest.parse(
                List.of("good" + FIELD_AND_VALUE_SEPARATOR + "one",
                        "another" + FIELD_AND_VALUE_SEPARATOR + "good_one",
                        "single wrong one is enough to throw exception"),
                attributes)
        );
    }

    //

    @Test
    void groupsMultipleFilterForTheSameFieldUsingOrLogic() {

        final List<EntityAttribute> attributes = List.of(EntityAttribute.builder()
                        .id("owner")
                        .title("Owner")
                        .filterable(true)
                        .build(),
                EntityAttribute.builder()
                        .id("title")
                        .title("Title")
                        .filterable(true)
                        .build());


        final Predicate<InMemoryFilterable> predicate = toTest.parse(List.of("owner:baldwin", "owner:beomund", "title:crusade"), attributes);
        assertTrue(predicate.test(createMock(Map.of("owner", "baldwin", "title", "crusade"))));
        assertTrue(predicate.test(createMock(Map.of("owner", "beomund", "title", "crusade"))));
        assertFalse(predicate.test(createMock(Map.of("owner", "casimir", "title", "crusade"))));
        assertFalse(predicate.test(createMock(Map.of("owner", "beomund", "title", "whatever"))));
    }

    private InMemoryFilterable createMock(final Map<String, Object> fields) {
        return fieldName -> Optional.ofNullable(fields.get(fieldName));
    }

}
