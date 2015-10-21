/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin;

import static com.codahale.metrics.MetricRegistry.name;

public final class GlobalMetricNames {

    private GlobalMetricNames() {}

    public static final String RATE_SUFFIX = "1-sec-rate";

    public static final String INPUT_THROUGHPUT = "org.graylog2.throughput.input";

    public static final String OUTPUT_THROUGHPUT = "org.graylog2.throughput.output";
    public static final String OUTPUT_THROUGHPUT_RATE = name(OUTPUT_THROUGHPUT, RATE_SUFFIX);

    public static final String INPUT_BUFFER_USAGE = "org.graylog2.buffers.input.usage";
    public static final String INPUT_BUFFER_SIZE = "org.graylog2.buffers.input.size";

    public static final String PROCESS_BUFFER_USAGE = "org.graylog2.buffers.process.usage";
    public static final String PROCESS_BUFFER_SIZE = "org.graylog2.buffers.process.size";

    public static final String OUTPUT_BUFFER_USAGE = "org.graylog2.buffers.output.usage";
    public static final String OUTPUT_BUFFER_SIZE = "org.graylog2.buffers.output.size";

    public static final String JOURNAL_APPEND_RATE = name("org.graylog2.journal.append", RATE_SUFFIX);
    public static final String JOURNAL_READ_RATE = name("org.graylog2.journal.read", RATE_SUFFIX);
    public static final String JOURNAL_SEGMENTS = "org.graylog2.journal.segments";
    public static final String JOURNAL_UNCOMMITTED_ENTRIES = "org.graylog2.journal.entries-uncommitted";
    public static final String JOURNAL_SIZE = "org.graylog2.journal.size";
    public static final String JOURNAL_SIZE_LIMIT = "org.graylog2.journal.size-limit";
    public static final String JOURNAL_UTILIZATION_RATIO = "org.graylog2.journal.utilization-ratio";
    public static final String JOURNAL_OLDEST_SEGMENT = "org.graylog2.journal.oldest-segment";

}
