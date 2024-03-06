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
import org.graylog2.database.filtering.inmemory.InMemoryFilterable;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleValueFilterTest {

    @Test
    void testPredicateCreation() {
        final SingleValueFilter toTest = new SingleValueFilter("field", "value");
        final Predicate<InMemoryFilterable> predicate = toTest.toPredicate();

        assertTrue(predicate.test(createMock(Map.of("field", "value"))));
        assertFalse(predicate.test(createMock(Map.of())));
        assertFalse(predicate.test(createMock(Map.of("field", "wrong_value"))));
    }

    @Test
    void testBsonCreation() {
        final SingleValueFilter toTest = new SingleValueFilter("field", "value");
        final Bson bson = toTest.toBson();

        assertEquals(Filters.eq("field", "value"), bson);
    }

    private InMemoryFilterable createMock(final Map<String, Object> fields) {
        return fieldName -> Optional.ofNullable(fields.get(fieldName));
    }
}
