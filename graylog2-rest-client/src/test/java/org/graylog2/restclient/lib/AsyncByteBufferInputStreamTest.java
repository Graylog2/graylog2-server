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
package org.graylog2.restclient.lib;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AsyncByteBufferInputStreamTest {
    @Test
    public void testSyncRead() throws InterruptedException, IOException {
        final AsyncByteBufferInputStream stream = new AsyncByteBufferInputStream();

        stream.putBuffer(ByteBuffer.wrap("123".getBytes()));
        stream.putBuffer(ByteBuffer.wrap("456".getBytes()));

        // we should be able to read 6 bytes in a row
        byte[] bytes = new byte[6];
        final int read = stream.read(bytes);
        Assert.assertEquals(6, read);
        Assert.assertArrayEquals("123456".getBytes(), bytes);
        stream.setDone(true);

        final int nextRead = stream.read();
        Assert.assertEquals(-1, nextRead);
        stream.close();
    }

    @Test
    public void testDoneWithMoreData() throws InterruptedException, IOException {
        final AsyncByteBufferInputStream stream = new AsyncByteBufferInputStream();

        stream.putBuffer(ByteBuffer.wrap("123".getBytes()));
        stream.putBuffer(ByteBuffer.wrap("456".getBytes()));

        // we should be able to read 6 bytes in a row
        byte[] bytes = new byte[6];
        final int read = stream.read(bytes);
        Assert.assertEquals(6, read);
        Assert.assertArrayEquals("123456".getBytes(), bytes);

        stream.putBuffer(ByteBuffer.wrap("789".getBytes()));
        stream.setDone(true);

        byte[] finalBytes = new byte[3];
        final int nextRead = stream.read(finalBytes);
        Assert.assertEquals(3, nextRead);
        Assert.assertArrayEquals("789".getBytes(), finalBytes);

        final int eos = stream.read();
        Assert.assertEquals(-1, eos);

        stream.close();
    }

    @Test
    public void testAsync() throws InterruptedException {
        final AsyncByteBufferInputStream stream = new AsyncByteBufferInputStream();

        final Thread writer = new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    stream.putBuffer(ByteBuffer.wrap("12345".getBytes()));
                    stream.putBuffer(ByteBuffer.wrap("67890".getBytes()));
                }
                stream.setDone(true);

            }
        };
        final AtomicInteger readBytes = new AtomicInteger(0);
        final Thread reader = new Thread() {
            @Override
            public void run() {
                try {
                    while (stream.read() != -1) {
                        readBytes.incrementAndGet();
                    }
                } catch (IOException ignored) {/* no IO done, it's all in memory */}
            }
        };
        reader.start();
        writer.start();
        reader.join();
        writer.join();

        assertTrue(stream.isDone());
        assertNull(stream.getFailed());
        assertEquals(100, readBytes.get());
    }

    @Test
    public void testAsyncException() throws InterruptedException {
        final AsyncByteBufferInputStream stream = new AsyncByteBufferInputStream();

        final Thread writer = new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 3; i++) {
                    stream.putBuffer(ByteBuffer.wrap("12345".getBytes()));
                    stream.putBuffer(ByteBuffer.wrap("67890".getBytes()));
                }
                stream.setFailed(new Exception("Some weird error"));
            }
        };
        final AtomicBoolean caughtExceptionInReader = new AtomicBoolean(false);
        final Thread reader = new Thread() {
            @Override
            public void run() {
                int count = 0;
                try {
                    while (stream.read() != -1) {
                        count++;

                        if (count > 30) {
                            fail("Should've caught IOException.");
                        }
                    }
                } catch (IOException e) {
                    caughtExceptionInReader.set(true);
                }
            }
        };
        reader.start();
        writer.start();
        reader.join();
        writer.join();

        await().atMost(1, TimeUnit.SECONDS).untilTrue(caughtExceptionInReader);
        assertTrue(stream.getFailed() instanceof Exception);
        assertEquals("Some weird error", stream.getFailed().getMessage());
    }

    @Test
    public void readIsBlockingUntilDataIsPresent() throws InterruptedException {
        final AsyncByteBufferInputStream stream = new AsyncByteBufferInputStream();

        final Thread writer = new Thread() {
            @Override
            public void run() {
                stream.putBuffer(ByteBuffer.wrap("12345".getBytes()));
                Uninterruptibles.sleepUninterruptibly(250, TimeUnit.MILLISECONDS);
                stream.putBuffer(ByteBuffer.wrap("67890".getBytes()));
                Uninterruptibles.sleepUninterruptibly(250, TimeUnit.MILLISECONDS);
                stream.setDone(true);
            }
        };
        final AtomicInteger readBytes = new AtomicInteger(0);
        final Thread reader = new Thread() {
            @Override
            public void run() {
                try {
                    while (stream.read() != -1) {
                        readBytes.incrementAndGet();
                    }
                } catch (IOException ignored) {/* no IO done, it's all in memory */}
            }
        };
        reader.start();
        writer.start();
        reader.join();
        writer.join();

        await().atMost(1, TimeUnit.SECONDS).until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return stream.isDone();
            }
        });

        assertNull(stream.getFailed());
        assertEquals(10, readBytes.get());
    }
}