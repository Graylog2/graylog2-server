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
package org.graylog2.outputs;

import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;

import javax.inject.Inject;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

public class BenchmarkOutput implements MessageOutput {
    private static final List<String> SKIPPED_METRIC_PREFIXES = ImmutableList.of("org.graylog2.rest.resources");

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Meter messagesWritten;
    private final CsvReporter csvReporter;
    private final MessageQueueAcknowledger messageQueueAcknowledger;
    private final Timer ackTime;

    @AssistedInject
    public BenchmarkOutput(final MetricRegistry metricRegistry,
                           final MessageQueueAcknowledger messageQueueAcknowledger,
                           @Assisted Stream stream,
                           @Assisted Configuration configuration) {
        this(metricRegistry, messageQueueAcknowledger);
    }

    @Inject
    public BenchmarkOutput(final MetricRegistry metricRegistry, MessageQueueAcknowledger messageQueueAcknowledger) {
        this.messageQueueAcknowledger = messageQueueAcknowledger;
        this.messagesWritten = metricRegistry.meter(name(this.getClass(), "messagesWritten"));
        this.ackTime = metricRegistry.timer(name(this.getClass(), "ackTime"));

        final File directory = new File("benchmark-csv");
        //noinspection ResultOfMethodCallIgnored
        directory.mkdirs();

        csvReporter = CsvReporter.forRegistry(metricRegistry)
                .formatFor(Locale.US)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(new CsvMetricFilter(SKIPPED_METRIC_PREFIXES))
                .build(directory);

        csvReporter.start(1, TimeUnit.SECONDS);

        isRunning.set(true);
    }

    @Override
    public void stop() {
        csvReporter.stop();
        isRunning.set(false);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        try (final Timer.Context ignored = ackTime.time()) {
            messageQueueAcknowledger.acknowledge(message.getMessageQueueId());
        }
        messagesWritten.mark();
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        messageQueueAcknowledger.acknowledge(messages.stream().map(Message::getMessageQueueId).collect(Collectors.toList()));

        messagesWritten.mark(messages.size());
    }

    public interface Factory extends MessageOutput.Factory<BenchmarkOutput> {
        @Override
        BenchmarkOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends MessageOutput.Config {
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Benchmark output", false, "", "Output that benchmarks message rates");
        }
    }

    private static class CsvMetricFilter implements MetricFilter {
        private final List<String> prefixes;

        public CsvMetricFilter(List<String> prefixes) {
            this.prefixes = prefixes;
        }

        @Override
        public boolean matches(String name, Metric metric) {
            for (String prefix : prefixes) {
                if (name.startsWith(prefix)) {
                    return false;
                }
            }

            return true;
        }
    }
}
