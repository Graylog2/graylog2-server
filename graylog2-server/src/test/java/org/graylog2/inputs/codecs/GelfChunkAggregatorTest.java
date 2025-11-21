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
package org.graylog2.inputs.codecs;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.inputs.codecs.GelfChunkAggregator.CHUNK_COUNTER;
import static org.graylog2.inputs.codecs.GelfChunkAggregator.COMPLETE_MESSAGES;
import static org.graylog2.inputs.codecs.GelfChunkAggregator.ChunkEntry;
import static org.graylog2.inputs.codecs.GelfChunkAggregator.DUPLICATE_CHUNKS;
import static org.graylog2.inputs.codecs.GelfChunkAggregator.EXPIRED_CHUNKS;
import static org.graylog2.inputs.codecs.GelfChunkAggregator.EXPIRED_MESSAGES;
import static org.graylog2.inputs.codecs.GelfChunkAggregator.WAITING_MESSAGES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class GelfChunkAggregatorTest {
    private static final byte[] CHUNK_MAGIC_BYTES = new byte[]{0x1e, 0x0f};

    private ScheduledThreadPoolExecutor poolExecutor;
    private GelfChunkAggregator aggregator;
    private MetricRegistry metricRegistry;

    @BeforeEach
    public void before() {
        poolExecutor = new ScheduledThreadPoolExecutor(1);
        metricRegistry = new MetricRegistry();
        aggregator = new GelfChunkAggregator(poolExecutor, metricRegistry);
    }

    @AfterEach
    public void after() {
        poolExecutor.shutdown();
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void addSingleChunk() {
        final ByteBuf[] singleChunk = createChunkedMessage(512, 1024);

        final CodecAggregator.Result result = aggregator.addChunk(singleChunk[0]);

        assertNotNull(result.getMessage(), "message should be complete");

        assertEquals(1, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
        assertEquals(1, counterValueNamed(metricRegistry, CHUNK_COUNTER));
        assertEquals(0, counterValueNamed(metricRegistry, WAITING_MESSAGES));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));
        assertEquals(0, counterValueNamed(metricRegistry, DUPLICATE_CHUNKS));
    }

    @Test
    public void manyChunks() {
        final ByteBuf[] chunks = createChunkedMessage(4096 + 512, 1024); // creates 5 chunks

        int i = 0;
        for (final ByteBuf chunk : chunks) {
            i++;
            final CodecAggregator.Result result = aggregator.addChunk(chunk);
            assertTrue(result.isValid());
            if (i == 5) {
                assertNotNull(result.getMessage(), "message should've been assembled from chunks");

                assertEquals(1, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
                assertEquals(5, counterValueNamed(metricRegistry, CHUNK_COUNTER));
                assertEquals(0, counterValueNamed(metricRegistry, WAITING_MESSAGES));
                assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
                assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));
                assertEquals(0, counterValueNamed(metricRegistry, DUPLICATE_CHUNKS));

            } else {
                assertNull(result.getMessage(), "chunks not complete");

                assertEquals(0, counterValueNamed(metricRegistry, COMPLETE_MESSAGES), "message not complete yet");
                assertEquals(i, counterValueNamed(metricRegistry, CHUNK_COUNTER));
                assertEquals(1, counterValueNamed(metricRegistry, WAITING_MESSAGES), "one message waiting");
                assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
                assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));
                assertEquals(0, counterValueNamed(metricRegistry, DUPLICATE_CHUNKS));
            }
        }
    }

    @Test
    public void tooManyChunks() {
        final ByteBuf[] chunks = createChunkedMessage(129 * 1024, 1024);
        int i = 1;
        for (final ByteBuf chunk : chunks) {
            final CodecAggregator.Result result = aggregator.addChunk(chunk);
            if (i == 129) {
                assertFalse(result.isValid(), "Message invalidated (chunk #" + i + ")");
                assertNull(result.getMessage(), "Message discarded (chunk #" + i + ")");

            } else {
                assertTrue(result.isValid(), "Incomplete message valid (chunk #" + i + ")");
                assertNull(result.getMessage(), "Message not complete (chunk #" + i + ")");
            }
            i++;
        }
    }

    @Test
    public void missingChunk() {
        final DateTime initialTime = new DateTime(2014, 1, 1, 1, 59, 59, 0, DateTimeZone.UTC);
        final InstantMillisProvider clock = new InstantMillisProvider(initialTime);
        DateTimeUtils.setCurrentMillisProvider(clock);

        // we don't want the clean up task to run automatically
        poolExecutor = mock(ScheduledThreadPoolExecutor.class);
        final MetricRegistry metricRegistry = new MetricRegistry();
        aggregator = new GelfChunkAggregator(poolExecutor, metricRegistry);
        final GelfChunkAggregator.ChunkEvictionTask evictionTask = aggregator.new ChunkEvictionTask();

        final ByteBuf[] chunks = createChunkedMessage(4096 + 512, 1024); // creates 5 chunks

        int i = 0;
        for (final ByteBuf chunk : chunks) {
            final CodecAggregator.Result result;
            // skip first chunk
            if (i++ == 0) {
                continue;
            }
            result = aggregator.addChunk(chunk);
            assertTrue(result.isValid());
            assertNull(result.getMessage(), "chunks not complete");
        }
        // move clock forward enough to evict all of the chunks
        clock.tick(Period.seconds(10));

        evictionTask.run();

        final CodecAggregator.Result result = aggregator.addChunk(chunks[0]);

        assertNull(result.getMessage(), "message should not be complete because chunks were evicted already");
        assertTrue(result.isValid());

        // we send all chunks but the last one comes too late
        assertEquals(0, counterValueNamed(metricRegistry, COMPLETE_MESSAGES), "no message is complete");
        assertEquals(5, counterValueNamed(metricRegistry, CHUNK_COUNTER), "received 5 chunks");
        assertEquals(1, counterValueNamed(metricRegistry, WAITING_MESSAGES), "last chunk creates another waiting message");
        assertEquals(4, counterValueNamed(metricRegistry, EXPIRED_CHUNKS), "4 chunks expired");
        assertEquals(1, counterValueNamed(metricRegistry, EXPIRED_MESSAGES), "one message expired");
        assertEquals(0, counterValueNamed(metricRegistry, DUPLICATE_CHUNKS), "no duplicate chunks");

        // reset clock for other tests
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void outOfOrderChunks() {
        final ByteBuf[] chunks = createChunkedMessage(4096 + 512, 1024); // creates 5 chunks
        CodecAggregator.Result result = null;
        for (int i = chunks.length - 1; i >= 0; i--) {
            result = aggregator.addChunk(chunks[i]);
            if (i != 0) {
                assertNull(result.getMessage(), "message still incomplete");
            }
        }
        assertNotNull(result);
        assertNotNull(result.getMessage(), "first chunk should've completed the message");
        assertEquals(1, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
        assertEquals(5, counterValueNamed(metricRegistry, CHUNK_COUNTER));
        assertEquals(0, counterValueNamed(metricRegistry, WAITING_MESSAGES));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));
        assertEquals(0, counterValueNamed(metricRegistry, DUPLICATE_CHUNKS));
    }

    @Test
    public void differentIdsDoNotInterfere() {
        final ByteBuf[] msg1 = createChunkedMessage(4096 + 1, 1024, generateMessageId(1));// 5 chunks;
        final ByteBuf[] msg2 = createChunkedMessage(4096 + 1, 1024, generateMessageId(2));// 5 chunks;

        CodecAggregator.Result result1 = null;
        CodecAggregator.Result result2 = null;
        for (int i = 0; i < msg1.length; i++) {
            result1 = aggregator.addChunk(msg1[i]);
            if (i > 0) {
                result2 = aggregator.addChunk(msg2[i]);
            }
        }
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result1.getMessage(), "message 1 should be complete");
        assertNull(result2.getMessage(), "message 2 should not be complete");
        // only one is complete, we sent 9 chunks
        assertEquals(1, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
        assertEquals(9, counterValueNamed(metricRegistry, CHUNK_COUNTER));
        assertEquals(1, counterValueNamed(metricRegistry, WAITING_MESSAGES));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));
        assertEquals(0, counterValueNamed(metricRegistry, DUPLICATE_CHUNKS));
    }

    @Test
    public void duplicateChunk() {
        final byte[] messageId1 = generateMessageId(1);
        final byte[] messageId2 = generateMessageId(2);
        final ByteBuf chunk1 = createChunk(messageId1, (byte) 0, (byte) 2, new byte[16]);
        final ByteBuf chunk2 = createChunk(messageId1, (byte) 0, (byte) 2, new byte[16]);
        final ByteBuf chunk3 = createChunk(messageId2, (byte) 0, (byte) 2, new byte[16]);
        final ByteBuf chunk4 = createChunk(messageId1, (byte) 1, (byte) 2, new byte[16]);
        final ByteBuf chunk5 = createChunk(messageId2, (byte) 1, (byte) 2, new byte[16]);

        assertNull(aggregator.addChunk(chunk1).getMessage(), "message should not be complete");
        assertNull(aggregator.addChunk(chunk2).getMessage(), "message should not be complete");
        assertNull(aggregator.addChunk(chunk3).getMessage(), "message should not be complete");
        assertNotNull(aggregator.addChunk(chunk4).getMessage(), "message 1 should be complete");
        assertNotNull(aggregator.addChunk(chunk5).getMessage(), "message 2 should be complete");

        assertEquals(2, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
        assertEquals(5, counterValueNamed(metricRegistry, CHUNK_COUNTER));
        assertEquals(0, counterValueNamed(metricRegistry, WAITING_MESSAGES));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));
        assertEquals(1, counterValueNamed(metricRegistry, DUPLICATE_CHUNKS));
    }

    @Test
    public void testChunkEntryCompareTo() throws Exception {
        // Test if the ChunkEntry#compareTo() method can handle ChunkEntry objects which have the same timestamp.
        // See: https://github.com/Graylog2/graylog2-server/issues/1462

        final ConcurrentSkipListSet<GelfChunkAggregator.ChunkEntry> sortedEvictionSet = new ConcurrentSkipListSet<>();
        final long currentTime = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            sortedEvictionSet.add(new GelfChunkAggregator.ChunkEntry(1, currentTime, "a" + i));
        }

        final int size = sortedEvictionSet.size();

        for (int i = 0; i < size; i++) {
            sortedEvictionSet.remove(sortedEvictionSet.first());
        }

        assertTrue(sortedEvictionSet.isEmpty(), "eviction set should be empty");
    }

    @Test
    public void testChunkEntryEquals() throws Exception {
        final GelfChunkAggregator.ChunkEntry entry = new ChunkEntry(1, 0L, "id");

        assertThat(entry).isEqualTo(new ChunkEntry(1, 0L, "id"));
        assertThat(entry).isEqualTo(new ChunkEntry(2, 0L, "id"));
        assertThat(entry).isNotEqualTo(new ChunkEntry(1, 1L, "id"));
        assertThat(entry).isNotEqualTo(new ChunkEntry(1, 0L, "foo"));
    }

    @Test
    public void testChunkEntryHashCode() throws Exception {
        final GelfChunkAggregator.ChunkEntry entry = new ChunkEntry(1, 0L, "id");

        assertThat(entry.hashCode()).isEqualTo(new ChunkEntry(1, 0L, "id").hashCode());
        assertThat(entry.hashCode()).isEqualTo(new ChunkEntry(2, 0L, "id").hashCode());
        assertThat(entry.hashCode()).isNotEqualTo(new ChunkEntry(1, 1L, "id").hashCode());
        assertThat(entry.hashCode()).isNotEqualTo(new ChunkEntry(1, 0L, "foo").hashCode());
    }

    private ByteBuf[] createChunkedMessage(int messageSize, int maxChunkSize) {
        return createChunkedMessage(messageSize, maxChunkSize, generateMessageId());
    }

    private ByteBuf[] createChunkedMessage(int messageSize, int maxChunkSize, byte[] messageId) {
        // partially copied from GelfClient (can't use here, because it uses netty4)

        int sequenceCount = (messageSize / maxChunkSize);

        // Check if we have to add another chunk due to integer division.
        if ((messageSize % maxChunkSize) != 0) {
            sequenceCount++;
        }

        final ByteBuf[] buffers = new ByteBuf[sequenceCount];
        for (int sequenceNumber = 0; sequenceNumber < sequenceCount; sequenceNumber++) {
            // fake payload, we don't care about actually parsing it in this test
            int payloadSize = maxChunkSize;
            // correctly size the last chunk
            if (sequenceNumber + 1 == sequenceCount) {
                payloadSize = (messageSize % maxChunkSize);
            }

            buffers[sequenceNumber] = createChunk(messageId, (byte) sequenceNumber, (byte) sequenceCount, new byte[payloadSize]);
        }

        return buffers;
    }

    private ByteBuf createChunk(byte[] messageId, byte sequenceNumber, byte sequenceCount, byte[] payload) {
        final ByteBuf channelBuffer = Unpooled.buffer(payload.length + 12);

        channelBuffer.writeBytes(CHUNK_MAGIC_BYTES);
        channelBuffer.writeBytes(messageId);
        channelBuffer.writeByte(sequenceNumber);
        channelBuffer.writeByte(sequenceCount);
        channelBuffer.writeBytes(payload);

        return channelBuffer;
    }

    private byte[] generateMessageId(int id) {
        final ByteBuf messageId = Unpooled.buffer(8);

        // 4 bytes of current time.
        messageId.writeInt((int) System.currentTimeMillis());
        messageId.writeInt(id);

        return messageId.array();
    }

    private byte[] generateMessageId() {
        return generateMessageId(0);
    }

    public static long counterValueNamed(MetricRegistry metricRegistry, String name) {
        return metricRegistry.getCounters(new SingleNameMatcher(name)).get(name).getCount();
    }

    private static class SingleNameMatcher implements MetricFilter {
        private final String metricName;

        public SingleNameMatcher(String metricName) {
            this.metricName = metricName;
        }

        @Override
        public boolean matches(String name, Metric metric) {
            return metricName.equals(name);
        }
    }
}
