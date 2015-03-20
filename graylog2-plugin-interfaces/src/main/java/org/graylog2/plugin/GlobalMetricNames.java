package org.graylog2.plugin;

import static com.codahale.metrics.MetricRegistry.name;

public class GlobalMetricNames {

    public static final String INPUT_THROUGHPUT = "org.graylog.throughput.input";
    public static final String INPUT_THROUGHPUT_RATE = name(INPUT_THROUGHPUT, "1-sec-rate");

    public static final String OUTPUT_THROUGHPUT = "org.graylog.throughput.output";
    public static final String OUTPUT_THROUGHPUT_RATE = name(OUTPUT_THROUGHPUT, "1-sec-rate");

}
