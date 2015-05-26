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
package org.graylog2.inputs.codecs;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.graylog2.inputs.codecs.gelf.GELFMessage;
import org.graylog2.inputs.codecs.gelf.GELFMessageChunk;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static com.codahale.metrics.MetricRegistry.name;

public class GelfChunkAggregator implements CodecAggregator {
    private static final Logger log = LoggerFactory.getLogger(GelfChunkAggregator.class);

    public static final Result VALID_EMPTY_RESULT = new Result(null, true);
    public static final Result INVALID_RESULT = new Result(null, false);
    public static final int VALIDITY_PERIOD = 5000; // millis
    private static final long CHECK_PERIOD = 1000;

    public static final String CHUNK_COUNTER = name(GelfChunkAggregator.class, "total-chunks");
    public static final String WAITING_MESSAGES = name(GelfChunkAggregator.class, "waiting-messages");
    public static final String COMPLETE_MESSAGES = name(GelfChunkAggregator.class, "complete-messages");
    public static final String EXPIRED_MESSAGES = name(GelfChunkAggregator.class, "expired-messages");
    public static final String EXPIRED_CHUNKS = name(GelfChunkAggregator.class, "expired-chunks");

    private final ConcurrentMap<String, ChunkEntry> chunks = Maps.newConcurrentMap();
    private final ConcurrentSkipListSet<ChunkEntry> sortedEvictionSet = new ConcurrentSkipListSet<>();
    private final Counter chunkCounter;
    private final Counter waitingMessages;
    private final Counter expiredMessages;
    private final Counter expiredChunks;
    private final Counter completeMessages;

    @Inject
    public GelfChunkAggregator(@Named("daemonScheduler") ScheduledExecutorService scheduler, MetricRegistry metricRegistry) {
        scheduler.scheduleAtFixedRate(new ChunkEvictionTask(), VALIDITY_PERIOD, CHECK_PERIOD, TimeUnit.MILLISECONDS);
        chunkCounter = metricRegistry.counter(CHUNK_COUNTER);
        // this is a counter instead of a Gauge, because calling sortedEvictionSet.size() is expensive
        waitingMessages = metricRegistry.counter(WAITING_MESSAGES);
        completeMessages = metricRegistry.counter(COMPLETE_MESSAGES);
        expiredMessages = metricRegistry.counter(EXPIRED_MESSAGES);
        expiredChunks = metricRegistry.counter(EXPIRED_CHUNKS);
    }

    @Nonnull
    @Override
    public Result addChunk(ChannelBuffer buffer) {
        final byte[] readable = new byte[buffer.readableBytes()];
        buffer.toByteBuffer().get(readable, buffer.readerIndex(), buffer.readableBytes());

        final GELFMessage msg = new GELFMessage(readable);

        final ChannelBuffer aggregatedBuffer;
        switch (msg.getGELFType()) {
            case CHUNKED:
                try {
                    chunkCounter.inc();
                    aggregatedBuffer = checkForCompletion(msg);
                    if (aggregatedBuffer == null) {
                        return VALID_EMPTY_RESULT;
                    }
                } catch (IllegalArgumentException e) {
                    log.debug("Invalid gelf message chunk, dropping message.", e);
                    return INVALID_RESULT;
                }
                break;
            case ZLIB:
            case GZIP:
            case UNCOMPRESSED:
                aggregatedBuffer = buffer;
                break;
            case UNSUPPORTED:
                return INVALID_RESULT;
            default:
                return INVALID_RESULT;
        }
        return new Result(aggregatedBuffer, true);
    }

    /**
     * Checks whether the presented gelf message chunk completes the incoming raw message and returns it if it does.
     * If the message isn't complete, it adds the chunk to the internal buffer and waits for more incoming messages.
     * Outdated chunks are being purged regularly.
     *
     * @param gelfMessage   the gelf message chunk
     * @return null or a {@link org.graylog2.plugin.journal.RawMessage raw message} object
     */
    private ChannelBuffer checkForCompletion(GELFMessage gelfMessage) {
        if (!chunks.isEmpty() && log.isDebugEnabled()) {
            log.debug("Dumping GELF chunk map [chunks for {} messages]:\n{}", chunks.size(), humanReadableChunkMap());
        }
        final GELFMessageChunk chunk = new GELFMessageChunk(gelfMessage, null); // TODO second parameter
        final int sequenceCount = chunk.getSequenceCount();

        final String messageId = chunk.getId();

        ChunkEntry entry = new ChunkEntry(sequenceCount, chunk.getArrival(), messageId);
        final ChunkEntry existing = chunks.putIfAbsent(messageId, entry);
        if (existing == null) {
            // add this chunk entry to the eviction set
            waitingMessages.inc();
            sortedEvictionSet.add(entry);
        } else {
            // the entry is already in the eviction set and chunk map
            entry = existing;
        }

        final int chunkWatermark = entry.chunkSlotsWritten.incrementAndGet();
        entry.payloadArray.set(chunk.getSequenceNumber(), chunk);

        if (chunkWatermark == sequenceCount) {
            // message is complete by chunk count, assemble and return it.
            // it might still be corrupt etc, but we've seen enough chunks
            // remove before operating on it, to avoid racing too much with the clean up job, some race is inevitable, though.
            entry = getAndCleanupEntry(messageId);

            final byte[] allChunks[] = new byte[sequenceCount][];
            for (int i = 0; i < entry.payloadArray.length(); i++) {
                final GELFMessageChunk messageChunk = entry.payloadArray.get(i);
                allChunks[i] = messageChunk.getData();

            }
            completeMessages.inc();
            return ChannelBuffers.wrappedBuffer(allChunks);
        }

        // message isn't complete yet, check if we should remove the other parts as well
        if (isOutdated(entry)) {
            // chunks are outdated, the oldest came in over 5 seconds ago, clean them all up
            log.debug("Not all chunks of <{}> arrived within {}ms. Dropping chunks.", messageId, VALIDITY_PERIOD);
            expireEntry(messageId);
        }
        return null;
    }

    private void expireEntry(String messageId) {
        final ChunkEntry cleanupEntry = getAndCleanupEntry(messageId);
        expiredMessages.inc();
        expiredChunks.inc(cleanupEntry.chunkSlotsWritten.get());
    }

    private boolean isOutdated(ChunkEntry entry) {
        return (Tools.iso8601().getMillis() - entry.firstTimestamp) > VALIDITY_PERIOD;
    }

    private ChunkEntry getAndCleanupEntry(String id) {
        final ChunkEntry entry = chunks.remove(id);
        sortedEvictionSet.remove(entry);
        waitingMessages.dec();
        return entry;
    }

    private String humanReadableChunkMap() {
        final StringBuilder sb = new StringBuilder();

        for (final Map.Entry<String, ChunkEntry> entry : chunks.entrySet()) {
            sb.append("Message <").append(entry.getKey()).append("> ");
            sb.append("\tChunks:\n");
            for (int i = 0; i < entry.getValue().payloadArray.length(); i++) {
                final GELFMessageChunk chunk = entry.getValue().payloadArray.get(i);
                sb.append("\t\t").append(chunk == null ? "<not arrived yet>" : chunk).append(("\n"));
            }
        }

        return sb.toString();
    }

    private static class ChunkEntry implements Comparable<ChunkEntry> {
        private final AtomicInteger chunkSlotsWritten = new AtomicInteger(0);
        private final long firstTimestamp;
        private final AtomicReferenceArray<GELFMessageChunk> payloadArray;
        private final String id;

        private ChunkEntry(int chunkCount, long firstTimestamp, String id) {
            this.payloadArray = new AtomicReferenceArray<>(chunkCount);
            this.firstTimestamp = firstTimestamp;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ChunkEntry that = (ChunkEntry) o;

            if (firstTimestamp != that.firstTimestamp) return false;
            if (!chunkSlotsWritten.equals(that.chunkSlotsWritten)) return false;
            //noinspection RedundantIfStatement
            if (!payloadArray.equals(that.payloadArray)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = chunkSlotsWritten.hashCode();
            result = 31 * result + (int) (firstTimestamp ^ (firstTimestamp >>> 32));
            result = 31 * result + payloadArray.hashCode();
            return result;
        }

        @Override
        public int compareTo(@Nonnull ChunkEntry o) {
            if (equals(o)) {
                return 0;
            }
            return firstTimestamp < o.firstTimestamp ? -1 : 1;
        }
    }

    @VisibleForTesting
    class ChunkEvictionTask implements Runnable {
        @Override
        public void run() {
            try {
                // loop until we've either evicted all outdated chunk entries, or the set is completely empty.
                // this task will run every second by default (see constant in constructor)
                while (true) {
                    final ChunkEntry oldestChunkEntry = sortedEvictionSet.first();
                    if (isOutdated(oldestChunkEntry)) {
                        expireEntry(oldestChunkEntry.id);
                    } else {
                        log.debug("No more outdated chunk entries found to evict, leaving cleanup loop.");
                        break;
                    }
                }
            } catch (Exception ignored) {
                // set empty, nothing more to do. make sure to never throw an exception out of this runnable, it's
                // being run in an executor
                log.debug("Eviction set empty, nothing more to do.");
            }
        }
    }
}
