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
package org.graylog2.rest.models.tools.responses;

import com.google.common.collect.ImmutableList;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageListResponseTest {

    @Test
    void createsProperResponse() {
        PageListResponse<String> pageListResponse = PageListResponse.create("gimme me data!",
                new PaginatedList<>(
                        ImmutableList.of("1", "2"),
                        500, 1, 5),
                "whatever", "asc",
                List.of(EntityAttribute.builder().title("some_attr").id("some_id").build()),
                EntityDefaults.builder().sort(Sorting.create("some_id", Sorting.Direction.ASC)).build());

        assertThat(pageListResponse.total()).isEqualTo(500);
        assertThat(pageListResponse.paginationInfo().page()).isEqualTo(1);
        assertThat(pageListResponse.paginationInfo().total()).isEqualTo(500);
        assertThat(pageListResponse.paginationInfo().perPage()).isEqualTo(5);
        assertThat(pageListResponse.sort()).isEqualTo("whatever");
        assertThat(pageListResponse.order()).isEqualTo(SortOrder.ASCENDING);
        assertTrue(pageListResponse.elements().containsAll(ImmutableList.of("1", "2")));
        assertEquals(pageListResponse.attributes(), List.of(EntityAttribute.builder().title("some_attr").id("some_id").build()));
        assertEquals(pageListResponse.defaults(), EntityDefaults.builder().sort(Sorting.create("some_id", Sorting.Direction.ASC)).build());
    }

}
