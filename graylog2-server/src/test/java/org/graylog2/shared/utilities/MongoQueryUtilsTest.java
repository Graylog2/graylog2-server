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
package org.graylog2.shared.utilities;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.mongojack.DBQuery;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MongoQueryUtilsTest {

    @Test
    public void getQueryCombinations() {
        final Set<Set<String>> result = MongoQueryUtils.getQueryCombinations(ImmutableSet.of("A", "B", "C"));

        assertThat(result).isEqualTo(ImmutableSet.of(
                ImmutableSet.of(),
                ImmutableSet.of("A"),
                ImmutableSet.of("B"),
                ImmutableSet.of("C"),
                ImmutableSet.of("A", "B"),
                ImmutableSet.of("A", "C"),
                ImmutableSet.of("B", "C"),
                ImmutableSet.of("A", "B", "C")
        ));
    }

    @Test
    public void getArrayIsContainedQuery() {
        final DBQuery.Query query = MongoQueryUtils.getArrayIsContainedQuery("field_1",
                ImmutableSet.of("IS_LEADER", "HAS_ARCHIVE", "CONSTRAINT_XY"));

        // there is no great way to peek into a query.
        // the DBJobTriggerServiceTest covers this on a higher level
        assertThat(query.conditions()).hasSize(1);
    }
}
