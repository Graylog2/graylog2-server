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

import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.database.NotFoundException;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface PipelineStreamConnectionsService {
    PipelineConnections save(PipelineConnections connections);

    PipelineConnections load(String streamId) throws NotFoundException;

    Set<PipelineConnections> loadAll();

    Set<PipelineConnections> loadByPipelineId(String pipelineId);

    void delete(String streamId);

    /**
     * Removes the given pipeline ID from all stream connection documents.
     * Deletes a connection document when its {@code pipeline_ids} list becomes empty.
     * <p>
     * Note: the default routing pipeline ("Default Routing") is not deletable, so its
     * connection to the default stream is never removed via this method.
     */
    void deleteConnectionsForPipeline(String pipelineId);

    Map<String, PipelineConnections> loadByStreamIds(Collection<String> streamIds);
}
