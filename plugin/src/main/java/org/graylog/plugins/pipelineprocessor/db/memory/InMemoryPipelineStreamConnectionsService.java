package org.graylog.plugins.pipelineprocessor.db.memory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.database.NotFoundException;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryPipelineStreamConnectionsService implements PipelineStreamConnectionsService {

    // poor man's id generator
    private AtomicLong idGen = new AtomicLong(0);

    private Map<String, PipelineConnections> store = new MapMaker().makeMap();

    @Override
    public PipelineConnections save(PipelineConnections connections) {
        PipelineConnections toSave = connections.id() != null
                ? connections
                : connections.toBuilder().id(createId()).build();
        store.put(toSave.id(), toSave);

        return toSave;
    }

    @Override
    public PipelineConnections load(String streamId) throws NotFoundException {
        final PipelineConnections connections = store.get(streamId);
        if (connections == null) {
            throw new NotFoundException("No such pipeline connections for stream " + streamId);
        }
        return connections;
    }

    @Override
    public Set<PipelineConnections> loadAll() {
        return ImmutableSet.copyOf(store.values());
    }

    @Override
    public void delete(String streamId) {
        try {
            final PipelineConnections connections = load(streamId);
            store.remove(connections.id());
        } catch (NotFoundException e) {
            // Do nothing
        }
    }

    private String createId() {
        return String.valueOf(idGen.incrementAndGet());
    }
}
