/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.inputs.codecs;

import org.graylog2.plugin.InstantMillisProvider;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

public class GelfChunkAggregatorTest {
    private static final byte[] CHUNK_MAGIC_BYTES = new byte[]{0x1e, 0x0f};
    private MessageInput input;
    private ScheduledThreadPoolExecutor poolExecutor;
    private GelfChunkAggregator aggregator;
    private InetSocketAddress remoteAddress;

    @BeforeTest
    public void before() {
        input = mock(MessageInput.class);
        when(input.getUniqueReadableId()).thenReturn("input-id");
        poolExecutor = new ScheduledThreadPoolExecutor(1);
        aggregator = new GelfChunkAggregator(poolExecutor);
        remoteAddress = InetSocketAddress.createUnresolved("127.0.0.1", 4444);
    }

    @AfterTest
    public void after() {
        poolExecutor.shutdown();
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void addSingleChunk() {
        final ChannelBuffer[] singleChunk = createChunkedMessage(512, 1024);

        final CodecAggregator.Result result = aggregator.addChunk(singleChunk[0]);

        assertNotNull(result.getMessage(), "message should be complete");

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
                assertNotNull(result.getMessage(), "message should've been assembled from chunks");
            } else {
                assertNull(result.getMessage(), "chunks not complete");
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
        aggregator = new GelfChunkAggregator(poolExecutor);
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
            assertNull(result.getMessage(), "chunks not complete");
        }
        // move clock forward enough to evict all of the chunks
        clock.tick(Period.seconds(10));

        evictionTask.run();

        final CodecAggregator.Result result = aggregator.addChunk(chunks[0]);

        assertNull(result.getMessage(), "message should not be complete because chunks were evicted already");
        assertTrue(result.isValid());

        // reset clock for other tests
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void outOfOrderChunks() {
        final ChannelBuffer[] chunks = createChunkedMessage(4096 + 512, 1024); // creates 5 chunks
        CodecAggregator.Result result = null;
        for (int i = chunks.length - 1 ; i >= 0; i--) {
            result = aggregator.addChunk(chunks[i]);
            if (i != 0) {
                assertNull(result.getMessage(), "message still incomplete");
            }
        }
        assertNotNull(result);
        assertNotNull(result.getMessage(), "first chunk should've completed the message");
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
        assertNotNull(result1.getMessage(), "message 1 should be complete");
        assertNull(result2.getMessage(), "message 2 should not be complete");
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
}