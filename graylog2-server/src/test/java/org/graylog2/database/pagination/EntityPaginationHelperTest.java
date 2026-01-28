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
package org.graylog2.database.pagination;

import org.graylog.grn.GRNDescriptor;
import org.graylog2.rest.models.SortOrder;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.Mockito.mock;

class EntityPaginationHelperTest {

    static Stream<org.junit.jupiter.params.provider.Arguments> filterProvider() {
        return Stream.of(
                of("type:example", true),
                of("title:example", true),
                of("example", true),
                of("invalid:filter", false),
                of(null, true)
        );
    }

    @ParameterizedTest
    @MethodSource("filterProvider")
    void testBuildPredicateParameterized(String filter, boolean expected) {
        Predicate<GRNDescriptor> predicate = EntityPaginationHelper.buildPredicate(
                filter,
                Map.of(
                        "type", descriptor -> "example",
                        "title", descriptor -> "Example Title")
        );

        assertThat(predicate.test(mock(GRNDescriptor.class))).isEqualTo(expected);
    }

    @Nested
    class BuildComparatorTest {

        record TestEntity(String name, String description, String category) {}

        private static final Map<String, Function<TestEntity, String>> FIELD_EXTRACTORS = Map.of(
                "name", TestEntity::name,
                "description", TestEntity::description,
                "category", TestEntity::category
        );

        @Test
        void testAscendingSortByName() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Charlie", "desc3", "cat1"),
                    new TestEntity("Alice", "desc1", "cat2"),
                    new TestEntity("Bob", "desc2", "cat3")
            );

            Comparator<TestEntity> comparator = EntityPaginationHelper.buildComparator(
                    "name", SortOrder.ASCENDING, FIELD_EXTRACTORS);

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            assertThat(sorted).extracting(TestEntity::name)
                    .containsExactly("Alice", "Bob", "Charlie");
        }

        @Test
        void testDescendingSortByName() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Charlie", "desc3", "cat1"),
                    new TestEntity("Alice", "desc1", "cat2"),
                    new TestEntity("Bob", "desc2", "cat3")
            );

            Comparator<TestEntity> comparator = EntityPaginationHelper.buildComparator(
                    "name", SortOrder.DESCENDING, FIELD_EXTRACTORS);

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            assertThat(sorted).extracting(TestEntity::name)
                    .containsExactly("Charlie", "Bob", "Alice");
        }

        @Test
        void testInvalidSortField_ReturnsZero() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Charlie", "desc3", "cat1"),
                    new TestEntity("Alice", "desc1", "cat2"),
                    new TestEntity("Bob", "desc2", "cat3")
            );

            Comparator<TestEntity> comparator = EntityPaginationHelper.buildComparator(
                    "invalid_field", SortOrder.ASCENDING, FIELD_EXTRACTORS);

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            // Order should remain unchanged when comparator returns 0
            assertThat(sorted).extracting(TestEntity::name)
                    .containsExactly("Charlie", "Alice", "Bob");
        }

        @Test
        void testEmptyFieldExtractors() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Charlie", "desc3", "cat1"),
                    new TestEntity("Alice", "desc1", "cat2")
            );

            Comparator<TestEntity> comparator = EntityPaginationHelper.buildComparator(
                    "name", SortOrder.ASCENDING, Map.of());

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            // Order should remain unchanged
            assertThat(sorted).extracting(TestEntity::name)
                    .containsExactly("Charlie", "Alice");
        }

        @Test
        void testSortByDifferentFields() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Alice", "Zebra", "Cat1"),
                    new TestEntity("Bob", "Apple", "Cat3"),
                    new TestEntity("Charlie", "Banana", "Cat2")
            );

            // Sort by description
            Comparator<TestEntity> comparator = EntityPaginationHelper.buildComparator(
                    "description", SortOrder.ASCENDING, FIELD_EXTRACTORS);

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            assertThat(sorted).extracting(TestEntity::description)
                    .containsExactly("Apple", "Banana", "Zebra");
        }
    }

    @Nested
    class BuildNumericComparatorTest {

        record TestEntity(String name, Long count, Long value) {}

        private static final Map<String, Function<TestEntity, Long>> NUMERIC_FIELD_EXTRACTORS = Map.of(
                "count", TestEntity::count,
                "value", TestEntity::value
        );

        @Test
        void testAscendingSortByCount() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Item1", 300L, 10L),
                    new TestEntity("Item2", 100L, 20L),
                    new TestEntity("Item3", 200L, 30L)
            );

            Comparator<TestEntity> comparator = EntityPaginationHelper.buildNumericComparator(
                    "count", SortOrder.ASCENDING, NUMERIC_FIELD_EXTRACTORS);

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            assertThat(sorted).extracting(TestEntity::count)
                    .containsExactly(100L, 200L, 300L);
        }

        @Test
        void testDescendingSortByCount() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Item1", 300L, 10L),
                    new TestEntity("Item2", 100L, 20L),
                    new TestEntity("Item3", 200L, 30L)
            );

            Comparator<TestEntity> comparator = EntityPaginationHelper.buildNumericComparator(
                    "count", SortOrder.DESCENDING, NUMERIC_FIELD_EXTRACTORS);

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            assertThat(sorted).extracting(TestEntity::count)
                    .containsExactly(300L, 200L, 100L);
        }

        @Test
        void testInvalidSortField_ReturnsZero() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Item1", 300L, 10L),
                    new TestEntity("Item2", 100L, 20L),
                    new TestEntity("Item3", 200L, 30L)
            );

            Comparator<TestEntity> comparator = EntityPaginationHelper.buildNumericComparator(
                    "invalid_field", SortOrder.ASCENDING, NUMERIC_FIELD_EXTRACTORS);

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            // Order should remain unchanged when comparator returns 0
            assertThat(sorted).extracting(TestEntity::count)
                    .containsExactly(300L, 100L, 200L);
        }

        @Test
        void testEmptyFieldExtractors() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Item1", 300L, 10L),
                    new TestEntity("Item2", 100L, 20L)
            );

            Comparator<TestEntity> comparator = EntityPaginationHelper.buildNumericComparator(
                    "count", SortOrder.ASCENDING, Map.of());

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            // Order should remain unchanged
            assertThat(sorted).extracting(TestEntity::count)
                    .containsExactly(300L, 100L);
        }

        @Test
        void testNegativeNumbers() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Item1", 100L, 10L),
                    new TestEntity("Item2", -50L, 20L),
                    new TestEntity("Item3", -100L, 30L)
            );

            Comparator<TestEntity> comparator = EntityPaginationHelper.buildNumericComparator(
                    "count", SortOrder.ASCENDING, NUMERIC_FIELD_EXTRACTORS);

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            assertThat(sorted).extracting(TestEntity::count)
                    .containsExactly(-100L, -50L, 100L);
        }

        @Test
        void testSortByDifferentFields() {
            List<TestEntity> entities = List.of(
                    new TestEntity("Item1", 300L, 50L),
                    new TestEntity("Item2", 100L, 30L),
                    new TestEntity("Item3", 200L, 40L)
            );

            // Sort by value instead of count
            Comparator<TestEntity> comparator = EntityPaginationHelper.buildNumericComparator(
                    "value", SortOrder.ASCENDING, NUMERIC_FIELD_EXTRACTORS);

            List<TestEntity> sorted = entities.stream().sorted(comparator).toList();

            assertThat(sorted).extracting(TestEntity::value)
                    .containsExactly(30L, 40L, 50L);
        }
    }

    @Nested
    class EntityFiltersPredicateTest {

        record TestEntity(String name, String type, String category) {}

        private static final Map<String, Function<TestEntity, String>> FIELD_EXTRACTORS = Map.of(
                "name", TestEntity::name,
                "type", TestEntity::type,
                "category", TestEntity::category
        );

        @Test
        void testNullFilters_AcceptsAll() {
            Predicate<TestEntity> predicate = EntityPaginationHelper.entityFiltersPredicate(
                    null,
                    filter -> entity -> false  // Even with reject-all factory
            );

            TestEntity entity = new TestEntity("Test", "TypeA", "Cat1");
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        void testEmptyFilters_AcceptsAll() {
            Predicate<TestEntity> predicate = EntityPaginationHelper.entityFiltersPredicate(
                    List.of(),
                    filter -> entity -> false  // Even with reject-all factory
            );

            TestEntity entity = new TestEntity("Test", "TypeA", "Cat1");
            assertThat(predicate.test(entity)).isTrue();
        }

        @Test
        void testSingleFilter_Matches() {
            Predicate<TestEntity> predicate = EntityPaginationHelper.entityFiltersPredicate(
                    List.of("name:Test"),
                    filter -> EntityPaginationHelper.buildPredicate(filter, FIELD_EXTRACTORS)
            );

            TestEntity matchingEntity = new TestEntity("Test", "TypeA", "Cat1");
            TestEntity nonMatchingEntity = new TestEntity("Other", "TypeA", "Cat1");

            assertThat(predicate.test(matchingEntity)).isTrue();
            assertThat(predicate.test(nonMatchingEntity)).isFalse();
        }

        @Test
        void testSingleFilter_NoMatch() {
            Predicate<TestEntity> predicate = EntityPaginationHelper.entityFiltersPredicate(
                    List.of("name:NonExistent"),
                    filter -> EntityPaginationHelper.buildPredicate(filter, FIELD_EXTRACTORS)
            );

            TestEntity entity = new TestEntity("Test", "TypeA", "Cat1");
            assertThat(predicate.test(entity)).isFalse();
        }

        @Test
        void testMultipleFilters_OrCombination_AtLeastOneMatches() {
            Predicate<TestEntity> predicate = EntityPaginationHelper.entityFiltersPredicate(
                    List.of("name:Test", "name:Other", "name:Third"),
                    filter -> EntityPaginationHelper.buildPredicate(filter, FIELD_EXTRACTORS)
            );

            // Should pass if ANY filter matches
            TestEntity entity1 = new TestEntity("Test", "TypeA", "Cat1");
            TestEntity entity2 = new TestEntity("Other", "TypeA", "Cat1");
            TestEntity entity3 = new TestEntity("Third", "TypeA", "Cat1");
            TestEntity entity4 = new TestEntity("NoMatch", "TypeA", "Cat1");

            assertThat(predicate.test(entity1)).isTrue();
            assertThat(predicate.test(entity2)).isTrue();
            assertThat(predicate.test(entity3)).isTrue();
            assertThat(predicate.test(entity4)).isFalse();
        }

        @Test
        void testMultipleFilters_AllFail() {
            Predicate<TestEntity> predicate = EntityPaginationHelper.entityFiltersPredicate(
                    List.of("name:NonExistent1", "name:NonExistent2", "name:NonExistent3"),
                    filter -> EntityPaginationHelper.buildPredicate(filter, FIELD_EXTRACTORS)
            );

            TestEntity entity = new TestEntity("Test", "TypeA", "Cat1");
            assertThat(predicate.test(entity)).isFalse();
        }
    }
}
