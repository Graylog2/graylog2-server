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
package org.graylog2.plugin;

import java.io.IOException;
import java.io.InputStream;

/**
 * A wrapper to protect GZIPInputStream and InflaterInputStream from decompression bombs. Used by
 * {@link org.graylog2.plugin.Tools#decompressGzip(byte[])} and
 * {@link org.graylog2.plugin.Tools#decompressZlib(byte[])}.
 */
public class LimitedInputStream extends InputStream {

    /**
     * Default limit set to 2 megabytes.
     */
    public static final long DECOMPRESS_MAXIMUM_OUTPUT = (long)(1024*1024*2);
    private final InputStream input;
    private long totalDecompressed = 0L;
    private long maximumOutput = DECOMPRESS_MAXIMUM_OUTPUT;

    /**
     * Constructor that uses default value for maximum amount of output.
     */
    public LimitedInputStream(InputStream in) { this.input = in; }

    /**
     * Alternate constructor that allows setting maximum amount of output.
     */
    public LimitedInputStream(InputStream in, long limit) { this.input = in; maximumOutput = limit; }


    @Override
    public int read() throws IOException {
        int result = input.read();
        totalDecompressed++;
        return result;
    }

    /**
     * Reads from InputStream. Wraps normal {@link java.io.InputStream#read(byte[])}.
     * @throws IOException On top of the ordinary if the limit for maximum output has been reached.
     */
    @Override
    public int read(byte[] b) throws IOException {
        int read = this.input.read(b);
        totalDecompressed += (long) read;
        if (totalDecompressed > maximumOutput) {
            throw new IOException("Got more than " + maximumOutput + " bytes");
        }
        return read;
    }
}
