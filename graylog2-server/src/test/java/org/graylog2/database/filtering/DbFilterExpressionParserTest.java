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
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.search.SearchQueryField;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.database.filtering.DbFilterExpressionParser.FIELD_AND_VALUE_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DbFilterExpressionParserTest {

    private DbFilterExpressionParser toTest;

    @BeforeEach
    void setUp() {
        toTest = new DbFilterExpressionParser();
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
    void throwsExceptionOnFieldThatDoesNotExistInAttributeList() {

        assertThrows(IllegalArgumentException.class, () ->
                toTest.parseSingleExpression("strange_field:blabla",
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
                toTest.parseSingleExpression("owner:juan",
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


        final List<Bson> expectedResult = List.of(
                Filters.or(
                        Filters.eq("owner", "baldwin"),
                        Filters.eq("owner", "beomund")
                ),
                Filters.eq("title", "crusade")

        );
        assertEquals(expectedResult,
                toTest.parse(List.of("owner:baldwin", "owner:beomund", "title:crusade"), attributes));
    }

    @Test
    void parsesFilterExpressionCorrectlyForStringType() {

        assertEquals(Filters.eq("owner", "baldwin"),
                toTest.parseSingleExpression("owner:baldwin",
                        List.of(EntityAttribute.builder()
                                .id("owner")
                                .title("Owner")
                                .filterable(true)
                                .build())
                ));
    }

    @Test
    void parsesFilterExpressionCorrectlyForBoolType() {

        assertEquals(Filters.eq("away", true),
                toTest.parseSingleExpression("away:true",
                        List.of(EntityAttribute.builder()
                                .id("away")
                                .title("Away")
                                .type(SearchQueryField.Type.BOOLEAN)
                                .filterable(true)
                                .build())
                ));
    }

    @Test
    void parsesFilterExpressionCorrectlyForObjectIdType() {

        assertEquals(Filters.eq("id", new ObjectId("5f4dfb9c69be46153b9a9a7b")),
                toTest.parseSingleExpression("id:5f4dfb9c69be46153b9a9a7b",
                        List.of(EntityAttribute.builder()
                                .id("id")
                                .title("Id")
                                .type(SearchQueryField.Type.OBJECT_ID)
                                .filterable(true)
                                .build())
                ));
    }

    @Test
    void parsesFilterExpressionCorrectlyForDateType() {

        assertEquals(Filters.eq("created_at", new DateTime(2012, 12, 12, 12, 12, 12, DateTimeZone.UTC)),
                toTest.parseSingleExpression("created_at:2012-12-12 12:12:12",
                        List.of(EntityAttribute.builder()
                                .id("created_at")
                                .title("Creation Date")
                                .type(SearchQueryField.Type.DATE)
                                .filterable(true)
                                .build())
                ));
    }

    @Test
    void parsesFilterExpressionCorrectlyForIntType() {

        assertEquals(Filters.eq("num", 42),
                toTest.parseSingleExpression("num:42",
                        List.of(EntityAttribute.builder()
                                .id("num")
                                .title("Num")
                                .type(SearchQueryField.Type.INT)
                                .filterable(true)
                                .build())
                ));
    }
}
