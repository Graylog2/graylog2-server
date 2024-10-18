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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.indexer.messages.ImmutableMessage;
import org.graylog2.indexer.messages.IndexingResults;
import org.graylog2.indexer.messages.MessageWithIndex;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.outputs.filter.DefaultFilteredMessage;
import org.graylog2.outputs.filter.FilteredMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.outputs.FilteredMessageOutput;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;

@Singleton
public class ElasticSearchOutput implements MessageOutput, FilteredMessageOutput {
    public static final String FILTER_KEY = "indexer";
    private static final String WRITES_METRICNAME = name(ElasticSearchOutput.class, "writes");
    private static final String FAILURES_METRICNAME = name(ElasticSearchOutput.class, "failures");
    private static final String PROCESS_TIME_METRICNAME = name(ElasticSearchOutput.class, "processTime");

    private static final String NAME = "ElasticSearch Output";
    private static final Logger LOG = LoggerFactory.getLogger(ElasticSearchOutput.class);

    private final Meter writes;
    private final Meter ignores;
    private final Meter failures;
    private final Timer processTime;
    private final Messages messages;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Inject
    public ElasticSearchOutput(MetricRegistry metricRegistry, Messages messages) {
        this.messages = messages;
        // Only constructing metrics here. write() get's another Core reference. (because this technically is a plugin)
        this.writes = metricRegistry.meter(WRITES_METRICNAME);
        this.failures = metricRegistry.meter(FAILURES_METRICNAME);
        this.processTime = metricRegistry.timer(PROCESS_TIME_METRICNAME);
        this.ignores = metricRegistry.meter(name(FilteredMessageOutput.class, FILTER_KEY, "ignores"));

        // Should be set in initialize once this becomes a real plugin.
        isRunning.set(true);
    }

    @Override
    public void writeFiltered(List<FilteredMessage> filteredMessages) throws Exception {
        final var messages = filteredMessages.stream()
                .filter(message -> !message.destinations().get(FILTER_KEY).isEmpty())
                .toList();

        writes.mark(messages.size());
        ignores.mark(filteredMessages.size() - messages.size());

        writeMessageEntries(messages);
    }

    @Override
    public void write(Message message) throws Exception {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Writing message id to [{}]: <{}>", NAME, message.getId());
        }
        writeMessageEntries(List.of(DefaultFilteredMessage.forDestinationKeys(message, Set.of(FILTER_KEY))));
    }

    @Override
    public void write(List<Message> messageList) throws Exception {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Writing {} messages to [{}]", messageList.size(), NAME);
        }
        writeMessageEntries(messageList.stream()
                .map(message -> DefaultFilteredMessage.forDestinationKeys(message, Set.of(FILTER_KEY)))
                .collect(Collectors.toList()));
    }

    private void writeMessageEntries(List<FilteredMessage> messageList) {
        // We need to create one message per index set. Use the streams from the filtered targets.
        final var messagesWithIndex = messageList.stream()
                .flatMap(message -> message.destinations()
                        .get(FILTER_KEY)
                        .stream()
                        .map(stream -> new MessageWithIndex(message.message(), stream.getIndexSet())))
                .toList();

        if (LOG.isTraceEnabled()) {
            @SuppressWarnings("deprecation")
            final String sortedIds = messagesWithIndex.stream()
                    .map(MessageWithIndex::message)
                    .map(ImmutableMessage::getId)
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.joining(", "));
            LOG.trace("Writing message ids to [{}]: <{}>", NAME, sortedIds);
        }

        writes.mark(messageList.size());
        final IndexingResults indexingResults;
        try (final Timer.Context ignored = processTime.time()) {
            indexingResults = messages.bulkIndex(messagesWithIndex);
        }
        failures.mark(indexingResults.errors().size());
    }

    @Override
    public void stop() {
        // TODO: Move ES stop code here.
        //isRunning.set(false);
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    public interface Factory extends MessageOutput.Factory<ElasticSearchOutput> {
        @Override
        ElasticSearchOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            // Built in output. This is just for plugin compat. No special configuration required.
            return new ConfigurationRequest();
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("Elasticsearch Output", false, "", "Elasticsearch Output");
        }

        public Descriptor(String name, boolean exclusive, String linkToDocs, String humanName) {
            super(name, exclusive, linkToDocs, humanName);
        }
    }
}
