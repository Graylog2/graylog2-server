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

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.graylog2.inputs.codecs.GelfChunkAggregator.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class GelfChunkAggregatorTest {
    private static final byte[] CHUNK_MAGIC_BYTES = new byte[]{0x1e, 0x0f};
    private ScheduledThreadPoolExecutor poolExecutor;
    private GelfChunkAggregator aggregator;
    private MetricRegistry metricRegistry;

    @Before
    public void before() {
        poolExecutor = new ScheduledThreadPoolExecutor(1);
        metricRegistry = new MetricRegistry();
        aggregator = new GelfChunkAggregator(poolExecutor, metricRegistry);
    }

    @After
    public void after() {
        poolExecutor.shutdown();
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void addSingleChunk() {
        final ChannelBuffer[] singleChunk = createChunkedMessage(512, 1024);

        final CodecAggregator.Result result = aggregator.addChunk(singleChunk[0]);

        assertNotNull("message should be complete", result.getMessage());

        assertEquals(1, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
        assertEquals(1, counterValueNamed(metricRegistry, CHUNK_COUNTER));
        assertEquals(0, counterValueNamed(metricRegistry, WAITING_MESSAGES));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));
    }

    @Test
    public void manyChunks() {
        final ChannelBuffer[] chunks = createChunkedMessage(4096 + 512, 1024); // creates 5 chunks

        int i = 0;
        for (final ChannelBuffer chunk : chunks) {
            i++;
            final CodecAggregator.Result result = aggregator.addChunk(chunk);
            assertTrue(result.isValid());
            if (i == 5) {
                assertNotNull("message should've been assembled from chunks", result.getMessage());

                assertEquals(1, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
                assertEquals(5, counterValueNamed(metricRegistry, CHUNK_COUNTER));
                assertEquals(0, counterValueNamed(metricRegistry, WAITING_MESSAGES));
                assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
                assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));

            } else {
                assertNull("chunks not complete", result.getMessage());

                assertEquals("message not complete yet", 0, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
                assertEquals(i, counterValueNamed(metricRegistry, CHUNK_COUNTER));
                assertEquals("one message waiting", 1, counterValueNamed(metricRegistry, WAITING_MESSAGES));
                assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
                assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));

            }
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

        final ChannelBuffer[] chunks = createChunkedMessage(4096 + 512, 1024); // creates 5 chunks

        int i = 0;
        for (final ChannelBuffer chunk : chunks) {
            final CodecAggregator.Result result;
            // skip first chunk
            if (i++ == 0) {
                continue;
            }
            result = aggregator.addChunk(chunk);
            assertTrue(result.isValid());
            assertNull("chunks not complete", result.getMessage());
        }
        // move clock forward enough to evict all of the chunks
        clock.tick(Period.seconds(10));

        evictionTask.run();

        final CodecAggregator.Result result = aggregator.addChunk(chunks[0]);

        assertNull("message should not be complete because chunks were evicted already", result.getMessage());
        assertTrue(result.isValid());

        // we send all chunks but the last one comes too late
        assertEquals("no message is complete", 0, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
        assertEquals("received 5 chunks", 5, counterValueNamed(metricRegistry, CHUNK_COUNTER));
        assertEquals("last chunk creates another waiting message", 1, counterValueNamed(metricRegistry, WAITING_MESSAGES));
        assertEquals("4 chunks expired", 4, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
        assertEquals("one message expired", 1, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));

        // reset clock for other tests
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void outOfOrderChunks() {
        final ChannelBuffer[] chunks = createChunkedMessage(4096 + 512, 1024); // creates 5 chunks
        CodecAggregator.Result result = null;
        for (int i = chunks.length - 1; i >= 0; i--) {
            result = aggregator.addChunk(chunks[i]);
            if (i != 0) {
                assertNull("message still incomplete", result.getMessage());
            }
        }
        assertNotNull(result);
        assertNotNull("first chunk should've completed the message", result.getMessage());
        assertEquals(1, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
        assertEquals(5, counterValueNamed(metricRegistry, CHUNK_COUNTER));
        assertEquals(0, counterValueNamed(metricRegistry, WAITING_MESSAGES));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));
    }

    @Test
    public void differentIdsDoNotInterfere() {
        final ChannelBuffer[] msg1 = createChunkedMessage(4096 + 1, 1024, generateMessageId(1));// 5 chunks;
        final ChannelBuffer[] msg2 = createChunkedMessage(4096 + 1, 1024, generateMessageId(2));// 5 chunks;

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
        assertNotNull("message 1 should be complete", result1.getMessage());
        assertNull("message 2 should not be complete", result2.getMessage());
        // only one is complete, we sent 9 chunks
        assertEquals(1, counterValueNamed(metricRegistry, COMPLETE_MESSAGES));
        assertEquals(9, counterValueNamed(metricRegistry, CHUNK_COUNTER));
        assertEquals(1, counterValueNamed(metricRegistry, WAITING_MESSAGES));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_CHUNKS));
        assertEquals(0, counterValueNamed(metricRegistry, EXPIRED_MESSAGES));
    }

    private ChannelBuffer[] createChunkedMessage(int messageSize, int maxChunkSize) {
        return createChunkedMessage(messageSize, maxChunkSize, generateMessageId());
    }

    private ChannelBuffer[] createChunkedMessage(int messageSize, int maxChunkSize, byte[] messageId) {
        // partially copied from GelfClient (can't use here, because it uses netty4)

        int sequenceCount = (messageSize / maxChunkSize);

        // Check if we have to add another chunk due to integer division.
        if ((messageSize % maxChunkSize) != 0) {
            sequenceCount++;
        }
        final ChannelBuffer[] buffers = new ChannelBuffer[sequenceCount];

        final byte[] sequenceCountArr = new byte[]{(byte) sequenceCount};

        for (int sequenceNumber = 0; sequenceNumber < sequenceCount; sequenceNumber++) {
            final byte[] sequenceNumberArry = new byte[]{(byte) sequenceNumber};

            // fake payload, we don't care about actually parsing it in this test
            int payloadSize = maxChunkSize;
            // correctly size the last chunk
            if (sequenceNumber + 1 == sequenceCount) {
                payloadSize = (messageSize % maxChunkSize);
            }
            final ChannelBuffer payload = ChannelBuffers.buffer(payloadSize);
            payload.writeZero(payloadSize);

            buffers[sequenceNumber] = ChannelBuffers.copiedBuffer(
                    CHUNK_MAGIC_BYTES,
                    messageId,
                    sequenceNumberArry,
                    sequenceCountArr,
                    payload.array()
            );
        }
        return buffers;
    }

    private byte[] generateMessageId(int id) {
        final ChannelBuffer messageId = ChannelBuffers.buffer(8);

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
