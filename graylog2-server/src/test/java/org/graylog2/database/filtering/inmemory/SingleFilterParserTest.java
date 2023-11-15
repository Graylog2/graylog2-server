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

import org.bson.types.ObjectId;
import org.graylog2.database.filtering.RangeFilter;
import org.graylog2.database.filtering.SingleValueFilter;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.search.SearchQueryField;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.graylog2.database.filtering.inmemory.SingleFilterParser.RANGE_VALUES_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SingleFilterParserTest {

    SingleFilterParser toTest;

    @BeforeEach
    void setUp() {
        toTest = new SingleFilterParser();
    }

    @Test
    void parsesFilterExpressionCorrectlyForStringType() {

        assertEquals(new SingleValueFilter("owner", "baldwin"),
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

        assertEquals(new SingleValueFilter("away", true),
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

        assertEquals(new SingleValueFilter("id", new ObjectId("5f4dfb9c69be46153b9a9a7b")),
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

        assertEquals(new SingleValueFilter("created_at", new DateTime(2012, 12, 12, 12, 12, 12, DateTimeZone.UTC).toDate()),
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

        assertEquals(new SingleValueFilter("num", 42),
                toTest.parseSingleExpression("num:42",
                        List.of(EntityAttribute.builder()
                                .id("num")
                                .title("Num")
                                .type(SearchQueryField.Type.INT)
                                .filterable(true)
                                .build())
                ));
    }

    @Test
    void parsesFilterExpressionCorrectlyForDateRanges() {
        final String fromString = "2012-12-12 12:12:12";
        final String toString = "2022-12-12 12:12:12";

        final List<EntityAttribute> entityAttributes = List.of(EntityAttribute.builder()
                .id("created_at")
                .title("Creation Date")
                .type(SearchQueryField.Type.DATE)
                .filterable(true)
                .build());

        assertEquals(
                new RangeFilter("created_at",
                        new DateTime(2012, 12, 12, 12, 12, 12, DateTimeZone.UTC).toDate(),
                        new DateTime(2022, 12, 12, 12, 12, 12, DateTimeZone.UTC).toDate()
                ),

                toTest.parseSingleExpression("created_at:" + fromString + RANGE_VALUES_SEPARATOR + toString,
                        entityAttributes
                ));
    }

    @Test
    void parsesFilterExpressionCorrectlyForOpenDateRanges() {
        final String dateString = "2012-12-12 12:12:12";
        final DateTime dateObject = new DateTime(2012, 12, 12, 12, 12, 12, DateTimeZone.UTC);

        final List<EntityAttribute> entityAttributes = List.of(EntityAttribute.builder()
                .id("created_at")
                .title("Creation Date")
                .type(SearchQueryField.Type.DATE)
                .filterable(true)
                .build());

        assertEquals(
                new RangeFilter("created_at", dateObject.toDate(), null),
                toTest.parseSingleExpression("created_at:" + dateString + RANGE_VALUES_SEPARATOR,
                        entityAttributes
                ));

        assertEquals(
                new RangeFilter("created_at", null, dateObject.toDate()),
                toTest.parseSingleExpression("created_at:" + RANGE_VALUES_SEPARATOR + dateString,
                        entityAttributes
                ));
    }

    @Test
    void parsesFilterExpressionCorrectlyForIntRanges() {
        final List<EntityAttribute> entityAttributes = List.of(EntityAttribute.builder()
                .id("number")
                .title("Number")
                .type(SearchQueryField.Type.INT)
                .filterable(true)
                .build());

        assertEquals(
                new RangeFilter("number", 42, 53),
                toTest.parseSingleExpression("number:42" + RANGE_VALUES_SEPARATOR + "53",
                        entityAttributes
                ));
    }

    @Test
    void parsesFilterExpressionForStringFieldsCorrectlyEvenIfValueContainsRangeSeparator() {
        final List<EntityAttribute> entityAttributes = List.of(EntityAttribute.builder()
                .id("text")
                .title("Text")
                .type(SearchQueryField.Type.STRING)
                .filterable(true)
                .build());

        assertEquals(
                new SingleValueFilter("text", "42" + RANGE_VALUES_SEPARATOR + "53"),

                toTest.parseSingleExpression("text:42" + RANGE_VALUES_SEPARATOR + "53",
                        entityAttributes
                ));
    }

}
