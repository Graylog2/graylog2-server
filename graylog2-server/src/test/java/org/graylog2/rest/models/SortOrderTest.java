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
package org.graylog2.rest.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.Sorts;
import jakarta.ws.rs.BadRequestException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.rest.models.SortOrder.ASCENDING;
import static org.graylog2.rest.models.SortOrder.DESCENDING;

class SortOrderTest {

    @Test
    void fromString() {
        assertThat(SortOrder.fromString("asc"))
                .isEqualTo(SortOrder.fromString("ASC"))
                .isEqualTo(SortOrder.fromString("asC"))
                .isEqualTo(ASCENDING);

        assertThat(SortOrder.fromString("desc"))
                .isEqualTo(SortOrder.fromString("DESC"))
                .isEqualTo(SortOrder.fromString("deSc"))
                .isEqualTo(DESCENDING);

        assertThatThrownBy(() -> SortOrder.fromString("sideways"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unknown sort order");
    }

    @Test
    void toBsonSort() {
        assertThat(ASCENDING.toBsonSort("a")).isEqualTo(Sorts.ascending("a"));
        assertThat(ASCENDING.toBsonSort("a", "b", "c")).isEqualTo(Sorts.ascending("a", "b", "c"));
        assertThat(ASCENDING.toBsonSort(List.of("a", "b", "c"))).isEqualTo(Sorts.ascending(List.of("a", "b", "c")));

        assertThat(DESCENDING.toBsonSort("a")).isEqualTo(Sorts.descending("a"));
        assertThat(DESCENDING.toBsonSort("a", "b", "c")).isEqualTo(Sorts.descending("a", "b", "c"));
        assertThat(DESCENDING.toBsonSort(List.of("a", "b", "c"))).isEqualTo(Sorts.descending(List.of("a", "b", "c")));
    }

    record TestRecord(SortOrder order) {}

    @Test
    void jsonConversion() throws JsonProcessingException {
        final var objectMapper = new ObjectMapperProvider().get();

        assertThat(objectMapper.readValue("""
                        {"order":"asc"}
                """, TestRecord.class))
                .isEqualTo(new TestRecord(ASCENDING));

        assertThat(objectMapper.readValue("""
                        {"order":"dEsc"}
                """, TestRecord.class))
                .isEqualTo(new TestRecord(DESCENDING));

        assertThat(
                objectMapper.readValue(objectMapper.writeValueAsString(new TestRecord(ASCENDING)), TestRecord.class))
                .isEqualTo(new TestRecord(ASCENDING));
    }

}
