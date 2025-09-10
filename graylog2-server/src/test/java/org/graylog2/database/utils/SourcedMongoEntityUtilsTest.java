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
package org.graylog2.database.utils;

import org.graylog2.database.BuildableMongoEntity;
import org.graylog2.database.entities.SourcedMongoEntity;
import org.graylog2.database.entities.source.EntitySource;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.database.utils.SourcedMongoEntityUtils.FILTERABLE_FIELD;

class SourcedMongoEntityUtilsTest {

    protected static final String ILLUMINATE_SOURCE = "ILLUMINATE";
    protected static final String USER_DEFINED_SOURCE = "USER_DEFINED";

    @Test
    void testEmptyFilters() {
        Predicate<TestEntity> basePredicate = e -> true;
        var result = SourcedMongoEntityUtils.handleEntitySourceFilter(List.of(), basePredicate);
        assertThat(result.filters()).isEmpty();
        assertThat(result.predicate().test(new TestEntity(newSource(ILLUMINATE_SOURCE)))).isTrue();
    }

    @Test
    void testSingleFilterIlluminate() {
        String filter = FILTERABLE_FIELD + ":ILLUMINATE";
        Predicate<TestEntity> basePredicate = e -> true;
        var result = SourcedMongoEntityUtils.handleEntitySourceFilter(List.of(filter), basePredicate);

        assertThat(result.filters()).isEmpty();
        assertThat(result.predicate().test(new TestEntity(newSource(ILLUMINATE_SOURCE)))).isTrue();
        assertThat(result.predicate().test(new TestEntity(newSource(USER_DEFINED_SOURCE)))).isFalse();
    }

    @Test
    void testSingleFilterUserDefined() {
        String filter = FILTERABLE_FIELD + ":USER_DEFINED";
        Predicate<TestEntity> basePredicate = e -> true;
        var result = SourcedMongoEntityUtils.handleEntitySourceFilter(List.of(filter), basePredicate);

        assertThat(result.filters()).isEmpty();
        assertThat(result.predicate().test(new TestEntity(newSource(USER_DEFINED_SOURCE)))).isTrue();
        assertThat(result.predicate().test(new TestEntity(newSource(ILLUMINATE_SOURCE)))).isFalse();
        assertThat(result.predicate().test(new TestEntity(null))).isTrue(); // null treated as USER_DEFINED
    }

    @Test
    void testMultipleFilters_shouldUseOr() {
        String filter1 = FILTERABLE_FIELD + ":ILLUMINATE";
        String filter2 = FILTERABLE_FIELD + ":USER_DEFINED";
        Predicate<TestEntity> basePredicate = e -> true;
        var result = SourcedMongoEntityUtils.handleEntitySourceFilter(List.of(filter1, filter2), basePredicate);

        assertThat(result.filters()).isEmpty();
        assertThat(result.predicate().test(new TestEntity(newSource(ILLUMINATE_SOURCE)))).isTrue();
        assertThat(result.predicate().test(new TestEntity(newSource(USER_DEFINED_SOURCE)))).isTrue();
        assertThat(result.predicate().test(new TestEntity(newSource("OTHER")))).isFalse();
    }

    @Test
    void testRespectsExistingPredicate() {
        String filter = FILTERABLE_FIELD + ":ILLUMINATE";
        Predicate<TestEntity> basePredicate = e -> e.entitySource().isPresent();
        var result = SourcedMongoEntityUtils.handleEntitySourceFilter(List.of(filter), basePredicate);

        assertThat(result.filters()).isEmpty();
        assertThat(result.predicate().test(new TestEntity(newSource(ILLUMINATE_SOURCE)))).isTrue();
        assertThat(result.predicate().test(new TestEntity(null))).isFalse();
    }

    private EntitySource newSource(String source) {
        return EntitySource.builder()
                .entityId("id")
                .source(source)
                .entityType("view")
                .build();
    }

    static class TestEntity implements SourcedMongoEntity {
        private final EntitySource entitySource;

        TestEntity(EntitySource entitySource) {
            this.entitySource = entitySource;
        }

        @Override
        public Optional<EntitySource> entitySource() {
            return Optional.ofNullable(entitySource);
        }

        @Override
        public BuildableMongoEntity.Builder toBuilder() {
            return null;
        }

        @Nullable
        @Override
        public String id() {
            return "id";
        }
    }
}
