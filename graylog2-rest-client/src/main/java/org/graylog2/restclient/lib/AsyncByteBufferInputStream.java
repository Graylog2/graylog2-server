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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AsyncByteBufferInputStream extends InputStream {

    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);

    private ByteArrayInputStream currentBuffer;

    private Throwable failed;

    private boolean done;

    private final BlockingQueue<ByteBuffer> buffers;

    public AsyncByteBufferInputStream() {
        // 100 limits the amount of memory we are willing to spend before blocking
        this(100);
    }

    public AsyncByteBufferInputStream(int size) {
        buffers = new LinkedBlockingQueue<>(size);
    }

    private void readNextBuffer() {
        try {
            final ByteBuffer buffer = buffers.take();
            currentBuffer = new ByteArrayInputStream(buffer.array());
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public int read() throws IOException {
        // fail if we encountered an exception
        if (failed != null) {
            throw new IOException(failed);
        }
        // claim next buffer
        if (currentBuffer == null) {
            readNextBuffer();
        }

        int nextByte = currentBuffer.read();
        if (nextByte == -1) { // currentBuffer exhausted, load next buffer if present
            if (buffers.size() == 0 && isDone()) { // there will be no more data
                return -1;
            }
            readNextBuffer(); // this can block until data is present.
            nextByte = currentBuffer.read(); // on isDone or isFailed the last buffer will be empty
            // double check for failure to detect empty buffer case
            if (failed != null) {
                throw new IOException(failed);
            }
        }
        return nextByte;
    }

    public Throwable getFailed() {
        return failed;
    }

    public void setFailed(Throwable failed) {
        insertMarkerBuffer();
        this.failed = failed;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        insertMarkerBuffer();
        this.done = done;
    }

    private void insertMarkerBuffer() {
        putBuffer(EMPTY_BUFFER);
    }

    public void putBuffer(ByteBuffer buffer) {
        Uninterruptibles.putUninterruptibly(buffers, buffer);
    }

}
