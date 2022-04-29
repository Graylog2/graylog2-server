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
package org.graylog2.indexer.fieldtypes.streamfiltered.filters;

import com.google.common.collect.ImmutableSet;
import org.graylog2.indexer.fieldtypes.FieldTypeDTO;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;

class AllowAllStreamBasedFieldTypeFilterTest {


    @Test
    void returnsUnchangedFields() {
        AllowAllStreamBasedFieldTypeFilter toTest = new AllowAllStreamBasedFieldTypeFilter();
        final ImmutableSet<FieldTypeDTO> fields = ImmutableSet.of(
                FieldTypeDTO.create("count", "long"),
                FieldTypeDTO.create("message", "text")
        );
        final Set<FieldTypeDTO> filteredFields = toTest.filterFieldTypes(fields, Collections.emptySet(), Collections.emptySet());

        assertSame(fields, filteredFields);
    }
}
