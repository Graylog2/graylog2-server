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
package org.graylog.plugins.pipelineprocessor.db.memory;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A PipelineService that does not persist any data, but simply keeps it in memory.
 */
public class InMemoryPipelineService implements PipelineService {
    // poor man's id generator
    private final AtomicLong idGen = new AtomicLong(0);

    private final Map<String, PipelineDao> store = new ConcurrentHashMap<>();
    private final Map<String, String> titleToId = new ConcurrentHashMap<>();

    private final ClusterEventBus clusterBus;

    @Inject
    public InMemoryPipelineService(ClusterEventBus clusterBus) {
        this.clusterBus = clusterBus;
    }

    @Override
    public PipelineDao save(PipelineDao pipeline) {
        PipelineDao toSave = pipeline.id() != null
                ? pipeline
                : pipeline.toBuilder().id(createId()).build();
        // enforce the title unique constraint
        if (titleToId.containsKey(toSave.title())) {
            // if this is an update and the title belongs to the passed pipeline, then it's fine
            if (!titleToId.get(toSave.title()).equals(toSave.id())) {
                throw new IllegalArgumentException("Duplicate pipeline titles are not allowed: " + toSave.title());
            }
        }
        titleToId.put(toSave.title(), toSave.id());
        store.put(toSave.id(), toSave);

        clusterBus.post(PipelinesChangedEvent.updatedPipelineId(toSave.id()));

        return toSave;
    }

    @Override
    public PipelineDao load(String id) throws NotFoundException {
        final PipelineDao pipeline = store.get(id);
        if (pipeline == null) {
            throw new NotFoundException("No such pipeline with id " + id);
        }
        return pipeline;
    }

    @Override
    public PipelineDao loadByName(String name) throws NotFoundException {
        final String id = titleToId.get(name);
        if (id == null) {
            throw new NotFoundException("No pipeline with name " + name);
        }
        return load(id);
    }

    @Override
    public Collection<PipelineDao> loadAll() {
        return ImmutableSet.copyOf(store.values());
    }

    @Override
    public void delete(String id) {
        if (id == null) {
            return;
        }
        final PipelineDao removed = store.remove(id);
        // clean up title index if the pipeline existed
        if (removed != null) {
            titleToId.remove(removed.title());
        }

        clusterBus.post(PipelinesChangedEvent.deletedPipelineId(id));
    }

    private String createId() {
        return String.valueOf(idGen.incrementAndGet());
    }
}
