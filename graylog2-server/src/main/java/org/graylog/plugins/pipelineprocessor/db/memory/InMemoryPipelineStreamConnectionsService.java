/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.db.memory;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryPipelineStreamConnectionsService implements PipelineStreamConnectionsService {
    // poor man's id generator
    private final AtomicLong idGen = new AtomicLong(0);

    private final Map<String, PipelineConnections> store = new ConcurrentHashMap<>();
    private final ClusterEventBus clusterBus;

    @Inject
    public InMemoryPipelineStreamConnectionsService(ClusterEventBus clusterBus) {
        this.clusterBus = clusterBus;
    }

    @Override
    public PipelineConnections save(PipelineConnections connections) {
        PipelineConnections toSave = connections.id() != null
                ? connections
                : connections.toBuilder().id(createId()).build();
        store.put(toSave.id(), toSave);
        clusterBus.post(PipelineConnectionsChangedEvent.create(toSave.streamId(), toSave.pipelineIds()));

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
    public Set<PipelineConnections> loadByPipelineId(String pipelineId) {
        return store.values().stream()
                .filter(connection -> connection.pipelineIds().contains(pipelineId))
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(String streamId) {
        try {
            final PipelineConnections connections = load(streamId);
            final Set<String> pipelineIds = connections.pipelineIds();

            store.remove(connections.id());
            clusterBus.post(PipelineConnectionsChangedEvent.create(streamId, pipelineIds));
        } catch (NotFoundException e) {
            // Do nothing
        }
    }

    private String createId() {
        return String.valueOf(idGen.incrementAndGet());
    }
}
