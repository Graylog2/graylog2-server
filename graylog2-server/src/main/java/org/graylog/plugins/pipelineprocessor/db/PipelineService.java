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
package org.graylog.plugins.pipelineprocessor.db;

import org.graylog2.database.NotFoundException;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface PipelineService {
    default PipelineDao save(PipelineDao pipeline) {
        return save(pipeline, true);
    }

    PipelineDao save(PipelineDao pipeline, boolean checkMutability);

    PipelineDao load(String id) throws NotFoundException;

    PipelineDao loadByName(String name) throws NotFoundException;

    /**
     * Returns all pipelines with given source pattern.
     * This method is only implemented in the MongoDB implementation.
     */
    default Collection<PipelineDao> loadBySourcePattern(String sourcePattern) {
        throw new UnsupportedOperationException("loadBySourcePattern is not implemented");
    }

    Collection<PipelineDao> loadAll();

    void delete(String id);

    default Set<PipelineDao> loadByIds(Set<String> pipelineIds) {
        return pipelineIds.stream().flatMap(id -> {
            try {
                return Stream.of(load(id));
            } catch (NotFoundException e) {
                return Stream.empty();
            }
        }).collect(Collectors.toSet());
    }
}
