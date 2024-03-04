package org.graylog2.plugin;

import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.messages.IndexingResult;
import org.graylog2.indexer.messages.IndexingResultCallback;
import org.graylog2.plugin.streams.Stream;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

/**
 * A Message that is used for System purposes like restoring Archives.
 * The message has the following properties:
 * <ul>
 *  <li>A size of 0, so its traffic is not accounted</li>
 *  <li>A single predetermined IndexSet</li>
 *  <li>No streams, so it will only be routed to the {@link org.graylog2.outputs.DefaultMessageOutput}</li>
 * </ul>
 */
public class SystemMessage extends Message {
    private final IndexSet indexSet;
    private final IndexingResultCallback resultCallback;

    public SystemMessage(IndexSet indexSet, Map<String, Object> fields, @Nullable IndexingResultCallback resultCallback) {
        super(fields);
        this.indexSet = indexSet;
        this.resultCallback = resultCallback;
    }

    public void runIndexingResultCallback(IndexingResult result) {
        if (resultCallback != null) {
            resultCallback.accept(result);
        }
    }

    @Override
    public Set<IndexSet> getIndexSets() {
        return Set.of(indexSet);
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public Set<Stream> getStreams() {
        return Set.of();
    }

    @Override
    public Object getMessageQueueId() {
        return null;
    }

}
