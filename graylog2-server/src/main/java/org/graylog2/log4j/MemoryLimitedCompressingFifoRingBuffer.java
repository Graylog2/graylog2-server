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
package org.graylog2.log4j;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * MemoryLimitedCompressingFifoRingBuffer is a first-in first-out buffer that
 * is limited by the memory it can consume. If the memory limit is exceeded, it will
 * evict the oldest elements, before it adds a new element.
 * Elements are compressed using Zstd.
 * To achieve better compression the elements are compressed in batches
 * of {@link MemoryLimitedCompressingFifoRingBuffer#BATCHSIZE}.
 * This means that the if the buffer is full, old elements are evicted
 * in full batches. Not single elements.
 */
public class MemoryLimitedCompressingFifoRingBuffer {
    private static final Logger LOG = LoggerFactory.getLogger(MemoryLimitedCompressingFifoRingBuffer.class);

    public static final int DEFAULT_ZSTD_COMPRESSION_LEVEL = 1;
    public static final int BATCHSIZE = 512;
    private final ArrayList<byte []> currentBatch;
    private final MemoryLimitedFifoRingBuffer compressedRingBuffer;
    private final int zStdCompressionLevel;
    private ZstdOutputStream compressedStream;
    private ByteArrayOutputStream outputStreamBuffer;

    /**
     * Construct a new MemoryLimitedCompressingFifoRingBuffer with the
     * default Zstd compression level of {@link MemoryLimitedCompressingFifoRingBuffer#DEFAULT_ZSTD_COMPRESSION_LEVEL}
     * @param memLimit    the memory limit of the Buffer in bytes
     */
    public MemoryLimitedCompressingFifoRingBuffer(long memLimit) {
        this(memLimit, DEFAULT_ZSTD_COMPRESSION_LEVEL);
    }

    /**
     * Construct a new MemoryLimitedCompressingFifoRingBuffer
     * @param memLimit    the memory limit of the Buffer in bytes
     * @param zStdCompressionLevel the Zstd compression level to use
     */
    public MemoryLimitedCompressingFifoRingBuffer(long memLimit, int zStdCompressionLevel) {
        currentBatch = new ArrayList<>(BATCHSIZE);
        compressedRingBuffer = new MemoryLimitedFifoRingBuffer(memLimit);
        this.zStdCompressionLevel = zStdCompressionLevel;
    }

    public synchronized void add(byte[] element) throws IOException {
        if (currentBatch.size() >= BATCHSIZE) {
            flush();
        }
        currentBatch.add(element);
        writeIntoCompressedStream(element);
    }

    private void writeIntoCompressedStream(byte[] element) throws IOException {
        if (compressedStream == null) {
            setUpCompressedStream();
        }
        compressedStream.write(element);
    }

    private void setUpCompressedStream() throws IOException {
        outputStreamBuffer = new ByteArrayOutputStream(8192);
        compressedStream = new ZstdOutputStream(outputStreamBuffer, zStdCompressionLevel);
    }

    private void flush() throws IOException {
        compressedStream.close();
        compressedRingBuffer.add(outputStreamBuffer.toByteArray());
        currentBatch.clear();
        setUpCompressedStream();
    }

    /**
     * Write the buffer content into the provided OutputStream.
     * @param outputStream The OutputStream to write the entries into.
     * @param limit    limit the returned entries. This is only a rough estimate and will be rounded to the nearest batch size.
     */
    public void streamContent(OutputStream outputStream, int limit) {
        // stream content with copies to avoid blocking add() for too long
        List<byte[]> current;
        List<byte[]> compressed;
        synchronized (this) {
            current = List.copyOf(currentBatch);
            compressed = List.copyOf(compressedRingBuffer);
        }
        limit = limit == 0 ? Integer.MAX_VALUE : limit;

        // The uncompressed currentBatch entries can be limited exactly
        final int currentBatchSize = current.size();
        int skipFromCurrent = limit - currentBatchSize;
        skipFromCurrent = skipFromCurrent > 0 ? 0 : skipFromCurrent * -1;

        limit -= currentBatchSize;

        // The compressed batches are returned in full
        final long getCompressedBatches = limit > 0 ? limit / BATCHSIZE + 1 : 0;
        long skipFromCompressed = getCompressedBatches - compressed.size();
        skipFromCompressed = skipFromCompressed > 0 ? 0 : skipFromCompressed * -1;

        compressed.stream()
                .skip(skipFromCompressed)
                .map(input -> {
                    try {
                        return decompress(input);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .forEach(b -> {
                    try {
                        outputStream.write(b);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        current.stream().skip(skipFromCurrent).forEach(b -> {
            try {
                outputStream.write(b);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private byte[] decompress(byte [] input) throws IOException {
        final ZstdInputStream zstdInputStream = new ZstdInputStream(new ByteArrayInputStream(input));
        return zstdInputStream.readAllBytes();
    }

    public synchronized void clear() throws IOException {
        currentBatch.clear();
        compressedRingBuffer.clear();
        compressedStream.close();
        compressedStream = null;
        outputStreamBuffer = null;
    }

    /**
     * Returns an estimate of the size in bytes
     * @return the estimated buffer content size in bytes
     */
    public long getLogsSize() {
        final Integer currentBatchSize = currentBatch.stream().map(b -> b.length).reduce(Integer::sum).orElse(0);
        // Assume the uncompressed size is 3 times bigger
        return (compressedRingBuffer.currentSize * 3) + currentBatchSize;
    }

    static class MemoryLimitedFifoRingBuffer extends LinkedList<byte[]> {
        private final long memLimit;
        private long currentSize;

        public MemoryLimitedFifoRingBuffer(long memLimit) {
            this.memLimit = memLimit;
            this.currentSize = 0;
        }

        @Override
        public boolean add(byte[] element) {
            while (currentSize + element.length > memLimit) {
                try {
                    removeFirst();
                } catch (NoSuchElementException ignored) {
                    LOG.warn("Buffer size <{}> too small to hold a single message of size <{}>", memLimit, element.length);
                    return false;
                }
            }
            currentSize += element.length;
            return super.add(element);
        }

        @Override
        public boolean remove(Object o) {
            final boolean removed = super.remove(o);
            if (removed && o instanceof byte[] bytes) {
                currentSize -= bytes.length;
            }
            return removed;
        }

        @Override
        public void clear() {
            super.clear();
            currentSize = 0;
        }

        @Override
        public byte[] removeFirst() {
            final byte[] removed = super.removeFirst();
            currentSize -= removed.length;
            return removed;
        }
    }
}
