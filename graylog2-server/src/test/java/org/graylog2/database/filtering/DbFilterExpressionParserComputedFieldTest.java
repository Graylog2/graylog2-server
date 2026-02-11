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
import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.search.SearchQueryField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbFilterExpressionParserComputedFieldTest {

    private DbFilterExpressionParser parser;

    @Mock
    private ComputedFieldRegistry computedFieldRegistry;

    @Mock
    private ComputedFieldProvider mockProvider;

    private final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder()
                    .id("title")
                    .title("Title")
                    .type(SearchQueryField.Type.STRING)
                    .filterable(true)
                    .build(),
            EntityAttribute.builder()
                    .id("computed_field")
                    .title("Computed Field")
                    .type(SearchQueryField.Type.STRING)
                    .filterable(true)
                    .build()
    );

    @BeforeEach
    void setUp() {
        parser = new DbFilterExpressionParser(computedFieldRegistry);
    }

    @Test
    void filtersOnComputedFieldOnly() {
        // Setup
        when(computedFieldRegistry.isComputedField("computed_field")).thenReturn(true);
        when(computedFieldRegistry.getProvider("computed_field")).thenReturn(Optional.of(mockProvider));
        when(mockProvider.getMatchingIds(eq("test_value"), eq(null))).thenReturn(Set.of("507f1f77bcf86cd799439011", "507f1f77bcf86cd799439012"));

        // Execute
        final List<Bson> result = parser.parse(List.of("computed_field:test_value"), attributes);

        // Verify - should return an $in filter with the matching IDs
        assertEquals(1, result.size());
        final Bson filter = result.get(0);
        final BsonDocument filterDoc = filter.toBsonDocument();
        assertTrue(filterDoc.containsKey("_id"));
        assertTrue(filterDoc.get("_id").isDocument());
        assertTrue(filterDoc.get("_id").asDocument().containsKey("$in"));
    }

    @Test
    void filtersOnComputedFieldWithNoMatches() {
        // Setup - provider returns empty set
        when(computedFieldRegistry.isComputedField("computed_field")).thenReturn(true);
        when(computedFieldRegistry.getProvider("computed_field")).thenReturn(Optional.of(mockProvider));
        when(mockProvider.getMatchingIds(eq("no_match_value"), eq(null))).thenReturn(Set.of());

        // Execute
        final List<Bson> result = parser.parse(List.of("computed_field:no_match_value"), attributes);

        // Verify - should return a filter that matches nothing
        assertEquals(1, result.size());
        final Bson filter = result.get(0);
        final BsonDocument filterDoc = filter.toBsonDocument();
        assertTrue(filterDoc.containsKey("_id"));
        // Should filter on an impossible ObjectId
        assertEquals(new ObjectId("000000000000000000000000"), filterDoc.get("_id").asObjectId().getValue());
    }

    @Test
    void combinesComputedFieldAndDatabaseFieldFilters() {
        // Setup
        when(computedFieldRegistry.isComputedField("computed_field")).thenReturn(true);
        when(computedFieldRegistry.isComputedField("title")).thenReturn(false);
        when(computedFieldRegistry.getProvider("computed_field")).thenReturn(Optional.of(mockProvider));
        when(mockProvider.getMatchingIds(eq("computed_value"), eq(null))).thenReturn(Set.of("507f1f77bcf86cd799439011"));

        // Execute - mix computed and database field filters
        final List<Bson> result = parser.parse(
                List.of("computed_field:computed_value", "title:test_title"),
                attributes
        );

        // Verify - should return 2 filters (one for computed, one for database)
        assertEquals(2, result.size());
    }

    @Test
    void handlesMultipleValuesForSameComputedField() {
        // Setup - OR logic within same field
        when(computedFieldRegistry.isComputedField("computed_field")).thenReturn(true);
        when(computedFieldRegistry.getProvider("computed_field")).thenReturn(Optional.of(mockProvider));
        when(mockProvider.getMatchingIds(eq("value1"), eq(null))).thenReturn(Set.of("507f1f77bcf86cd799439011"));
        when(mockProvider.getMatchingIds(eq("value2"), eq(null))).thenReturn(Set.of("507f1f77bcf86cd799439012"));

        // Execute
        final List<Bson> result = parser.parse(
                List.of("computed_field:value1", "computed_field:value2"),
                attributes
        );

        // Verify - should combine both sets with OR logic (union)
        assertEquals(1, result.size());
        final Bson filter = result.get(0);
        final BsonDocument filterDoc = filter.toBsonDocument();
        assertTrue(filterDoc.containsKey("_id"));
        assertTrue(filterDoc.get("_id").asDocument().containsKey("$in"));
    }

    @Test
    void handlesInvalidComputedFieldValue() {
        // Setup - provider throws exception for invalid value
        when(computedFieldRegistry.isComputedField("computed_field")).thenReturn(true);
        when(computedFieldRegistry.getProvider("computed_field")).thenReturn(Optional.of(mockProvider));
        when(mockProvider.getMatchingIds(eq("invalid_value"), eq(null))).thenThrow(new IllegalArgumentException("Invalid value"));

        // Execute
        final List<Bson> result = parser.parse(List.of("computed_field:invalid_value"), attributes);

        // Verify - should handle exception gracefully and return empty result filter
        assertEquals(1, result.size());
        final Bson filter = result.get(0);
        final BsonDocument filterDoc = filter.toBsonDocument();
        assertTrue(filterDoc.containsKey("_id"));
    }

    @Test
    void handlesNoComputedFields() {
        // Setup - no computed fields
        when(computedFieldRegistry.isComputedField("title")).thenReturn(false);

        // Execute - only database field filters
        final List<Bson> result = parser.parse(List.of("title:test_title"), attributes);

        // Verify - should process normally as database field
        assertEquals(1, result.size());
        final Bson filter = result.get(0);
        final BsonDocument filterDoc = filter.toBsonDocument();
        assertFalse(filterDoc.containsKey("_id") && filterDoc.get("_id").isDocument());
    }
}
