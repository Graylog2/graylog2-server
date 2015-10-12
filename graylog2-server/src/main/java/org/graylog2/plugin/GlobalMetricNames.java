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
