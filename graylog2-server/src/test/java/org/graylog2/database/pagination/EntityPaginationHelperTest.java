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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
                descriptor -> "example",
                descriptor -> "Example Title"
        );

        assertThat(predicate.test(mock(GRNDescriptor.class))).isEqualTo(expected);
    }
}
