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
package org.graylog2.plugin;

import static com.codahale.metrics.MetricRegistry.name;

public final class GlobalMetricNames {

    private GlobalMetricNames() {}

    public static final String OLDEST_SEGMENT_SUFFIX = "oldest-segment";
    public static final String RATE_SUFFIX = "1-sec-rate";

    public static final String INPUT_THROUGHPUT = "org.graylog2.throughput.input";

    public static final String OUTPUT_THROUGHPUT = "org.graylog2.throughput.output";
    public static final String OUTPUT_THROUGHPUT_RATE = name(OUTPUT_THROUGHPUT, RATE_SUFFIX);

    public static final String INPUT_TRAFFIC = "org.graylog2.traffic.input";
    public static final String DECODED_TRAFFIC = "org.graylog2.traffic.decoded";
    public static final String OUTPUT_TRAFFIC = "org.graylog2.traffic.output";
    public static final String SYSTEM_OUTPUT_TRAFFIC = "org.graylog2.traffic.system-output-traffic";

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
    public static final String JOURNAL_OLDEST_SEGMENT = name("org.graylog2.journal", OLDEST_SEGMENT_SUFFIX);
}
