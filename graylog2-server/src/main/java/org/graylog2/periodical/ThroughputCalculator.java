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
package org.graylog2.periodical;

import com.codahale.metrics.Counting;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codahale.metrics.MetricRegistry.name;
import static org.graylog2.shared.metrics.MetricUtils.filterSingleMetric;

public class ThroughputCalculator extends Periodical {
    private static final Logger log = LoggerFactory.getLogger(ThroughputCalculator.class);

    private final MetricRegistry metricRegistry;

    protected static final Pattern incomingMessagesPattern = Pattern.compile("org\\.graylog2\\.plugin\\.streams\\.Stream\\..*?\\.incomingMessages");
    protected static final MetricFilter streamMetricFilter = new MetricFilter() {
        @Override
        public boolean matches(String name, Metric metric) {
            Matcher matcher = incomingMessagesPattern.matcher(name);
            return matcher.matches();
        }
    };

    private ConcurrentMap<String, CounterSample> sampledCounters = Maps.newConcurrentMap();

    @Inject
    public ThroughputCalculator(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
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
        final SortedMap<String, ? extends Counting> counters = metricRegistry.getCounters(
                filterSingleMetric(GlobalMetricNames.OUTPUT_THROUGHPUT)
        );
        // rinse and repeat for input throughput
        final SortedMap<String, ? extends Counting> inputCounters = metricRegistry.getCounters(
                filterSingleMetric(GlobalMetricNames.INPUT_THROUGHPUT)
        );

        // StreamMetrics isn't accessible here, so we need to use a metrics filter instead.
        final SortedMap<String, ? extends Counting> streamMeters = metricRegistry.getMeters(streamMetricFilter);

        final Iterable<Map.Entry<String, ? extends Counting>> entries = Iterables.concat(counters.entrySet(),
                                                                                         inputCounters.entrySet(),
                                                                                         streamMeters.entrySet());

        // calculate rates
        for (Map.Entry<String, ? extends Counting> countingEntry : entries) {
            final Counting value = countingEntry.getValue();

            final String metricName = countingEntry.getKey();
            CounterSample counterSample = sampledCounters.get(metricName);
            if (counterSample == null) {
                counterSample = new CounterSample();
                sampledCounters.put(metricName, counterSample);
            }
            counterSample.updateAverage(value.getCount());
            final String rateName = name(metricName, GlobalMetricNames.RATE_SUFFIX);
            if (!metricRegistry.getMetrics().containsKey(rateName)) {
                try {
                    log.debug("Registering derived, per-second metric {}", rateName);
                    metricRegistry.register(rateName, new Gauge<Double>() {
                        @Override
                        public Double getValue() {
                            final CounterSample sample = sampledCounters.get(metricName);
                            return sample == null ? 0d : sample.getCurrentAverage();
                        }
                    });
                } catch (IllegalArgumentException e) {
                    log.warn(
                            "Could not register gauge {} despite checking before that it didn't exist. This should not happen.",
                            rateName);
                }
            }
        }
    }

    private static class CounterSample {
        private long previousCount = 0L;
        private double currentAverage = 0d;

        /**
         * Updates the "average" of the corresponding counter metric.
         * <p/>
         * Implementation note: <br/>
         * We assume that this is done once per second and live with any inaccuracies if that schedule isn't hit exactly.
         * Thus the average becomes the delta, because the denominator is always 1. The field and return value is a double
         * to keep the option to sample at different intervals in the future.
         *
         * @param currentCount the currently observed count
         */
        public void updateAverage(long currentCount) {
            currentAverage = currentCount - previousCount;
            previousCount = currentCount;
        }

        public double getCurrentAverage() {
            return currentAverage;
        }
    }

}
