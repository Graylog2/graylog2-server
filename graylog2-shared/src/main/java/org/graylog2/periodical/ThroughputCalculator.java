package org.graylog2.periodical;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Iterables;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.GlobalMetricNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.graylog2.shared.metrics.MetricUtils.filterSingleMetric;

public class ThroughputCalculator extends Periodical {
    private static final Logger log = LoggerFactory.getLogger(ThroughputCalculator.class);

    private final MetricRegistry metricRegistry;
    private long prevOutputCount = 0;
    private final AtomicLong outputCountAvgLastSecond = new AtomicLong(0);
    private long prevInputCount = 0;
    private AtomicLong inputCountAvgLastSecond = new AtomicLong(0);

    @Inject
    public ThroughputCalculator(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        try {
            metricRegistry.register(GlobalMetricNames.OUTPUT_THROUGHPUT_RATE, new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return outputCountAvgLastSecond.get();
                }
            });

            metricRegistry.register(GlobalMetricNames.INPUT_THROUGHPUT_RATE, new Gauge<Long>() {
                @Override
                public Long getValue() {
                    return inputCountAvgLastSecond.get();
                }
            });

        } catch (Exception e) {
            log.error("Unable to register metric", e);
        }
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public void doRun() {
        final SortedMap<String, Counter> counters = metricRegistry.getCounters(
                filterSingleMetric(GlobalMetricNames.OUTPUT_THROUGHPUT)
        );

        final Counter outputThroughput = Iterables.getOnlyElement(counters.values(), null);
        if (outputThroughput == null) {
            log.debug("Could not find throughput meter '{}'. Trying again later.", GlobalMetricNames.OUTPUT_THROUGHPUT);
        } else {
            final long currentOutputThroughput = outputThroughput.getCount();
            outputCountAvgLastSecond.set(currentOutputThroughput - prevOutputCount);
            prevOutputCount = currentOutputThroughput;
        }

        // rinse and repeat for input throughput
        final SortedMap<String, Counter> inputCounters = metricRegistry.getCounters(
                filterSingleMetric(GlobalMetricNames.INPUT_THROUGHPUT)
        );

        final Counter inputThroughput = Iterables.getOnlyElement(inputCounters.values(), null);
        if (inputThroughput == null) {
            log.debug("Could not find throughput meter '{}'. Trying again later.", GlobalMetricNames.INPUT_THROUGHPUT);
        } else {
            final long currentInputThroughput = inputThroughput.getCount();
            inputCountAvgLastSecond.set(currentInputThroughput - prevInputCount);
            prevInputCount = currentInputThroughput;
        }
    }
}
