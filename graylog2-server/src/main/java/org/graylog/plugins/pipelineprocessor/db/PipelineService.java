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

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface PipelineService {
    PipelineDao save(PipelineDao pipeline);

    PipelineDao load(String id) throws NotFoundException;

    PipelineDao loadByName(String name) throws NotFoundException;

    /**
     * Returns a list of pipelines. Each pipeline uses at least one
     * rule from the provided set.
     */
    @NotNull
    List<PipelineDao> loadByRules(@NotNull Set<String> ruleNames);

    Collection<PipelineDao> loadAll();

    void delete(String id);
}
