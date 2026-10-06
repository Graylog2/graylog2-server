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
package org.graylog2.inputs.metrics;

import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.inputs.InputService;
import org.graylog2.metrics.entity.EntityMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InputExtractorCountDescriptorTest {

    private InputService inputService;
    private InputExtractorCountDescriptor descriptor;

    @BeforeEach
    void setUp() {
        inputService = mock(InputService.class);
        descriptor = new InputExtractorCountDescriptor(inputService);
    }

    @Test
    void compute_returnsExtractorCounts() {
        when(inputService.extractorCountByInputId(anyCollection()))
                .thenReturn(Map.of("input-1", 3, "input-2", 0));

        final SearchUser searchUser = mock(SearchUser.class);
        final List<EntityMetric<Integer>> result = descriptor.compute(List.of("input-1", "input-2"), searchUser);

        assertThat(result).hasSize(2);
        assertThat(result).filteredOn(m -> m.entityId().equals("input-1"))
                .first().extracting(EntityMetric::value).isEqualTo(3);
        assertThat(result).filteredOn(m -> m.entityId().equals("input-2"))
                .first().extracting(EntityMetric::value).isEqualTo(0);
    }

    @Test
    void compute_returnsZeroForMissingInputs() {
        when(inputService.extractorCountByInputId(anyCollection()))
                .thenReturn(Map.of());

        final SearchUser searchUser = mock(SearchUser.class);
        final List<EntityMetric<Integer>> result = descriptor.compute(List.of("input-1"), searchUser);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().entityId()).isEqualTo("input-1");
        assertThat(result.getFirst().value()).isEqualTo(0);
    }
}
