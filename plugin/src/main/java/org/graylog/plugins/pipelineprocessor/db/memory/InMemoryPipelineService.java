package org.graylog.plugins.pipelineprocessor.db.memory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog2.database.NotFoundException;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A PipelineService that does not persist any data, but simply keeps it in memory.
 */
public class InMemoryPipelineService implements PipelineService {

    // poor man's id generator
    private AtomicLong idGen = new AtomicLong(0);

    private Map<String, PipelineDao> store = new MapMaker().makeMap();
    private Map<String, String> titleToId = new MapMaker().makeMap();

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
    }

    private String createId() {
        return String.valueOf(idGen.incrementAndGet());
    }
}
