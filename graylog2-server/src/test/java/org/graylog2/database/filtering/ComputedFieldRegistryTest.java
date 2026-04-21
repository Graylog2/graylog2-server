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

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComputedFieldRegistryTest {

    @Test
    void registersProvidersCorrectly() {
        final MockComputedFieldProvider provider1 = new MockComputedFieldProvider("field1");
        final MockComputedFieldProvider provider2 = new MockComputedFieldProvider("field2");

        final ComputedFieldRegistry registry = new ComputedFieldRegistry(Set.of(provider1, provider2));

        assertTrue(registry.isComputedField("field1"));
        assertTrue(registry.isComputedField("field2"));
        assertFalse(registry.isComputedField("field3"));
    }

    @Test
    void retrievesProviderByFieldName() {
        final MockComputedFieldProvider provider = new MockComputedFieldProvider("test_field");
        final ComputedFieldRegistry registry = new ComputedFieldRegistry(Set.of(provider));

        final Optional<ComputedFieldProvider> result = registry.getProvider("test_field");
        assertTrue(result.isPresent());
        assertEquals("test_field", result.get().getFieldName());
    }

    @Test
    void returnsEmptyOptionalForUnknownField() {
        final MockComputedFieldProvider provider = new MockComputedFieldProvider("test_field");
        final ComputedFieldRegistry registry = new ComputedFieldRegistry(Set.of(provider));

        final Optional<ComputedFieldProvider> result = registry.getProvider("unknown_field");
        assertFalse(result.isPresent());
    }

    @Test
    void throwsExceptionForDuplicateFieldNames() {
        final MockComputedFieldProvider provider1 = new MockComputedFieldProvider("duplicate");
        final MockComputedFieldProvider provider2 = new MockComputedFieldProvider("duplicate");

        assertThrows(IllegalStateException.class, () -> new ComputedFieldRegistry(Set.of(provider1, provider2)));
    }

    @Test
    void handlesEmptyProviderSet() {
        final ComputedFieldRegistry registry = new ComputedFieldRegistry(Set.of());

        assertFalse(registry.isComputedField("any_field"));
        assertFalse(registry.getProvider("any_field").isPresent());
    }

    /**
     * Mock implementation for testing
     */
    private static class MockComputedFieldProvider implements ComputedFieldProvider {
        private final String fieldName;

        MockComputedFieldProvider(String fieldName) {
            this.fieldName = fieldName;
        }

        @Override
        public Set<String> getMatchingIds(String filterValue, String authToken) {
            return Set.of();
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }
    }
}
