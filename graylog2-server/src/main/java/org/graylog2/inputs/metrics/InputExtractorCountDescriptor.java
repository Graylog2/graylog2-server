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

import jakarta.inject.Inject;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.inputs.InputService;
import org.graylog2.metrics.entity.EntityMetric;
import org.graylog2.metrics.entity.EntityUncachedMetricDescriptor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class InputExtractorCountDescriptor implements EntityUncachedMetricDescriptor<Integer> {

    public static final String FIELD_NAME = "extractor_count";

    private final InputService inputService;

    @Inject
    public InputExtractorCountDescriptor(InputService inputService) {
        this.inputService = inputService;
    }

    @Override
    public String fieldName() {
        return FIELD_NAME;
    }

    @Override
    public List<EntityMetric<Integer>> compute(Collection<String> entityIds, SearchUser searchUser) {
        final Map<String, Integer> counts = inputService.extractorCountByInputId(entityIds);
        return entityIds.stream()
                .map(id -> new EntityMetric<>(id, counts.getOrDefault(id, 0)))
                .toList();
    }
}
